package liquibase.ext.contract.drop.column

import liquibase.change.*
import liquibase.database.Database
import liquibase.database.core.OracleDatabase
import liquibase.exception.ValidationErrors
import liquibase.statement.SqlStatement

@DatabaseChange(
        name = "setUnusedColumn",
        description = "Deprecate existing column online",
        priority = ChangeMetaData.PRIORITY_DEFAULT,
        appliesTo = ["column"]
)
class SetUnusedColumn() : AbstractChange(), ChangeWithColumns<ColumnConfig> {
    var catalogName: String? = null
    var schemaName: String? = null
    var tableName: String? = null

    // We either use single column name or a list of columns
    var columnName: String? = null
    var columnsProxy: MutableList<ColumnConfig> = mutableListOf()

    override fun supports(db: Database): Boolean = db is OracleDatabase
    override fun supportsRollback(database: Database?): Boolean = false

    override fun addColumn(col: ColumnConfig) {
        columnsProxy.add(col)
    }

    override fun getColumns(): MutableList<ColumnConfig> = columnsProxy
    override fun setColumns(cols: MutableList<ColumnConfig>) {
        columnsProxy = cols
    }

    override fun generateStatements(db: Database): Array<SqlStatement> = arrayOf(SetUnusedColumnStatement(
            catalogName,
            schemaName,
            tableName,
            computedColumns()
    ))

    override fun getConfirmationMessage(): String = "generated set unused column"
    override fun validate(db: Database): ValidationErrors {
        val errors = ValidationErrors()
        errors.checkRequiredField("tableName", tableName)
        if (columnName.isNullOrEmpty() && columnsProxy.isEmpty()) {
            errors.addError("Column or columns have to be provided")
        }
        if (!columnName.isNullOrEmpty() && columnsProxy.isNotEmpty()) {
            errors.addError("Column and columns are both provided, but are mutually exclusive")
        }
        if (!supports(db)) {
            errors.addError("Database $db is not supported")
        }
        return errors
    }

    private fun computedColumns(): Set<String?> = if (columnName.isNullOrEmpty()) {
        columnsProxy.map { it.name }.toSet()
    } else {
        setOf(columnName)
    }
}