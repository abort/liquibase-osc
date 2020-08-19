package liquibase.ext.expand.drop.index

import liquibase.change.ChangeMetaData
import liquibase.change.DatabaseChange
import liquibase.database.Database
import liquibase.ext.expand.ExpandableChange
import liquibase.statement.SqlStatement

@DatabaseChange(
        name = "dropIndexOnline",
        description = "Drops an existing index online",
        priority = ChangeMetaData.PRIORITY_DEFAULT,
        appliesTo = ["index"]
)
class DropIndexOnline : ExpandableChange() {
    var schema : String? = null
    var index : String? = null

    override fun getConfirmationMessage(): String = "Dropped Index Online"

    override fun generateStatements(db: Database): Array<SqlStatement> =
            arrayOf(DropIndexOnlineStatement(schema, index))
}