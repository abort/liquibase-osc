package liquibase.ext.rewrites

import liquibase.change.ChangeMetaData
import liquibase.change.DatabaseChange
import liquibase.change.core.DropUniqueConstraintChange
import liquibase.database.Database
import liquibase.statement.SqlStatement
import liquibase.statement.core.DropUniqueConstraintStatement

@DatabaseChange(
    name = "dropUniqueConstraint",
    description = "Drops an existing unique constraint (online if supported and enabled)",
    priority = ChangeMetaData.PRIORITY_DEFAULT + 1,
    appliesTo = ["uniqueConstraint"]
)
class DropUniqueConstraintOnline : DropUniqueConstraintChange(), RewritableChange {
    override fun generateStatements(db: Database): Array<SqlStatement> =
        super.generateStatements(db).rewriteStatements(changeSet, db) {
            when (it) {
                is DropUniqueConstraintStatement -> DropUniqueConstraintOnlineWrapperStatement(it)
                else -> it
            }
        }

    override fun supportsOnlineRewriteForDatabase(db: Database): Boolean = db.isRequiredOracleEnterpriseVersion()
}
