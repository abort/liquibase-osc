package liquibase.ext.rewrites

import liquibase.changelog.ChangeLogParameters
import liquibase.ext.util.TestUtils
import org.hamcrest.MatcherAssert
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.text.MatchesPattern
import org.junit.jupiter.api.Test

class DropColumnOnlineTest : RewriteIntegrationTest(
        rewriteChangeLogXml = "rewrites/drop/column/rewrite.xml",
        originalChangeLogXml = "rewrites/drop/column/original.xml",
        changeClass = DropColumnOnline::class,
        checkOnlineOracleDDLString = { sql ->
            assertThat(
                    "SQL should be a Oracle online DDL statement",
                    sql,
                    MatchesPattern(RegexPattern)
            )
        }
) {
    companion object {
        val RegexPattern = """alter\s+table\s\w+(\.\w+)*\s+set unused column\s*\(\s*\w+(\s*,\s*\w+)*\s*\)\s*online""".toRegex(RegexOption.IGNORE_CASE).toPattern()
    }

    @Test
    fun `Change statement for online DDL should generate Oracle code when compatible for single column drops`() {
        val db = getCompatibleOracleSpy()
        val log = xmlParser.parse("rewrites/drop/column/rewrite-single.xml", ChangeLogParameters(db), accessor)
        val change = log.changeSets.first().changes.first()
        checkOnlineOracleDDLString(generator.generateSql(change, db).joinToString(separator = "\n") { it.toSql() })
    }

    @Test
    fun `Change statement for online DDL should generate the same code as usual when database is not supported for single column drops`() {
        val db = getIncompatibleOracleSpy()
        val log = xmlParser.parse("rewrites/drop/column/rewrite-single.xml", ChangeLogParameters(db), accessor)
        val change = log.changeSets.first().changes.first()
        checkOnlineOracleDDLString(generator.generateSql(change, db).joinToString(separator = "\n") { it.toSql() })
    }
}