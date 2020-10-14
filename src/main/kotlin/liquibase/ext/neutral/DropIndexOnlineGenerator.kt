package liquibase.ext.neutral

import liquibase.database.Database
import liquibase.database.core.OracleDatabase
import liquibase.exception.ValidationErrors
import liquibase.ext.base.BaseSqlGenerator
import liquibase.sql.Sql
import liquibase.sql.UnparsedSql
import liquibase.sqlgenerator.SqlGeneratorChain
import liquibase.sqlgenerator.SqlGeneratorFactory

class DropIndexOnlineGenerator : BaseSqlGenerator<DropIndexOnlineWrapperStatement>() {
    override fun generate(
            stmt: DropIndexOnlineWrapperStatement,
            db: Database,
            generatorChain: SqlGeneratorChain<DropIndexOnlineWrapperStatement>
    ): Array<Sql> = SqlGeneratorFactory.getInstance().generateSql(stmt.original, db).mapFirstIf(db is OracleDatabase) { e ->
            UnparsedSql("${e.toSql()} ONLINE")
        }

    override fun validate(
            stmt: DropIndexOnlineWrapperStatement,
            db: Database,
            chain: SqlGeneratorChain<DropIndexOnlineWrapperStatement>
    ): ValidationErrors =
            SqlGeneratorFactory.getInstance().getGenerators(stmt.original, db).first().validate(stmt.original, db, chain)
}
