package liquibase.ext.rewrites

import liquibase.database.Database
import liquibase.database.core.OracleDatabase
import liquibase.ext.base.RewriteBaseSqlGenerator
import liquibase.ext.helpers.ArrayUtils.mapFirst
import liquibase.sql.Sql
import liquibase.sql.UnparsedSql
import liquibase.sqlgenerator.SqlGeneratorChain

class DropIndexOnlineGenerator : RewriteBaseSqlGenerator<DropIndexOnlineWrapperStatement>() {
    override fun generate(
        stmt: DropIndexOnlineWrapperStatement,
        db: Database,
        generatorChain: SqlGeneratorChain<DropIndexOnlineWrapperStatement>
    ): Array<Sql> = generateOriginal(stmt, db).mapFirst(db) { db, e ->
        when (db) {
            is OracleDatabase -> UnparsedSql("${e.toSql()} ONLINE")
            else -> e
        }
    }
}
