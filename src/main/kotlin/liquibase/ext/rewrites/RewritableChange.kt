package liquibase.ext.rewrites

import liquibase.changelog.ChangeSet
import liquibase.database.Database
import liquibase.database.core.OracleDatabase
import liquibase.ext.helpers.ArrayUtils.mapFirstIf
import liquibase.ext.helpers.ArrayUtils.mapIf
import liquibase.ext.helpers.PropertyUtils
import liquibase.statement.SqlStatement

interface RewritableChange {
    companion object {
        private const val OracleRequiredProductInfix = "enterprise edition"
        private const val OracleRequiredMajorVersion = 12
        private const val PropertyRewriteDDL: String = "auto-online-ddl"
        private const val PropertyRewriteDDLDefaultValue = "false"
    }

    fun shouldRewrite(changeSet : ChangeSet) : Boolean =
            PropertyUtils.getProperty(changeSet, PropertyRewriteDDL, PropertyRewriteDDLDefaultValue).toBoolean()


    fun Array<SqlStatement>.rewriteStatements(
            changeSet: ChangeSet,
            db : Database,
            f: (SqlStatement) -> SqlStatement
    ) : Array<SqlStatement> =
        mapIf(supportsOnlineRewriteForDatabase(db) && shouldRewrite(changeSet)) {
            f(it)
        }

    /**
     * A function that determines whether online translations are possible for this change and database
     */
    fun supportsOnlineRewriteForDatabase(db : Database) : Boolean

    /**
     * A provided function that can check whether it is the correct Oracle version to support online DDL
     */
    fun Database.isRequiredEnterpriseVersionIfOracle() : Boolean = this !is OracleDatabase ||
                    (databaseProductName.contains(OracleRequiredProductInfix, ignoreCase = true)
                            && databaseMajorVersion >= OracleRequiredMajorVersion)
}