package liquibase.ext.rewrites

import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.mockk
import io.mockk.spyk
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

    private val accessor = ClassLoaderResourceAccessor()
    private val generator = SqlGeneratorFactory.getInstance()

    private val xmlParser = ChangeLogParserFactory.getInstance().getParser("xml", accessor)



    @BeforeEach
    fun init() {
        SqlGeneratorFactory.reset()
    }

    @Test
    fun `Change should be of the correct subtype when rewrite is enabled`() {
        val log = xmlParser.parse(rewriteChangeLog, ChangeLogParameters(getCompatibleOracleSpy()), accessor)
        assertEquals(1, log.changeSets.size)
        assertEquals(1, log.changeSets[0].changes.size)
        assertThat(log.changeSets.first().changes.first(), instanceOf(DropIndexOnline::class.java))
    }

    @Test
    fun `Change should be of the correct subtype when rewrite is disabled`() {
        val log = xmlParser.parse(originalChangeLog, ChangeLogParameters(getCompatibleOracleSpy()), accessor)
        assertEquals(1, log.changeSets.size)
        assertEquals(1, log.changeSets[0].changes.size)
        assertThat(log.changeSets.first().changes.first(), instanceOf(DropIndexOnline::class.java))
    }

    @Test
    fun `Change statement for online DDL is supposed to generate Oracle code when compatible`() {
        val db = getCompatibleOracleSpy()
        val log = xmlParser.parse(rewriteChangeLog, ChangeLogParameters(db), accessor)
        val change = log.changeSets.first().changes.first()
        assertThat(generator.generateSql(change, db).joinToString(separator = "\n") { it.toSql() },
                endsWithIgnoringCase("online"))
    }

    @Test
    fun `Change statement for online DDL is supposed to generate the same code as usual when Oracle is incompatible`() =
            checkNoRewrites(getIncompatibleOracleSpy())


    @Test
    fun `Change statement for online DDL is supposed to generate the same code as usual when database is not supported`() =
            checkNoRewrites(MySQLDatabase())

    private fun checkNoRewrites(db: Database) {
        val log = xmlParser.parse(rewriteChangeLog, ChangeLogParameters(db), accessor)
        val change = log.changeSets.first().changes.first()
        val proxiedGen = genSqlString(change, db)
        generator.unregister(DropIndexOnlineGenerator::class.java)
        val gen = genSqlString(change, db)

        assertEquals(proxiedGen, gen)
    }

    @Test
    fun `Change statement that will be rewritten will be deemed valid`() {
        val db = getCompatibleOracleSpy()
        val log = xmlParser.parse(rewriteChangeLog, ChangeLogParameters(db), accessor)
        val errors = log.changeSets.first().changes.flatMap { it.validate(db).errorMessages.toSet() }
        assertEquals(0, errors.count())
    }

    private fun genSqlString(change : Change, db : Database) : String =
            generator.generateSql(change, db).joinToString(separator = "\n") { it.toSql() }.trim()

    private fun getCompatibleOracleSpy() = spyk<OracleDatabase>().apply {
        every { databaseMajorVersion } returns 12
        every { databaseProductName } returns "Oracle Database 12c Enterprise Edition Release"
    }

    private fun getIncompatibleOracleSpy() = spyk<OracleDatabase>().apply {
        every { databaseMajorVersion } returns 11
        every { databaseProductName } returns "Oracle Database 11g"
    }
}