package liquibase.ext.neutral

import liquibase.statement.AbstractSqlStatement

data class DisableIndexOnlineStatement(val catalog : String?, val schema : String?, val table : String?, val index : String?) : AbstractSqlStatement()