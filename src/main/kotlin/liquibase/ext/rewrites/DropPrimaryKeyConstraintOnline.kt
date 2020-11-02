package liquibase.ext.rewrites

import liquibase.change.ChangeMetaData
import liquibase.change.DatabaseChange
import liquibase.change.core.DropPrimaryKeyChange
import liquibase.database.Database
import liquibase.statement.SqlStatement
import liquibase.statement.core.DropPrimaryKeyStatement

@DatabaseChange(
    name = "dropPrimaryKey",
    description = "Drops an existing primary key (online if supported and enabled)",
    priority = ChangeMetaData.PRIORITY_DEFAULT + 1,
    appliesTo = ["primaryKey"]
)
class DropPrimaryKeyConstraintOnline : DropPrimaryKeyChange(), RewritableChange {
    override fun generateStatements(db: Database): Array<SqlStatement> =
        super.generateStatements(db).rewriteStatements(changeSet, db) {
            when (it) {
                is DropPrimaryKeyStatement -> DropPrimaryKeyOnlineWrapperStatement(it)
                else -> it
            }
        }

    override fun supportsOnlineRewriteForDatabase(db: Database): Boolean = db.isRequiredOracleEnterpriseVersion()
}
