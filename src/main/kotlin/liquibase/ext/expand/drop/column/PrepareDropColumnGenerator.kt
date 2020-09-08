package liquibase.ext.expand.drop.column

import liquibase.database.Database
import liquibase.database.core.OracleDatabase
import liquibase.exception.ValidationErrors
import liquibase.ext.base.BaseSqlGenerator
import liquibase.sql.Sql
import liquibase.sql.UnparsedSql
import liquibase.sqlgenerator.SqlGeneratorChain

class PrepareDropColumnGenerator : BaseSqlGenerator<PrepareDropColumnStatement>() {
    override fun generate(
            stmt: PrepareDropColumnStatement,
            db: Database,
            generatorChain: SqlGeneratorChain<PrepareDropColumnStatement>
    ): Array<Sql> = stmt.run {
        when (db) {
            is OracleDatabase -> {
                val sb = StringBuilder("ALTER TABLE ")
                sb.append(db.escapeTableName(catalogName, schemaName, tableName))
                sb.append(" SET UNUSED COLUMN (")
                columns.forEach {
                    sb.append(db.escapeColumnName(null, null, null, it))
                    sb.append(',')
                }
                sb.deleteCharAt(sb.length - 1)
                sb.append(");")
                arrayOf(UnparsedSql(sb.toString()))
            }
            else -> TODO()
        }
    }

    override fun validate(
            stmt: PrepareDropColumnStatement,
            db: Database,
            generatorChain: SqlGeneratorChain<PrepareDropColumnStatement>
    ): ValidationErrors {
        val errors = ValidationErrors()
        if (stmt.schemaName == null) errors.addWarning("Schema field is not provided")
        errors.checkRequiredField("tableName", stmt.tableName)
        errors.checkRequiredField("columns", stmt.columns)
        return errors
    }
}