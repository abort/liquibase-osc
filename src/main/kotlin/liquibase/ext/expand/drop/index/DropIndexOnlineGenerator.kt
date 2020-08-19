package liquibase.ext.expand.drop.index

import liquibase.database.Database
import liquibase.database.core.OracleDatabase
import liquibase.exception.ValidationErrors
import liquibase.ext.base.BaseSqlGenerator
import liquibase.sql.Sql
import liquibase.sql.UnparsedSql
import liquibase.sqlgenerator.SqlGeneratorChain

class DropIndexOnlineGenerator : BaseSqlGenerator<DropIndexOnlineStatement>() {
    override fun supports(statement: DropIndexOnlineStatement, database: Database): Boolean =
            database is OracleDatabase

    override fun validate(
            statement: DropIndexOnlineStatement,
            db: Database,
            generatorChain: SqlGeneratorChain<DropIndexOnlineStatement>
    ): ValidationErrors {
        val errors = ValidationErrors();
        errors.checkRequiredField("index", statement.index);
        return errors;
    }

    override fun generate(stmt: DropIndexOnlineStatement,
                          db: Database,
                          generatorChain: SqlGeneratorChain<DropIndexOnlineStatement>): Array<Sql> = stmt.run {
            val sb = StringBuilder("DROP INDEX ")
            sb.append(db.escapeIndexName(null, schema, index))
            sb.append(" ONLINE")
            arrayOf(UnparsedSql(sb.toString()))
        }
}