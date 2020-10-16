package liquibase.ext.rewrites

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.text.MatchesPattern

class DropIndexOnlineTest : RewriteIntegrationTest(
        rewriteChangeLogXml = "rewrites/drop/index/rewrite.xml",
        originalChangeLogXml = "rewrites/drop/index/original.xml",
        changeClass = DropIndexOnline::class,
        checkOnlineOracleDDLString = { sql ->
            assertThat(
                    "SQL should be a Oracle online DDL statement",
                    sql,
                    MatchesPattern("""drop\s+index\s+\w+(\.\w+)*\s+online""".toRegex(RegexOption.IGNORE_CASE).toPattern())
            )
        }
)