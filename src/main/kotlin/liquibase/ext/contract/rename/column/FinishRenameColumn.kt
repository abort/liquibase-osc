package liquibase.ext.contract.rename.column

import liquibase.change.AbstractChange
import liquibase.change.ChangeMetaData
import liquibase.change.DatabaseChange
import liquibase.database.Database
import liquibase.database.core.OracleDatabase
import liquibase.ext.expand.drop.column.PrepareDropColumnStatement
import liquibase.ext.helpers.SyncTriggerStatement
import liquibase.statement.SqlStatement

/**
 * Guarantees: no one uses old column anymore, therefore we can remove the trigger first and then the column (with autocommit)
 */
@DatabaseChange(
        name = "finishRenameColumn",
        description = "Finishes renaming an existing column",
        priority = ChangeMetaData.PRIORITY_DEFAULT,
        appliesTo = ["column"]
)
class FinishRenameColumn : AbstractChange() {
    var catalogName: String? = null
    var schemaName: String? = null
    var tableName: String? = null
    var oldColumnName: String? = null
    var newColumnName: String? = null

    override fun generateStatements(db: Database): Array<SqlStatement> = arrayOf(
            SyncTriggerStatement.DropSyncTriggerStatement(tableName, oldColumnName, newColumnName),
            PrepareDropColumnStatement(catalogName, schemaName, tableName, setOf(oldColumnName))
    )

    /**
     * TODO: could be possible if we specify the datatype again:
     * 1. create old column with same datatype
     * 2. create sync trigger between new and old
     * 3. update old column with new values
     */
    override fun supportsRollback(db: Database): Boolean = false
    override fun supports(db: Database): Boolean = db is OracleDatabase

    override fun getConfirmationMessage(): String = "Finished Rename Column"
}