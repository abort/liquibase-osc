package liquibase.ext.changes

import liquibase.database.Database
import liquibase.database.core.OracleDatabase
import liquibase.exception.ValidationErrors
import liquibase.ext.base.BaseSqlGenerator
import liquibase.sql.Sql
import liquibase.sql.UnparsedSql
import liquibase.sqlgenerator.SqlGeneratorChain
import liquibase.structure.core.Column

class SetUnusedColumnGenerator : BaseSqlGenerator<SetUnusedColumnStatement>() {
    override fun generate(
        stmt: SetUnusedColumnStatement,
        db: Database,
        generatorChain: SqlGeneratorChain<SetUnusedColumnStatement>
    ): Array<Sql> = stmt.run {
        when (db) {
            is OracleDatabase -> {
                val sb = StringBuilder("ALTER TABLE ")
                sb.append(db.escapeTableName(catalogName, schemaName, tableName))
                sb.append(" SET UNUSED (")
                columns.forEach {
                    sb.append(db.escapeObjectName(it, Column::class.java))
                    sb.append(',')
                }
                sb.deleteCharAt(sb.length - 1)
                sb.append(") ONLINE")
                arrayOf(UnparsedSql(sb.toString()))
            }
            else -> TODO()
        }
    }

    override fun validate(
        stmt: SetUnusedColumnStatement,
        db: Database,
        generatorChain: SqlGeneratorChain<SetUnusedColumnStatement>
    ): ValidationErrors = ValidationErrors().apply {
        if (stmt.schemaName == null) {
            addWarning("Schema field is not provided")
        }
        checkRequiredField("tableName", stmt.tableName)
        checkRequiredField("columns", stmt.columns)
    }

    override fun supports(stmt: SetUnusedColumnStatement, db: Database): Boolean = db is OracleDatabase
}
