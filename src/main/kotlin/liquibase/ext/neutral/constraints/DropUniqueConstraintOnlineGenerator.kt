package liquibase.ext.neutral.constraints

import liquibase.database.Database
import liquibase.exception.ValidationErrors
import liquibase.ext.base.BaseSqlGenerator
import liquibase.sql.Sql
import liquibase.sql.UnparsedSql
import liquibase.sqlgenerator.SqlGeneratorChain

class DropUniqueConstraintOnlineGenerator : BaseSqlGenerator<DropUniqueConstraintOnlineStatement>() {
    override fun generate(stmt: DropUniqueConstraintOnlineStatement, db: Database, generatorChain: SqlGeneratorChain<DropUniqueConstraintOnlineStatement>): Array<Sql> = run {
        val sql = StringBuilder("ALTER TABLE ")
        sql.append(db.escapeTableName(stmt.catalogName, stmt.schemaName, stmt.tableName))
        sql.append(" DROP CONSTRAINT ")
        sql.append(db.escapeConstraintName(stmt.constraintName))
        sql.append(" ONLINE");

        // TODO: compute affected objects everywhere and see what they are good for? in 3.10 this has limited support
        arrayOf(UnparsedSql(sql.toString()))
    }

    override fun validate(stmt: DropUniqueConstraintOnlineStatement, db: Database, chain: SqlGeneratorChain<DropUniqueConstraintOnlineStatement>): ValidationErrors = ValidationErrors().apply {
        checkRequiredField("tableName", stmt.tableName)
        checkRequiredField("constraintName", stmt.constraintName)
    }
}