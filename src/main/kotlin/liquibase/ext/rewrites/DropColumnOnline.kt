package liquibase.ext.rewrites

import liquibase.change.ChangeMetaData
import liquibase.change.DatabaseChange
import liquibase.change.core.DropColumnChange
import liquibase.database.Database
import liquibase.ext.changes.SetUnusedColumnStatement
import liquibase.statement.SqlStatement

@DatabaseChange(
    name = "dropColumn",
    description = "Drop column(s) (online if supported and enabled)",
    priority = ChangeMetaData.PRIORITY_DEFAULT + 1,
    appliesTo = ["column"]
)
class DropColumnOnline : DropColumnChange(), RewritableChange {
    override fun generateStatements(db: Database): Array<SqlStatement> =
        if (shouldRewrite(changeSet) && db.isRequiredOracleEnterpriseVersion()) {
            arrayOf(SetUnusedColumnStatement(catalogName, schemaName, tableName, columns.map { it.name }.toSet()))
        } else {
            super.generateStatements(db)
        }

    override fun supportsOnlineRewriteForDatabase(db: Database): Boolean = db.isRequiredOracleEnterpriseVersion()
}
