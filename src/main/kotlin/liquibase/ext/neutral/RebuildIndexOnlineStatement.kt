package liquibase.ext.neutral

import liquibase.statement.AbstractSqlStatement

class RebuildIndexOnlineStatement(val catalog : String?, val schema : String?, val table : String?, val index : String?) : AbstractSqlStatement()