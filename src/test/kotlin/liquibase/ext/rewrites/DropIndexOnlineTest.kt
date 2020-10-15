package liquibase.ext.rewrites

import liquibase.change.Change
import liquibase.change.core.DropIndexChange
import liquibase.changelog.ChangeLogParameters
import liquibase.database.Database
import liquibase.database.core.MySQLDatabase
import liquibase.database.core.OracleDatabase
import liquibase.parser.ChangeLogParserFactory
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.sqlgenerator.SqlGeneratorFactory
import liquibase.statement.core.DropIndexStatement
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

// TODO make this generalizable and do it for all online rewrites
class DropIndexOnlineTest {
    private val rewriteChangeLog = "rewrites/drop/index/rewrite.xml"
    private val originalChangeLog = "rewrites/drop/index/original.xml"


    private val db = OracleDatabase()
    private val accessor = ClassLoaderResourceAccessor()
    private val generator = SqlGeneratorFactory.getInstance()

    private val xmlParser = ChangeLogParserFactory.getInstance().getParser("xml", accessor)

    @BeforeEach
    fun init() {
        SqlGeneratorFactory.reset()
    }

    @Test
    fun `Change should be of the correct subtype when rewrite is enabled`() {
        val log = xmlParser.parse(rewriteChangeLog, ChangeLogParameters(db), accessor)
        assertEquals(1, log.changeSets.size)
        assertEquals(1, log.changeSets[0].changes.size)
        assertThat(log.changeSets.first().changes.first(), instanceOf(DropIndexChange::class.java))
    }

    @Test
    fun `Change should be of the correct subtype when rewrite is disabled`() {
        val log = xmlParser.parse(originalChangeLog, ChangeLogParameters(db), accessor)
        assertEquals(1, log.changeSets.size)
        assertEquals(1, log.changeSets[0].changes.size)
        assertThat(log.changeSets.first().changes.first(), instanceOf(DropIndexOnline::class.java))
    }

    @Test
    fun `Change statement for online DDL is supposed to generate Oracle code`() {
        val log = xmlParser.parse(rewriteChangeLog, ChangeLogParameters(db), accessor)
        val change = log.changeSets.first().changes.first()
        assertThat(generator.generateSql(change, db).joinToString(separator = "\n") { it.toSql() },
                endsWithIgnoringCase("online"))
    }

    @Test
    fun `Change statement for online DDL is supposed to generate the same code as usual when database is not supported`() {
        val db = MySQLDatabase()
        val log = xmlParser.parse(rewriteChangeLog, ChangeLogParameters(db), accessor)
        val change = log.changeSets.first().changes.first()
        val proxiedGen = genSqlString(change, db)
        generator.unregister(DropIndexOnlineGenerator::class.java)
        val gen = genSqlString(change, db)

        assertEquals(proxiedGen, gen)
    }

    @Test
    fun `Change statement that will be rewritten will be deemed valid`() {
        val log = xmlParser.parse(rewriteChangeLog, ChangeLogParameters(db), accessor)
        val errors = log.changeSets.first().changes.flatMap { it.validate(db).errorMessages.toSet() }
        assertEquals(0, errors.count())
    }

    private fun genSqlString(change : Change, db : Database) : String =
            generator.generateSql(change, db).joinToString(separator = "\n") { it.toSql() }.trim()
}