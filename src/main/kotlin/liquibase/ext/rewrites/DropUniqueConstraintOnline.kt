package liquibase.ext.rewrites

import liquibase.change.DatabaseChange
import liquibase.change.core.DropUniqueConstraintChange
import liquibase.database.Database
import liquibase.database.core.OracleDatabase
import liquibase.ext.rewrites.WrapperStatement.*
import liquibase.statement.SqlStatement
import liquibase.statement.core.DropUniqueConstraintStatement


@DatabaseChange(
        name = "dropUniqueConstraint",
        description = "Drops an existing unique constraint (online if supported and enabled)",
        priority = 2,
        appliesTo = ["uniqueConstraint"]
)
class DropUniqueConstraintOnline : DropUniqueConstraintChange(), RewritableChange {
    override fun generateStatements(db: Database): Array<SqlStatement> =
            super.generateStatements(db).rewriteFirstStatement(changeSet, db) {
                when (it) {
                    is DropUniqueConstraintStatement -> DropUniqueConstraintOnlineWrapperStatement(it)
                    else -> it
                }
            }

    override fun supportsOnlineRewriteForDatabase(db: Database): Boolean = db.isRequiredEnterpriseVersionIfOracle()
}