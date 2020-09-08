package liquibase.ext.contract.drop.column

import liquibase.change.AbstractChange
import liquibase.database.Database
import liquibase.statement.SqlStatement

// TODO: ALTER TABLE X DROP UNUSED COLUMNS CHECKPOINT Y;
class FinishDropColumn : AbstractChange() {
    override fun generateStatements(p0: Database?): Array<SqlStatement> {
        TODO("Not yet implemented")
    }

    override fun getConfirmationMessage(): String {
        TODO("Not yet implemented")
    }
}