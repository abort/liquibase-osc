package liquibase.ext.neutral

import liquibase.change.AbstractChange
import liquibase.change.ChangeMetaData
import liquibase.change.DatabaseChange

// Enforce a single type of change
// Tries to change 'offline DDL' to online for databases that can handle it
// We wrap similar to how rollback does
//@DatabaseChange(
//        name = "useOnlineDDL",
//        description = "Rewrite 'offline DDL' to 'online DDL' for databases that can handle it",
//        priority = ChangeMetaData.PRIORITY_DEFAULT
//        // appliesTo = ["index"] // useless as it applies to all
//)
//class UseOnlineDDL : AbstractChange() {
//
//}