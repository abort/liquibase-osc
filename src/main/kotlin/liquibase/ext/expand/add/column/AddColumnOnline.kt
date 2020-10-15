package liquibase.ext.expand.add.column

import liquibase.change.*
import liquibase.change.core.AddColumnChange
import liquibase.database.Database
import liquibase.database.core.OracleDatabase
import liquibase.exception.ValidationErrors
import liquibase.ext.expand.ExpandableChange
import liquibase.ext.contract.drop.column.SetUnusedColumnStatement
import liquibase.statement.SqlStatement

@DatabaseChange(
        name = "addColumnOnline",
        description = "Add column online",
        priority = ChangeMetaData.PRIORITY_DEFAULT,
        appliesTo = ["table"]
)
class AddColumnOnline() : ExpandableChange(), ChangeWithColumns<AddColumnConfig> {
    var catalogName: String? = null
    var schemaName: String? = null
    var tableName: String? = null

    // This is used to denote that a table is unused
    // Therefore add column changes are safe for zero-downtime deployments (bypassing nullable/defaults check!)
    var unusedTable: Boolean = false

    private val delegate = AddColumnChange()

    override fun validateAdditionalConstraints(db: Database): ValidationErrors = run {
        val errors = ValidationErrors()
        columns.filter { it.isContract() }.forEach {
            errors.addError("${it.name} column is not an expansion")
        }
        errors
    }

    override fun getConfirmationMessage(): String = "Generated add column online"

    override fun generateStatements(db: Database): Array<SqlStatement> = run {
        val p = this
        delegate.apply {
            catalogName = p.catalogName
            schemaName = p.schemaName
            tableName = p.tableName
            columns = p.columns
        }.run { generateStatements(db) }
    }

    override fun addColumn(cfg: AddColumnConfig) = delegate.addColumn(cfg)
    override fun getColumns(): MutableList<AddColumnConfig> = delegate.columns
    override fun setColumns(cols: MutableList<AddColumnConfig>) = delegate.let {
        columns = cols
    }

    override fun supportsRollback(db: Database): Boolean = when (db) {
        is OracleDatabase -> true
        else -> false
    }

    override fun generateRollbackStatements(db: Database): Array<SqlStatement> = arrayOf(
        SetUnusedColumnStatement(catalogName, schemaName, tableName, columns.map { it.name }.toSet())
    )


    private fun AddColumnConfig.isExpand(): Boolean =
            unusedTable || hasDefaultValue() || (constraints?.isNullable ?: false)

    private fun AddColumnConfig.isContract(): Boolean = !isExpand()
}