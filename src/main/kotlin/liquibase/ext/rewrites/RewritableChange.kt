package liquibase.ext.rewrites

import liquibase.changelog.ChangeSet
import liquibase.database.Database
import liquibase.database.core.OracleDatabase
import liquibase.ext.helpers.ArrayUtils.mapIf
import liquibase.statement.SqlStatement

interface RewritableChange {
    companion object {
        private const val OracleRequiredProductInfix: String = "enterprise edition"
        private const val OracleRequiredMajorVersion: Int = 19
        public const val PropertyRewriteDDL: String = "auto-online-ddl"
        const val AutoRewriteByDefault: Boolean = false
    }

    fun shouldRewrite(changeSet: ChangeSet?): Boolean = if (changeSet == null) {
        false
    } else {
        // getValue already checks recursively
        val x = changeSet.changeLogParameters.getValue(PropertyRewriteDDL, changeSet.changeLog) as String?
        x?.toBoolean() ?: AutoRewriteByDefault
    }

    /**
     * TODO: might make it more safe by using LiquibaseUtil.getBuildVersion to check if it is compatible
     */
    fun Array<SqlStatement>.rewriteStatements(
        changeSet: ChangeSet?,
        db: Database,
        f: (SqlStatement) -> SqlStatement
    ): Array<SqlStatement> =
        mapIf(supportsOnlineRewriteForDatabase(db) && shouldRewrite(changeSet)) {
            f(it)
        }

    /**
     * A function that determines whether online translations are possible for this change and database
     */
    fun supportsOnlineRewriteForDatabase(db: Database): Boolean

    /**
     * A provided function that can check whether it is the correct Oracle version to support online DDL
     */
    fun Database.isRequiredOracleEnterpriseVersion(): Boolean = this is OracleDatabase &&
        (databaseProductVersion?.contains(OracleRequiredProductInfix, ignoreCase = true) ?: false) &&
        databaseMajorVersion >= OracleRequiredMajorVersion
}
