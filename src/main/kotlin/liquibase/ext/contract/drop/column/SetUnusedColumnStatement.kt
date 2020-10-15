package liquibase.ext.contract.drop.column

import liquibase.statement.AbstractSqlStatement

data class SetUnusedColumnStatement(
        val catalogName: String?,
        val schemaName: String?,
        val tableName: String?,
        val columns: Set<String?>
) : AbstractSqlStatement()