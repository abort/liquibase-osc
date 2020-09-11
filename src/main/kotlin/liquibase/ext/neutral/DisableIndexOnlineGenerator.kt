package liquibase.ext.neutral

import liquibase.database.Database
import liquibase.database.core.OracleDatabase
import liquibase.exception.ValidationErrors
import liquibase.ext.base.BaseSqlGenerator
import liquibase.sql.Sql
import liquibase.sql.UnparsedSql
import liquibase.sqlgenerator.SqlGeneratorChain

class DisableIndexOnlineGenerator : BaseSqlGenerator<DisableIndexOnlineStatement>() {
    override fun supports(statement: DisableIndexOnlineStatement, database: Database): Boolean =
            database is OracleDatabase

    // Only validate for oracle
    override fun validate(
            statement: DisableIndexOnlineStatement,
            db: Database,
            generatorChain: SqlGeneratorChain<DisableIndexOnlineStatement>
    ): ValidationErrors {
        val errors = ValidationErrors();
        errors.checkRequiredField("index", statement.index);
        return errors
    }

    override fun generate(stmt: DisableIndexOnlineStatement,
                          db: Database,
                          generatorChain: SqlGeneratorChain<DisableIndexOnlineStatement>): Array<Sql> = stmt.run {
        val sb = StringBuilder("ALTER INDEX ")
        sb.append(db.escapeIndexName(catalog, schema, index))
        sb.append(" UNUSABLE ONLINE")
        arrayOf(UnparsedSql(sb.toString()))
    }
}