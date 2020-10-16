package liquibase.ext.rewrites

import liquibase.change.ChangeMetaData
import liquibase.change.DatabaseChange
import liquibase.change.core.DropForeignKeyConstraintChange
import liquibase.database.Database
import liquibase.statement.SqlStatement
import liquibase.statement.core.DropForeignKeyConstraintStatement

@DatabaseChange(
        name = "dropForeignKeyConstraint",
        description = "Drops an existing foreign key (online if supported and enabled)",
        priority = ChangeMetaData.PRIORITY_DEFAULT + 1,
        appliesTo = ["foreignKey"]
)
class DropForeignKeyConstraintOnline : DropForeignKeyConstraintChange(), RewritableChange {
    override fun generateStatements(db: Database): Array<SqlStatement> = super.generateStatements(db).rewriteStatements(changeSet, db) {
        when (it) {
            is DropForeignKeyConstraintStatement -> DropForeignKeyOnlineWrapperStatement(it)
            else -> it
        }
    }

    override fun supportsOnlineRewriteForDatabase(db: Database): Boolean = db.isRequiredOracleEnterpriseVersion()
}