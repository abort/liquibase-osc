package liquibase.ext.rewrites

import liquibase.ext.util.TestUtils
import org.hamcrest.MatcherAssert.*
import org.hamcrest.core.StringEndsWith
import org.hamcrest.text.MatchesPattern

class DropForeignKeyConstraintOnlineIntegrationTest : RewriteIntegrationTest(
        rewriteChangeLogXml = "rewrites/drop/fk/rewrite.xml",
        originalChangeLogXml = "rewrites/drop/fk/original.xml",
        changeClass = DropForeignKeyConstraintOnline::class,
        checkOnlineOracleDDLString = { sql ->
            assertThat("SQL should be a Oracle online DDL statement",
                    sql,
                    MatchesPattern(TestUtils.DropConstraintOnlineRegex)
            )
        }
)