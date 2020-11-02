package liquibase.ext.rewrites

import liquibase.change.ChangeMetaData
import liquibase.change.DatabaseChange
import liquibase.change.core.DropIndexChange
import liquibase.database.Database
import liquibase.statement.SqlStatement
import liquibase.statement.core.DropIndexStatement

@DatabaseChange(
    name = "dropIndex",
    description = "Drops an existing index (online if supported and enabled)",
    priority = ChangeMetaData.PRIORITY_DEFAULT + 1,
    appliesTo = ["index"]
)
class DropIndexOnline : DropIndexChange(), RewritableChange {
    override fun generateStatements(db: Database): Array<SqlStatement> =
        super.generateStatements(db).rewriteStatements(changeSet, db) {
            when (it) {
                is DropIndexStatement -> DropIndexOnlineWrapperStatement(it)
                else -> it
            }
        }

    override fun supportsOnlineRewriteForDatabase(db: Database): Boolean = db.isRequiredOracleEnterpriseVersion()
}
