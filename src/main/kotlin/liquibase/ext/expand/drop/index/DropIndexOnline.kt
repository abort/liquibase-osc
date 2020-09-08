package liquibase.ext.expand.drop.index

import liquibase.change.ChangeMetaData
import liquibase.change.DatabaseChange
import liquibase.change.DatabaseChangeProperty
import liquibase.database.Database
import liquibase.database.core.OracleDatabase
import liquibase.exception.ValidationErrors
import liquibase.ext.expand.ExpandableChange
import liquibase.statement.SqlStatement

@DatabaseChange(
        name = "dropIndexOnline",
        description = "Drops an existing index online",
        priority = ChangeMetaData.PRIORITY_DEFAULT,
        appliesTo = ["index"]
)
class DropIndexOnline : ExpandableChange() {
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
            description = "Name of the index to drop"
    )]
    var indexName : String? = null

    override fun getConfirmationMessage(): String = "Dropped Index Online"

    override fun generateStatements(db: Database): Array<SqlStatement> =
            arrayOf(DropIndexOnlineStatement(catalogName, schemaName, tableName, indexName))

    override fun supports(db: Database): Boolean = when(db) {
        is OracleDatabase -> true
        else -> false
    }

    override fun supportsRollback(db: Database): Boolean = false
}