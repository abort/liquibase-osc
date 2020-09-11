package liquibase.ext.neutral.constraints

import liquibase.change.ColumnConfig
import liquibase.statement.AbstractSqlStatement
import liquibase.statement.UniqueConstraint
import liquibase.structure.DatabaseObject

data class DropUniqueConstraintOnlineStatement(
    val catalogName: String?,
    val schemaName: String?,
    val tableName: String?,
    val constraintName: String?) : AbstractSqlStatement()