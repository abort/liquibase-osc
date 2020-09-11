package liquibase.ext.neutral.constraints

import liquibase.change.AbstractChange
import liquibase.change.ChangeMetaData
import liquibase.change.DatabaseChange
import liquibase.database.Database
import liquibase.database.core.OracleDatabase
import liquibase.statement.SqlStatement

@DatabaseChange(
        name = "dropUniqueConstraintOnline",
        description = "Drops an existing unique constraint online",
        priority = ChangeMetaData.PRIORITY_DEFAULT,
        appliesTo = ["uniqueConstraint"]
)
class DropUniqueConstraintOnline : AbstractChange() {
    var catalogName: String? = null
    var schemaName: String? = null
    var tableName: String? = null
    var constraintName: String? = null

    override fun getConfirmationMessage(): String = "Dropped Unique Constraint online"

    override fun generateStatements(db: Database): Array<SqlStatement> = arrayOf(DropUniqueConstraintOnlineStatement(catalogName, schemaName, tableName, constraintName))

    override fun supports(db: Database): Boolean = db is OracleDatabase
    override fun supportsRollback(db: Database): Boolean = false
}