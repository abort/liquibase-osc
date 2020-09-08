package liquibase.ext.helpers

import liquibase.statement.AbstractSqlStatement
import okio.Buffer

// TODO: add schema name (see: https://docs.oracle.com/cd/B19306_01/server.102/b14200/statements_9005.htm)
sealed class SyncTriggerStatement : AbstractSqlStatement() {
    abstract val tableName : String?
    abstract val leftColumn : String?
    abstract val rightColumn : String?

    val name by lazy {
        Buffer().writeUtf8(tableName.orEmpty())
                .writeUtf8(leftColumn.orEmpty())
                .writeUtf8(rightColumn.orEmpty())
                .sha1().hex()
    }

    data class AddSyncTriggerStatement(override val tableName : String?, override val leftColumn : String?, override val rightColumn : String?) : SyncTriggerStatement()
    data class DropSyncTriggerStatement(override val tableName : String?, override val leftColumn : String?, override val rightColumn : String?) : SyncTriggerStatement()
}