package liquibase.ext.rewrites

import liquibase.change.DatabaseChange
import liquibase.change.core.DropColumnChange
import liquibase.database.Database
import liquibase.database.core.OracleDatabase
import liquibase.ext.helpers.ArrayUtils.mapFirstIf
import liquibase.ext.rewrites.WrapperStatement.DropColumnOnlineWrapperStatement
import liquibase.statement.SqlStatement
import liquibase.statement.core.DropColumnStatement

@DatabaseChange(
        name = "dropColumn",
        description = "Drop column(s) (online if supported and enabled)",
        priority = 2,
        appliesTo = ["column"]
)
class DropColumnOnline : DropColumnChange(), RewritableChange {
    override fun generateStatements(db: Database): Array<SqlStatement> = super.generateStatements(db).rewriteFirstStatement(changeSet, db) {
        when (it) {
            is DropColumnStatement -> DropColumnOnlineWrapperStatement(it)
            else -> it
        }
    }

    override fun supportsOnlineRewriteForDatabase(db: Database): Boolean = db is OracleDatabase
}