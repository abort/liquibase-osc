package liquibase.ext.expand.drop.column

import liquibase.statement.AbstractSqlStatement

data class PrepareDropColumnStatement(
        val catalogName: String?,
        val schemaName: String?,
        val tableName: String?,
        val columns: Set<String?>
) : AbstractSqlStatement()