package liquibase.ext.neutral

import liquibase.statement.AbstractSqlStatement
import liquibase.statement.core.DropIndexStatement

data class DropIndexOnlineWrapperStatement(val original : DropIndexStatement) : AbstractSqlStatement()