package liquibase.ext.rewrites

import liquibase.database.Database
import liquibase.database.core.OracleDatabase
import liquibase.exception.ValidationErrors
import liquibase.ext.base.RewriteBaseSqlGenerator
import liquibase.ext.helpers.ArrayUtils.mapFirstIf
import liquibase.sql.Sql
import liquibase.sql.UnparsedSql
import liquibase.sqlgenerator.SqlGeneratorChain

class DropUniqueConstraintOnlineGenerator : RewriteBaseSqlGenerator<DropUniqueConstraintOnlineWrapperStatement>() {
    override fun generate(
            stmt: DropUniqueConstraintOnlineWrapperStatement,
            db: Database,
            generatorChain: SqlGeneratorChain<DropUniqueConstraintOnlineWrapperStatement>
    ): Array<Sql> = generatorFactory.generateSql(stmt.original, db).mapFirstIf(db is OracleDatabase) { e ->
        UnparsedSql("${e.toSql()} ONLINE")
    }

    override fun validateWrapper(
            stmt: DropUniqueConstraintOnlineWrapperStatement,
            db: Database,
            chain: SqlGeneratorChain<DropUniqueConstraintOnlineWrapperStatement>
    ): ValidationErrors = ValidationErrors().apply {
        addWarning("DropUniqueConstraint can not be done online for deferrable constraints")
    }
}