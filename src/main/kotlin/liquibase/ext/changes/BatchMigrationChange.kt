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
import java.sql.*

/**
 * Class to do migrations in small batches
 * Should be followed by a <update> where update is idempotent
 * For databases that are not implemented this class serves as a 'no-op'
 */
class BatchMigrationChange : CustomTaskChange, CustomTaskRollback {
    var table: String? = null
    var fromColumns: String? = null
    var toColumns: String? = null
    var primaryKeyColumns: String? = null
    var chunkSize: Int? = 250

    private var resourceAccessor: ResourceAccessor? = null

    private val fromArray: Array<Column>? by lazy {
        Column.arrayFromNames(fromColumns)
    }

    private val toArray: Array<Column>? by lazy {
        Column.arrayFromNames(toColumns)
    }

    private val updateColumnsString: String by lazy {
        fromArray!!.zip(toArray!!).joinToString(separator = ", ") { (f, t) -> "${f.name} = ${t.name}" }
    }

    private val whereClauseString: String by lazy {
        fromArray!!.zip(toArray!!).joinToString(separator = " OR ") { (f, t) -> "${f.name} != ${t.name}" }
    }

    private val pkArray: Array<Column>? by lazy {
        Column.arrayFromNames(primaryKeyColumns)
    }

    private val orderClauseString: String by lazy {
        pkArray!!.joinToString(separator = ",") { x -> x.name }
    }

    override fun setFileOpener(ra: ResourceAccessor?) {
        resourceAccessor = ra
    }

    override fun getConfirmationMessage(): String =
        "Successfully migrated $fromColumns to $toColumns in $table"

    override fun setUp() {
        Scope.getCurrentScope().getLog(javaClass).info("Initialized BatchMigrationChange")
    }

    override fun validate(db: Database): ValidationErrors {
        val errors = ValidationErrors()
        if (db !is OracleDatabase) {
            return errors
        }

        if (table.isNullOrEmpty()) {
            errors.addError("table is not provided")
        }

        val nullCount = setOf(fromColumns, toColumns).filter { it.isNullOrEmpty() }.count()
        if (nullCount >= 1) {
            errors.addError("Both fromColumns and toColumns need to be provided")
        }

        if (errors.hasErrors()) {
            return errors
        }

        if (chunkSize == null || chunkSize!! <= 0) {
            errors.addError("chunkSize should be provided as a positive integer")
        }

        if (fromArray?.toSet()?.size != fromArray?.size) {
            errors.addError("Duplicate elements in fromColumns")
        }
        if (toArray?.toSet()?.size != toArray?.size) {
            errors.addError("Duplicate elements in toColumns")
        }

        if (fromArray?.size != toArray?.size) {
            errors.addError("fromColumns and toColumns require a 1:1 relationship (unequal column count)")
        }
        else {
            val toSet = toArray!!.toSet()
            fromArray?.forEachIndexed { i: Int, col: Column ->
                if (col == toArray!![i]) {
                    errors.addError("Column $col should not be migrated to itself")
                }
                else if (col in toSet) {
                    errors.addError("Migration of $col crosses, which is not possible")
                }
            }
        }

        if (primaryKeyColumns.isNullOrEmpty()) {
            errors.addError("Primary keys are not defined")
        }

        return errors
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

            do {
                val n = executeMigrationChunk(conn)
                Thread.sleep(10000L)
            } while (n > 0)
        } catch (e: CustomChangeException) {
            throw e
        }
    }

    private fun executeMigrationChunk(conn: JdbcConnection): Int {
        try {
            // Fetch only the rows where not all values are synced yet
            val query = """
                UPDATE $table
                SET $updateColumnsString
                WHERE rowId IN
                (SELECT rowId
                      FROM $table
                      WHERE $whereClauseString
                      FETCH NEXT $chunkSize ROWS ONLY                  
                )
            """.trimIndent() // ORDER BY $orderClauseString
            println("Executing migration chunk:")
            println(query)
            if (1 > 0) {
                // TODO: remove this :), temporary
                return 1
            }

            val stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)

            return stmt.executeUpdate()
        } catch (e: SQLException) {
            throw CustomChangeException("Could not update $table in batch", e)
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

    private fun queryMaxRowId(conn: JdbcConnection): RowId {
        val query = "SELECT max(rowId) as rws from :table FETCH FIRST 1 ROWS ONLY"
        try {
            val stmt = conn.prepareStatement(query)
            stmt.setString(1, table)

            val rs = stmt.executeQuery()
            rs.next()
            val rows = rs.getRowId("rws")
            print("Retrieved row count for $table: $rows")
            return rows
        } catch (e: SQLException) {
            throw CustomChangeException("Can not count amount of rows in table $table", e)
        }
    }

    // We do not need rollbacks for this as it is not semantics, even though we 'could' by an inverse operation
    override fun rollback(db: Database) {
        Scope.getCurrentScope().getLog(javaClass).info("Rollback requested: no-op result")
    }

    override fun toString(): String {
        return "BatchMigrationChange(table=$table, fromColumns=$fromColumns, toColumns=$toColumns, primaryKeyColumns=$primaryKeyColumns, chunkSize=$chunkSize, resourceAccessor=$resourceAccessor)"
    }
}
