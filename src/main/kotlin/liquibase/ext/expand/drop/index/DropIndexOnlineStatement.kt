package liquibase.ext.expand.drop.index

import liquibase.statement.AbstractSqlStatement

data class DropIndexOnlineStatement(val schema : String?, val index : String?) : AbstractSqlStatement()