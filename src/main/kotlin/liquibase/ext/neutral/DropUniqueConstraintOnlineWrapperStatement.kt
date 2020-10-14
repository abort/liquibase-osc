package liquibase.ext.neutral

import liquibase.statement.AbstractSqlStatement
import liquibase.statement.core.DropUniqueConstraintStatement

data class DropUniqueConstraintOnlineWrapperStatement(val original :DropUniqueConstraintStatement) : AbstractSqlStatement()