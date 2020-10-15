package liquibase.ext.rewrites

import liquibase.changelog.ChangeSet
import liquibase.database.Database
import liquibase.ext.helpers.ArrayUtils.mapFirstIf
import liquibase.ext.helpers.PropertyUtils
import liquibase.statement.SqlStatement

interface RewritableChange {
    fun shouldRewrite(changeSet : ChangeSet) : Boolean =
            PropertyUtils.getProperty(changeSet, "auto-online-ddl", "false").toBoolean()

    fun Array<SqlStatement>.rewriteFirstStatement(
            changeSet: ChangeSet,
            db : Database,
            f: (SqlStatement) -> SqlStatement
    ) : Array<SqlStatement> =
        mapFirstIf(supportsOnlineRewriteForDatabase(db) && shouldRewrite(changeSet)) {
            f(it)
        }

    /**
     * A function that determines whether online translations are possible for this change and database
     */
    fun supportsOnlineRewriteForDatabase(db : Database) : Boolean
}