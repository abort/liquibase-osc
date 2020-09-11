package liquibase.ext.contract.drop.column

import com.datical.liquibase.ext.appdba.markunused.change.MarkUnusedStatement
import liquibase.change.AbstractChange
import liquibase.database.Database
import liquibase.ext.expand.drop.column.PrepareDropColumn
import liquibase.ext.expand.drop.column.PrepareDropColumnStatement
import liquibase.ext.helpers.SyncTriggerStatement
import liquibase.ext.helpers.SyncTriggerStatement.*
import liquibase.statement.SqlStatement


// TODO: ALTER TABLE X DROP UNUSED COLUMNS CHECKPOINT Y;
//class FinishDropColumn : AbstractChange()