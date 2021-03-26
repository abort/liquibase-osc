package liquibase.ext.changes

import liquibase.Scope
import liquibase.change.custom.CustomTaskChange
import liquibase.change.custom.CustomTaskRollback
import liquibase.database.Database
import liquibase.database.core.OracleDatabase
import liquibase.database.jvm.JdbcConnection
import liquibase.exception.CustomChangeException
import liquibase.exception.ValidationErrors
import liquibase.resource.ResourceAccessor
import liquibase.structure.core.Column
import java.sql.DatabaseMetaData
import java.sql.PreparedStatement
import java.sql.RowIdLifetime
import java.sql.SQLException
import java.sql.Statement

/**
 * Class to do migrations in small batches
 * Should be followed by a <update> where update is idempotent with this one
 * For databases that are not implemented this class serves as a 'no-op'
 */
class BatchMigrationChange : CustomTaskChange, CustomTaskRollback {
    companion object {
        const val DefaultChunkSize = 1000L
        const val DefaultSleepTime = 0L
    }

    var catalogName: String? = null
    var schemaName: String? = null
    var tableName: String? = null
    var fromColumns: String? = null
    var toColumns: String? = null
    var chunkSize: Long? = DefaultChunkSize
    var sleepTime: Long? = DefaultSleepTime

    private var resourceAccessor: ResourceAccessor? = null

    private val fromArray: Array<Column>? by lazy {
        Column.arrayFromNames(fromColumns)
    }

    private val toArray: Array<Column>? by lazy {
        Column.arrayFromNames(toColumns)
    }

    private val updateColumnsString: String by lazy {
        toArray!!.zip(fromArray!!).joinToString(separator = ", ") { (f, t) -> "${f.name} = ${t.name}" }
    }

    private val fullName: String by lazy {
        OracleDatabase().escapeTableName(catalogName, schemaName, tableName)
    }

    private val whereClauseString: String by lazy {
        val t = toArray!!.first()
        val f = fromArray!!.first()

        "(${t.name} IS NULL and ${f.name} IS NOT NULL)"
//        toArray!!.zip(fromArray!!).joinToString(separator = " OR ") { (f, t) ->
//            "(${f.name} IS NULL and ${t.name} IS NOT NULL)"
//        }
    }

    override fun setFileOpener(ra: ResourceAccessor?) {
        resourceAccessor = ra
    }

    override fun getConfirmationMessage(): String =
        "Successfully migrated $fromColumns to $toColumns in $tableName"

    override fun setUp() {
        Scope.getCurrentScope().getLog(javaClass).info("Initialized BatchMigrationChange")
    }

    override fun validate(db: Database): ValidationErrors {
        val errors = ValidationErrors()
        // make use of short-circuit
        if (db !is OracleDatabase ||
            errors.checkAndAddSimpleErrors() ||
            errors.checkAndAddDuplicateErrors(fromArray!!, "fromColumns") ||
            errors.checkAndAddDuplicateErrors(toArray!!, "toColumns") ||
            errors.checkAndAddNEqFromAndTo() ||
            errors.checkAndAddCrossingColumns()
        ) {
            return errors
        }

        if (sleepTime == null) {
            sleepTime = 0L
        }

        return errors
    }

    private fun ValidationErrors.checkAndAddCrossingColumns(): Boolean = run {
        val toSet = toArray!!.toSet()
        fromArray!!.forEachIndexed { i: Int, col: Column ->
            if (col == toArray!![i]) {
                addError("Column $col should not be migrated to itself")
            } else if (col in toSet) {
                addError("Migration of $col crosses, which is not possible")
            }
        }
        hasErrors()
    }

    private fun ValidationErrors.checkAndAddNEqFromAndTo(): Boolean = run {
        val unequal = fromArray!!.size != toArray!!.size
        if (unequal) {
            addError("Both in and output columns require a 1:1 relationship")
        }
        unequal
    }

    private fun ValidationErrors.checkAndAddSimpleErrors(): Boolean = run {
        if (tableName.isNullOrEmpty()) {
            addError("Table is not provided")
        }

        val nullCount = setOf(fromColumns, toColumns).filter { it.isNullOrEmpty() }.count()
        if (nullCount >= 1) {
            addError("Both fromColumns and toColumns need to be provided")
        }

        if (chunkSize == null || chunkSize!! <= 0L) {
            addError("chunkSize should be provided as a positive long")
        }

        if (sleepTime != null && sleepTime!! < 0L) {
            addError("sleepTime can not be negative")
        }

        hasErrors()
    }

    private fun ValidationErrors.checkAndAddDuplicateErrors(arr: Array<Column>, name: String): Boolean = run {
        val equal = arr.toSet().size != arr.size
        if (equal) {
            addError("Duplicate elements in $name")
        }
        equal
    }

    override fun execute(db: Database) {
        try {
            val scope = Scope.getCurrentScope()
            when (db) {
                is OracleDatabase -> {
                    scope.getLog(javaClass).info("Executing BatchMigrationChange on Oracle Database")
                    startMigration(scope, db)
                }
                else -> {
                    scope.getLog(javaClass).info("Skipping BatchMigrationChange due to non-Oracle Database")
                }
            }
        } catch (e: CustomChangeException) {
            throw e
        }
    }

    @Suppress("ThrowsCount", "NestedBlockDepth")
    private fun startMigration(scope: Scope, db: OracleDatabase) {
        if (db.connection == null || db.connection.isClosed) {
            scope.getLog(javaClass).severe("Not connected to Oracle database")
            throw CustomChangeException("Not connected to Oracle database")
        }

        val conn = db.connection as JdbcConnection
        try {
            if (!hasImmutableRowIds(conn)) {
                scope.getLog(javaClass).severe("rowIds are not immutable, migration strategy can not be applied")
                throw CustomChangeException("Database has no immutable rowIds")
            }

            var running = true
            while (running) {
                val n = executeMigrationChunk(scope, conn)
                if (n == 0L) {
                    running = false
                } else {
                    if (sleepTime != null && sleepTime!! > 0L) {
                        Thread.sleep(sleepTime!!)
                    }
                }
            }
        } catch (e: CustomChangeException) {
            throw e
        }
    }

    private fun executeMigrationChunk(scope: Scope, conn: JdbcConnection): Long {
        var stmt: PreparedStatement? = null
        try {
            // Fetch only the rows where not all values are synced yet
            val query = """
                UPDATE $fullName
                SET $updateColumnsString
                WHERE rowId IN
                (SELECT rowId
                      FROM $fullName
                      WHERE $whereClauseString
                      FETCH FIRST $chunkSize ROWS ONLY
                )
            """.trimIndent()

            stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)
            scope.getLog(javaClass).info("Executing $stmt with query $query")
            val affectedRows = stmt.executeLargeUpdate()
            // serves as no-op when auto-commit = true
            conn.commit()
            scope.getLog(javaClass).info("Committed")
            return affectedRows
        } catch (e: SQLException) {
            throw CustomChangeException("Could not update $tableName in batch", e)
        } finally {
            stmt?.close()
        }
    }

    private fun hasImmutableRowIds(conn: JdbcConnection): Boolean {
        try {
            val dbMetaData: DatabaseMetaData = conn.metaData
            val lifetime = dbMetaData.rowIdLifetime
            return (lifetime == RowIdLifetime.ROWID_VALID_FOREVER)
        } catch (e: SQLException) {
            throw CustomChangeException("Failed to query for rowId lifetime", e)
        }
    }

    // We do not need rollbacks for this as it is not semantics, even though we 'could' by an inverse operation
    override fun rollback(db: Database) {
        Scope.getCurrentScope().getLog(javaClass).info("Rollback requested: no-op result")
    }

    override fun toString(): String {
        return "BatchMigrationChange(table=$tableName, fromColumns=$fromColumns, " +
            "toColumns=$toColumns, " +
            "chunkSize=$chunkSize, sleepTime=$sleepTime)"
    }
}
