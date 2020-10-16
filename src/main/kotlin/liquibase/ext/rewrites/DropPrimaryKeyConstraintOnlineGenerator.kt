package liquibase.ext.rewrites

import liquibase.change.core.DropPrimaryKeyChange
import liquibase.database.Database
import liquibase.database.core.OracleDatabase
import liquibase.exception.ValidationErrors
import liquibase.ext.base.RewriteBaseSqlGenerator
import liquibase.ext.helpers.ArrayUtils.mapFirst
import liquibase.ext.helpers.ArrayUtils.mapFirstIf
import liquibase.ext.rewrites.WrapperStatement.*
import liquibase.sql.Sql
import liquibase.sql.UnparsedSql
import liquibase.sqlgenerator.SqlGeneratorChain
import liquibase.statement.core.DropPrimaryKeyStatement

class DropPrimaryKeyConstraintOnlineGenerator() : RewriteBaseSqlGenerator<DropPrimaryKeyOnlineWrapperStatement>() {
    override fun generate(
            stmt: DropPrimaryKeyOnlineWrapperStatement,
            db: Database,
            generatorChain: SqlGeneratorChain<DropPrimaryKeyOnlineWrapperStatement>
    ): Array<Sql> = generatorFactory.generateSql(stmt.original, db).mapFirst(db) { db, e ->
        when (db) {
            is OracleDatabase -> UnparsedSql("${e.toSql()} ONLINE")
            else -> e
        }
    }

    override fun validateWrapper(
            stmt: DropPrimaryKeyOnlineWrapperStatement,
            db: Database,
            chain: SqlGeneratorChain<DropPrimaryKeyOnlineWrapperStatement>
    ): ValidationErrors = ValidationErrors().apply {
        addWarning("DropPrimaryKey can not be done online for deferrable constraints")

        if (db is OracleDatabase) {
            val original = stmt.original as DropPrimaryKeyStatement
            if (original.dropIndex) {
                addError("Please split dropping the primary key and dropping index, or disable automatic online DDL rewriting instead")
            }
        }
    }

}