package liquibase.ext.helpers

import liquibase.database.Database
import liquibase.database.core.OracleDatabase
import liquibase.exception.ValidationErrors
import liquibase.ext.base.BaseSqlGenerator
import liquibase.ext.helpers.SyncTriggerStatement.DropSyncTriggerStatement
import liquibase.sql.Sql
import liquibase.sql.UnparsedSql
import liquibase.sqlgenerator.SqlGeneratorChain

class DropSyncTriggerGenerator : BaseSqlGenerator<DropSyncTriggerStatement>() {
    override fun generate(
        stmt: DropSyncTriggerStatement,
        db: Database,
        generatorChain: SqlGeneratorChain<DropSyncTriggerStatement>
    ): Array<Sql> = run {
        arrayOf(UnparsedSql("DROP TRIGGER ${stmt.name};"))
    }

    override fun validate(
        stmt: DropSyncTriggerStatement,
        db: Database,
        generatorChain: SqlGeneratorChain<DropSyncTriggerStatement>
    ): ValidationErrors = ValidationErrors().apply {
        checkRequiredField("tableName", stmt.tableName)
        checkRequiredField("leftColumn", stmt.leftColumn)
        checkRequiredField("rightColumn", stmt.rightColumn)
    }

    override fun supports(stmt: DropSyncTriggerStatement, db: Database): Boolean =
        db is OracleDatabase
}
