package liquibase.ext.rewrites

import liquibase.ext.util.TestUtils
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.text.MatchesPattern

class DropPrimaryKeyConstraintOnlineIntegrationTest : RewriteIntegrationTest(
        rewriteChangeLogXml = "rewrites/drop/pk/rewrite.xml",
        originalChangeLogXml = "rewrites/drop/pk/original.xml",
        changeClass = DropPrimaryKeyConstraintOnline::class,
        checkOnlineOracleDDLString = { sql ->
            assertThat("SQL should be a Oracle online DDL statement",
                    sql,
                    MatchesPattern(
                            """alter\s+table\s+\w+(\.\w+)*\s+drop\s+primary\s+key\s+((drop|keep)\s+index\s+)?online"""
                                    .toRegex(RegexOption.IGNORE_CASE).toPattern()
                    )
            )
        }
)