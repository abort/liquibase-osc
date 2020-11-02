package liquibase.ext.rewrites

import org.hamcrest.MatcherAssert
import org.hamcrest.text.MatchesPattern

class CreateIndexOnlineTest : RewriteIntegrationTest(
    rewriteChangeLogXml = "rewrites/create/index/rewrite.xml",
    originalChangeLogXml = "rewrites/create/index/original.xml",
    changeClass = CreateIndexOnline::class,
    checkOnlineOracleDDLString = { sql ->
        MatcherAssert.assertThat(
            "SQL should be a Oracle online DDL statement",
            sql,
            MatchesPattern(
                """create\s+index\s+\w+(\.\w+)*\s+on\s+\w+(\.\w+)*\s*\(\s*\w+(\s*,\s*\w+)*\s*\)\s*online"""
                    .toRegex(RegexOption.IGNORE_CASE).toPattern()
            )
        )
    }
)
