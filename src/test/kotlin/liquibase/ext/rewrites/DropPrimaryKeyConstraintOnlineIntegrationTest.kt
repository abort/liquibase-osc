package liquibase.ext.rewrites

import org.hamcrest.MatcherAssert.*
import org.hamcrest.core.StringEndsWith

class DropPrimaryKeyConstraintOnlineIntegrationTest : RewriteIntegrationTest(
        rewriteChangeLogXml = "rewrites/drop/pk/rewrite.xml",
        originalChangeLogXml = "rewrites/drop/pk/original.xml",
        changeClass = DropPrimaryKeyConstraintOnline::class,
        checkOnlineOracleDDLString = { sql ->
            println("SQL: $sql")
            assertThat("SQL should be a Oracle online DDL statement", sql, StringEndsWith(true, "online"))
        }
)