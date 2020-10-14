package liquibase.ext.neutral

import liquibase.change.DatabaseChange
import liquibase.change.core.DropUniqueConstraintChange
import liquibase.database.Database
import liquibase.ext.helpers.PropertyUtils
import liquibase.statement.SqlStatement
import liquibase.statement.core.DropUniqueConstraintStatement


@DatabaseChange(
        name = "dropUniqueConstraint",
        description = "Drops an existing unique constraint (online if supported)",
        priority = 2,
        appliesTo = ["uniqueConstraint"]
)
class DropUniqueConstraintOnline : DropUniqueConstraintChange() {
    // TODO: add runIf?
    override fun generateStatements(db: Database): Array<SqlStatement> = super.generateStatements(db).run {
        if (shouldRewrite()) {
            map {
                when (it) {
                    is DropUniqueConstraintStatement -> DropUniqueConstraintOnlineWrapperStatement(it)
                    else -> it
                }
            }.toTypedArray()
        }
        else {
            this
        }
    }

    private fun shouldRewrite() : Boolean =
            PropertyUtils.getProperty(changeSet, "auto-online-ddl", "false").toBoolean()
}