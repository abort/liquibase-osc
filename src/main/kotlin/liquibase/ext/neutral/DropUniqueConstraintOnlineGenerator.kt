package liquibase.ext.neutral

import liquibase.database.Database
import liquibase.database.core.OracleDatabase
import liquibase.exception.ValidationErrors
import liquibase.ext.base.BaseSqlGenerator
import liquibase.sql.Sql
import liquibase.sql.UnparsedSql
import liquibase.sqlgenerator.SqlGeneratorChain
import liquibase.sqlgenerator.SqlGeneratorFactory

class DropUniqueConstraintOnlineGenerator : BaseSqlGenerator<DropUniqueConstraintOnlineWrapperStatement>() {
    override fun generate(
            stmt: DropUniqueConstraintOnlineWrapperStatement,
            db: Database,
            generatorChain: SqlGeneratorChain<DropUniqueConstraintOnlineWrapperStatement>
    ): Array<Sql> = SqlGeneratorFactory.getInstance().generateSql(stmt.original, db).mapFirstIf(db is OracleDatabase) { e ->
        UnparsedSql("${e.toSql()} ONLINE")
    }

    override fun validate(
            stmt: DropUniqueConstraintOnlineWrapperStatement,
            db: Database,
            chain: SqlGeneratorChain<DropUniqueConstraintOnlineWrapperStatement>
    ): ValidationErrors = run {
        val errors = SqlGeneratorFactory.getInstance().getGenerators(stmt.original, db).first().validate(stmt.original, db, chain)
        errors.addWarning("DropUniqueConstraint can not be done online for deferrable constraints")
        errors
    }
}