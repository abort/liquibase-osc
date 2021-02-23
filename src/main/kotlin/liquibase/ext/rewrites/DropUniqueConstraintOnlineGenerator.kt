package liquibase.ext.rewrites

import liquibase.database.Database
import liquibase.database.core.OracleDatabase
import liquibase.exception.ValidationErrors
import liquibase.ext.base.RewriteBaseSqlGenerator
import liquibase.ext.helpers.ArrayUtils.mapFirst
import liquibase.sql.Sql
import liquibase.sql.UnparsedSql
import liquibase.sqlgenerator.SqlGeneratorChain

class DropUniqueConstraintOnlineGenerator : RewriteBaseSqlGenerator<DropUniqueConstraintOnlineWrapperStatement>() {
    override fun generate(
        stmt: DropUniqueConstraintOnlineWrapperStatement,
        db: Database,
        generatorChain: SqlGeneratorChain<DropUniqueConstraintOnlineWrapperStatement>
    ): Array<Sql> = getGeneratorFactory().generateSql(stmt.original, db).mapFirst(db) { db, e ->
        when (db) {
            is OracleDatabase -> UnparsedSql("${e.toSql()} ONLINE")
            else -> e
        }
    }

    override fun validateWrapper(
        stmt: DropUniqueConstraintOnlineWrapperStatement,
        db: Database,
        chain: SqlGeneratorChain<DropUniqueConstraintOnlineWrapperStatement>
    ): ValidationErrors = ValidationErrors().apply {
        addWarning("DropUniqueConstraint can not be done online for deferrable constraints")
    }
}
