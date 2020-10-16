package liquibase.ext.helpers

import liquibase.database.Database
import liquibase.database.core.OracleDatabase
import liquibase.exception.ValidationErrors
import liquibase.ext.base.BaseSqlGenerator
import liquibase.ext.helpers.SyncTriggerStatement.AddSyncTriggerStatement
import liquibase.sql.Sql
import liquibase.sql.UnparsedSql
import liquibase.sqlgenerator.SqlGeneratorChain


class AddSyncTriggerGenerator : BaseSqlGenerator<AddSyncTriggerStatement>() {
    // TODO: Assumes that left column is still filled in in code!
    override fun generate(stmt: AddSyncTriggerStatement, db: Database, generatorChain: SqlGeneratorChain<AddSyncTriggerStatement>): Array<Sql> = run {
        val sql = """
              CREATE OR REPLACE TRIGGER ${stmt.name} BEFORE INSERT OR UPDATE ON ${db.escapeTableName(null, null, stmt.tableName)} FOR EACH ROW
              BEGIN
                if (:new.${stmt.rightColumn} is null and :new.${stmt.leftColumn} is not null) then
                    :new.${stmt.rightColumn} := :new.${stmt.leftColumn};
                end if;
              EXCEPTION
                WHEN ACCESS_INTO_NULL THEN
                    DBMS_OUTPUT.PUT_LINE('column ${stmt.rightColumn} is dropped');
              END;
        """.trimIndent()
        arrayOf(UnparsedSql(sql))
    }

    override fun validate(stmt: AddSyncTriggerStatement, db: Database, generatorChain: SqlGeneratorChain<AddSyncTriggerStatement>): ValidationErrors = ValidationErrors().apply {
        checkRequiredField("tableName", stmt.tableName)
        checkRequiredField("leftColumn", stmt.leftColumn)
        checkRequiredField("rightColumn", stmt.rightColumn)
    }

    override fun supports(stmt: AddSyncTriggerStatement, db: Database): Boolean =
        db is OracleDatabase
}