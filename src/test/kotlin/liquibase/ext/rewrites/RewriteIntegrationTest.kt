package liquibase.ext.rewrites

import io.mockk.every
import io.mockk.spyk
import liquibase.change.Change
import liquibase.changelog.ChangeLogParameters
import liquibase.changelog.ChangeSet
import liquibase.changelog.DatabaseChangeLog
import liquibase.database.Database
import liquibase.database.core.MySQLDatabase
import liquibase.database.core.OracleDatabase
import liquibase.parser.ChangeLogParser
import liquibase.parser.ChangeLogParserFactory
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.sqlgenerator.SqlGeneratorFactory
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

abstract class RewriteIntegrationTest(
    val rewriteChangeLogXml: String,
    val originalChangeLogXml: String,
    val changeClass: KClass<out RewritableChange>,
    val checkOnlineOracleDDLString: (String) -> Unit
) {
    companion object {
        private const val ChangeLogExtensionXml = ".xml"
    }

    protected val accessor = ClassLoaderResourceAccessor()
    protected val generator: SqlGeneratorFactory = SqlGeneratorFactory.getInstance()

    private val parserFactory: ChangeLogParserFactory = ChangeLogParserFactory.getInstance()
    protected val xmlParser: ChangeLogParser = parserFactory.getParser(ChangeLogExtensionXml, accessor)

    @BeforeEach
    fun init() {
        SqlGeneratorFactory.reset()
    }

    @Test
    fun `Change should be of the correct subtype when rewrite is enabled`() {
        val log = xmlParser.parse(rewriteChangeLogXml, ChangeLogParameters(getCompatibleOracleSpy()), accessor)
        checkSingleAndCorrectSubType(log.changeSets)
    }

    private fun checkSingleAndCorrectSubType(changeSets: List<ChangeSet>) {
        assertEquals(1, changeSets.size)
        assertEquals(1, changeSets[0].changes.size)
        assertThat(changeSets.first().changes.first(), instanceOf(changeClass.java))
    }

    @Test
    fun `Change should be of the correct subtype when rewrite is disabled`() {
        val log = xmlParser.parse(originalChangeLogXml, ChangeLogParameters(getCompatibleOracleSpy()), accessor)
        checkParsedToCorrectChange(log)
    }

    @Test
    fun `Rewritten change should result in the same checksum as the original`() {
        val originalLog = xmlParser.parse(originalChangeLogXml, ChangeLogParameters(getCompatibleOracleSpy()), accessor)
        val rewriteLog = xmlParser.parse(rewriteChangeLogXml, ChangeLogParameters(getCompatibleOracleSpy()), accessor)
        val pairs = originalLog.changeSets.zip(rewriteLog.changeSets)
        assertTrue(
            pairs.all { (o, r) ->
                o.generateCheckSum() == r.generateCheckSum()
            },
            "All rewritten changes should have the same checksum as their original"
        )

        pairs.forEach {
            val checksumsOriginal = it.first.changes.map { c -> c.generateCheckSum() }
            val checksumsRewrite = it.second.changes.map { c -> c.generateCheckSum() }
            assertEquals(checksumsOriginal, checksumsRewrite, "Rewritten checksums should equal their original")
        }
    }

    private fun checkParsedToCorrectChange(log: DatabaseChangeLog) {
        assertEquals(1, log.changeSets.size)
        assertEquals(1, log.changeSets[0].changes.size)
        assertThat(log.changeSets.first().changes.first(), instanceOf(changeClass.java))
    }

    @Test
    fun `Change statement for online DDL should generate Oracle code when compatible`() {
        val db = getCompatibleOracleSpy()
        val log = xmlParser.parse(rewriteChangeLogXml, ChangeLogParameters(db), accessor)
        val change = log.changeSets.first().changes.first()

        checkOnlineOracleDDLString(generator.generateSql(change, db).joinToString(separator = "\n") { it.toSql() })
    }

    @Test
    fun `Change statement for online DDL should generate the same code as usual when Oracle is incompatible`() =
        checkNoRewrites(getIncompatibleOracleSpy())

    @Test
    fun `Change statement for online DDL should generate the same code as usual when database is not supported`() =
        checkNoRewrites(MySQLDatabase())

    private fun checkNoRewrites(db: Database) = checkNoRewrites(rewriteChangeLogXml, db)
    protected fun checkNoRewrites(xmlPath: String, db: Database) {
        val log = xmlParser.parse(xmlPath, ChangeLogParameters(db), accessor)
        val change = log.changeSets.first().changes.first()
        val proxiedGen = genSqlString(change, db)
        generator.unregister(DropIndexOnlineGenerator::class.java)
        val gen = genSqlString(change, db)

        assertEquals(proxiedGen, gen)
    }

    @Test
    fun `Change statement that will be rewritten should be valid`() {
        val db = getCompatibleOracleSpy()
        val log = xmlParser.parse(rewriteChangeLogXml, ChangeLogParameters(db), accessor)
        val errors = log.changeSets.first().changes.flatMap { it.validate(db).errorMessages.toSet() }
        assertEquals(0, errors.count())
    }

    private fun genSqlString(change: Change, db: Database): String =
        generator.generateSql(change, db).joinToString(separator = "\n") { it.toSql() }.trim()

    protected fun getCompatibleOracleSpy() = spyk<OracleDatabase>().apply {
        every { databaseMajorVersion } returns 19
        every { databaseProductVersion } returns "Oracle Database 19c Enterprise Edition Release"
    }

    protected fun getIncompatibleOracleSpy() = spyk<OracleDatabase>().apply {
        every { databaseMajorVersion } returns 11
        every { databaseProductVersion } returns "Oracle Database 11g Community Edition Release"
    }
}
