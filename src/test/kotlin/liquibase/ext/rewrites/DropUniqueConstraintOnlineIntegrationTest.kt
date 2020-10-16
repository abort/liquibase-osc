package liquibase.ext.rewrites

import liquibase.ext.util.TestUtils.DropConstraintOnlineRegex
import org.hamcrest.MatcherAssert.*
import org.hamcrest.core.StringEndsWith
import org.hamcrest.text.MatchesPattern
import java.util.regex.Pattern

class DropUniqueConstraintOnlineIntegrationTest : RewriteIntegrationTest(
        rewriteChangeLogXml = "rewrites/drop/unique/rewrite.xml",
        originalChangeLogXml = "rewrites/drop/unique/original.xml",
        changeClass = DropUniqueConstraintOnline::class,
        checkOnlineOracleDDLString = { sql ->
            assertThat(
                    "SQL should be a Oracle online DDL statement",
                    sql.toLowerCase(),
                    MatchesPattern(DropConstraintOnlineRegex)
            )
        }
)