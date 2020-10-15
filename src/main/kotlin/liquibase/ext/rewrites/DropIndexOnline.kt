package liquibase.ext.rewrites

import liquibase.change.DatabaseChange
import liquibase.change.core.DropIndexChange
import liquibase.database.Database
import liquibase.database.core.OracleDatabase
import liquibase.ext.rewrites.WrapperStatement.*
import liquibase.statement.SqlStatement
import liquibase.statement.core.DropIndexStatement

@DatabaseChange(
        name = "dropIndex",
        description = "Drops an existing index (online if supported and enabled)",
        priority = 2,
        appliesTo = ["index"]
)
class DropIndexOnline : DropIndexChange(), RewritableChange {
    override fun generateStatements(db: Database): Array<SqlStatement> = super.generateStatements(db).rewriteFirstStatement(changeSet, db) {
        when (it) {
            is DropIndexStatement -> DropIndexOnlineWrapperStatement(it)
            else -> it
        }
    }

    override fun supportsOnlineRewriteForDatabase(db: Database): Boolean = db is OracleDatabase
}