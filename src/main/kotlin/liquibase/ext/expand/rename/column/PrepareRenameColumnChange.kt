package liquibase.ext.expand.rename.column

import liquibase.change.ChangeMetaData
import liquibase.change.DatabaseChange
import liquibase.database.Database
import liquibase.ext.expand.ExpandableChange
import liquibase.ext.expand.drop.column.PrepareDropColumnStatement
import liquibase.ext.helpers.SyncTriggerStatement.*
import liquibase.statement.DatabaseFunction
import liquibase.statement.NotNullConstraint
import liquibase.statement.SqlStatement
import liquibase.statement.core.AddColumnStatement
import liquibase.statement.core.UpdateStatement

@DatabaseChange(
        name = "prepareRenameColumn",
        description = "Prepares to renames an existing column",
        priority = ChangeMetaData.PRIORITY_DEFAULT,
        appliesTo = ["column"]
)
class PrepareRenameColumnChange : ExpandableChange() {
    var catalogName: String? = null
    var schemaName: String? = null
    var tableName: String? = null
    var oldColumnName: String? = null
    var newColumnName: String? = null
    var columnDataType: String? = null
    var remarks: String? = null
    var nullable: Boolean? = null

    // TODO: think about constraints?
    override fun generateStatements(db: Database): Array<SqlStatement> = run {
        val update = UpdateStatement(catalogName, schemaName, tableName)
        update.addNewColumnValue(newColumnName, DatabaseFunction(oldColumnName))
        val add = AddColumnStatement(catalogName, schemaName, tableName, newColumnName, columnDataType, null, remarks)
        if (nullable == false) {
            add.constraints.add(NotNullConstraint(newColumnName))
        }

        arrayOf(
                add,
                update,
                AddSyncTriggerStatement(tableName, oldColumnName, newColumnName)
        )
    }

    override fun supportsRollback(db: Database): Boolean = true

    // TODO: check if this makes sense...
    override fun generateRollbackStatements(db: Database): Array<SqlStatement> = arrayOf(
            PrepareDropColumnStatement(catalogName, schemaName, tableName, setOf(newColumnName)),
            DropSyncTriggerStatement(tableName, oldColumnName, newColumnName)
    )

    override fun getConfirmationMessage(): String = "Generated expand for rename column"
}