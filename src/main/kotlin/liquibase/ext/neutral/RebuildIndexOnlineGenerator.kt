package liquibase.ext.neutral

import liquibase.database.Database
import liquibase.database.core.OracleDatabase
import liquibase.exception.ValidationErrors
import liquibase.ext.base.BaseSqlGenerator
import liquibase.sql.Sql
import liquibase.sql.UnparsedSql
import liquibase.sqlgenerator.SqlGeneratorChain

class RebuildIndexOnlineGenerator : BaseSqlGenerator<RebuildIndexOnlineStatement>() {
    override fun supports(stmt: RebuildIndexOnlineStatement, db: Database): Boolean =
            db is OracleDatabase

    // Only validate for oracle
    override fun validate(
            statement: RebuildIndexOnlineStatement,
            db: Database,
            generatorChain: SqlGeneratorChain<RebuildIndexOnlineStatement>
    ): ValidationErrors {
        val errors = ValidationErrors();
        errors.checkRequiredField("index", statement.index);
        return errors
    }

    override fun generate(stmt: RebuildIndexOnlineStatement,
                          db: Database,
                          generatorChain: SqlGeneratorChain<RebuildIndexOnlineStatement>): Array<Sql> = stmt.run {
        val sb = StringBuilder("ALTER INDEX ")
        sb.append(db.escapeIndexName(catalog, schema, index))
        sb.append(" REBUILD ONLINE")
        arrayOf(UnparsedSql(sb.toString()))
    }
}