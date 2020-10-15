package liquibase.ext.rewrites

import liquibase.database.Database
import liquibase.database.core.OracleDatabase
import liquibase.exception.ValidationErrors
import liquibase.ext.base.RewriteBaseSqlGenerator
import liquibase.ext.helpers.ArrayUtils.mapFirst
import liquibase.ext.helpers.ArrayUtils.mapFirstIf
import liquibase.sql.Sql
import liquibase.sql.UnparsedSql
import liquibase.sqlgenerator.SqlGeneratorChain

class DropForeignKeyConstraintOnlineGenerator : RewriteBaseSqlGenerator<DropForeignKeyOnlineWrapperStatement>() {
    override fun generate(
            stmt: DropForeignKeyOnlineWrapperStatement,
            db: Database,
            generatorChain: SqlGeneratorChain<DropForeignKeyOnlineWrapperStatement>
    ): Array<Sql> = generateOriginal(stmt, db).mapFirst(db) { db, e ->
        when (db) {
            is OracleDatabase -> UnparsedSql("${e.toSql()} ONLINE")
            else -> TODO("This should not occur as wrapper statement should not be generated if not supported")
        }
    }

    override fun validateWrapper(
            stmt: DropForeignKeyOnlineWrapperStatement,
            db: Database,
            chain: SqlGeneratorChain<DropForeignKeyOnlineWrapperStatement>
    ): ValidationErrors = ValidationErrors().apply {
        addWarning("DropForeignKey can not be done online for deferrable constraints")
    }
}