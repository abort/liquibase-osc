package liquibase.ext.neutral

import liquibase.change.AbstractChange
import liquibase.change.ChangeMetaData
import liquibase.change.DatabaseChange
import liquibase.change.DatabaseChangeProperty
import liquibase.database.Database
import liquibase.database.core.OracleDatabase
import liquibase.ext.expand.ExpandableChange
import liquibase.statement.SqlStatement

@DatabaseChange(
        name = "disableIndexOnline",
        description = "Disables an existing index online (e.g. for performance reasons when batch importing)",
        priority = ChangeMetaData.PRIORITY_DEFAULT,
        appliesTo = ["index"]
)
class DisableIndexOnline : AbstractChange() {
    @get:[DatabaseChangeProperty(mustEqualExisting = "index.catalog")]
    var catalogName: String? = null

    @get:[DatabaseChangeProperty(mustEqualExisting = "index.schema")]
    var schemaName: String? = null

    @get:[DatabaseChangeProperty(
            mustEqualExisting = "index.table",
            description = "Name of the indexed table.",
            requiredForDatabase = ["sybase", "mysql", "mssql", "mariadb", "asany"]
    )]
    var tableName : String? = null

    @get:[DatabaseChangeProperty(
            mustEqualExisting = "index",
            description = "Name of the index to disable"
    )]
    var indexName : String? = null

    override fun getConfirmationMessage(): String = "Disabled Index Online"

    override fun generateStatements(db: Database): Array<SqlStatement> =
            arrayOf(DisableIndexOnlineStatement(catalogName, schemaName, tableName, indexName))

    // TODO: implement rollback by Rebuild, check if we need volatile?
    override fun generateRollbackStatements(db: Database): Array<SqlStatement> = TODO()

    override fun supports(db: Database): Boolean = when(db) {
        is OracleDatabase -> true
        else -> false
    }

    override fun supportsRollback(db: Database): Boolean = true
}