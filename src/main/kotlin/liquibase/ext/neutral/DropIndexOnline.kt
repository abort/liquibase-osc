package liquibase.ext.neutral

import liquibase.change.DatabaseChange
import liquibase.change.core.DropIndexChange
import liquibase.database.Database
import liquibase.database.core.OracleDatabase
import liquibase.ext.expand.drop.index.DropIndexOnlineStatement
import liquibase.statement.SqlStatement
import liquibase.statement.core.DropIndexStatement

@DatabaseChange(name = "dropIndex", description = "Drops an existing index (online if supported)", priority = 2, appliesTo = ["index"])
class DropIndexOnline : DropIndexChange() {
    override fun generateStatements(db: Database): Array<SqlStatement> = super.generateStatements(db).map {
        when (it) {
            is DropIndexStatement -> DropIndexOnlineWrapperStatement(it)
            else -> it
        }
    }.toTypedArray()


}