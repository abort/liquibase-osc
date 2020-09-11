package liquibase.ext.neutral

import liquibase.change.AbstractChange
import liquibase.change.ChangeMetaData
import liquibase.change.DatabaseChange
import liquibase.change.DatabaseChangeProperty
import liquibase.database.Database
import liquibase.database.core.OracleDatabase
import liquibase.ext.expand.ExpandableChange
import liquibase.statement.SqlStatement

// TODO: add parallel construct etc: see https://docs.oracle.com/database/121/SPATL/alter-index-rebuild.htm#SPATL1017
@DatabaseChange(
        name = "rebuildIndexOnline",
        description = "Rebuilds an existing index online",
        priority = ChangeMetaData.PRIORITY_DEFAULT,
        appliesTo = ["index"]
)
class RebuildIndexOnline : AbstractChange() {
    @get:[DatabaseChangeProperty(mustEqualExisting = "index.catalog")]
    var catalogName: String? = null

    @get:[DatabaseChangeProperty(mustEqualExisting = "index.schema")]
    var schemaName: String? = null

    @get:[DatabaseChangeProperty(
            mustEqualExisting = "index.table",
            description = "Name of the indexed table.",
            requiredForDatabase = ["sybase", "mysql", "mssql", "mariadb", "asany"]
    )]
    var tableName: String? = null

    @get:[DatabaseChangeProperty(
            mustEqualExisting = "index",
            description = "Name of the index to rebuild"
    )]
    var indexName: String? = null

    override fun getConfirmationMessage(): String = "Rebuilt Index Online"

    override fun generateStatements(db: Database): Array<SqlStatement> =
            arrayOf(DisableIndexOnlineStatement(catalogName, schemaName, tableName, indexName))

    // TODO: implement rollback by Rebuild, check if we need volatile?
    override fun generateRollbackStatements(db: Database): Array<SqlStatement> = TODO()

    override fun supports(db: Database): Boolean = when (db) {
        is OracleDatabase -> true
        else -> false
    }

    override fun supportsRollback(db: Database): Boolean = true
}