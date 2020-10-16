package liquibase.ext.rewrites

import liquibase.change.ChangeMetaData
import liquibase.change.DatabaseChange
import liquibase.change.core.CreateIndexChange
import liquibase.database.Database
import liquibase.statement.SqlStatement
import liquibase.statement.core.CreateIndexStatement

@DatabaseChange(
        name = "createIndex",
        description = "Creates an index on an existing column or set of columns (online if supported and enabled)",
        priority = ChangeMetaData.PRIORITY_DEFAULT + 1,
        appliesTo = ["index"]
)
class CreateIndexOnline : CreateIndexChange(), RewritableChange {
    override fun generateStatements(db: Database): Array<SqlStatement> = super.generateStatements(db).rewriteStatements(changeSet, db) {
        when (it) {
            is CreateIndexStatement -> CreateIndexOnlineWrapperStatement(it)
            else -> it
        }
    }

    override fun supportsOnlineRewriteForDatabase(db: Database): Boolean = db.isRequiredEnterpriseVersionIfOracle()
}