package liquibase.ext.rewrites

import liquibase.change.ChangeMetaData
import liquibase.change.DatabaseChange
import liquibase.change.core.DropColumnChange
import liquibase.database.Database
import liquibase.database.core.OracleDatabase
import liquibase.ext.contract.drop.column.SetUnusedColumnStatement
import liquibase.statement.SqlStatement
import liquibase.statement.core.DropColumnStatement
import liquibase.structure.core.Column

@DatabaseChange(
        name = "dropColumn",
        description = "Drop column(s) (online if supported and enabled)",
        priority = ChangeMetaData.PRIORITY_DEFAULT + 1,
        appliesTo = ["column"]
)
class DropColumnOnline : DropColumnChange(), RewritableChange {
    override fun generateStatements(db: Database): Array<SqlStatement> = if (isSingleColumn || db !is OracleDatabase) {
        super.generateStatements(db).rewriteStatements(changeSet, db) {
            when (it) {
                is DropColumnStatement -> DropColumnOnlineWrapperStatement(it)
                else -> it
            }
        }
    }
    else {
        arrayOf(SetUnusedColumnStatement(catalogName, schemaName, tableName, columns.map { it.name }.toSet()))
    }

    override fun supportsOnlineRewriteForDatabase(db: Database): Boolean = db.isRequiredEnterpriseVersionIfOracle()

    private val isSingleColumn : Boolean = columns != null && columns.size == 1
}