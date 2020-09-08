package liquibase.ext.expand.drop.index

import liquibase.statement.AbstractSqlStatement

data class DropIndexOnlineStatement(val catalog : String?, val schema : String?, val table : String?, val index : String?) : AbstractSqlStatement()