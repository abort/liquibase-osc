package liquibase.ext.changes

import io.mockk.every
import io.mockk.mockk
import liquibase.change.custom.CustomChangeWrapper
import liquibase.changelog.ChangeLogParameters
import liquibase.database.core.OracleDatabase
import liquibase.database.jvm.JdbcConnection
import liquibase.parser.ChangeLogParser
import liquibase.parser.ChangeLogParserFactory
import liquibase.resource.ClassLoaderResourceAccessor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.sql.DatabaseMetaData
import java.sql.PreparedStatement
import java.sql.RowIdLifetime

class BatchMigrationIntegrationTest {
    companion object {
        private const val ChangeLogExtensionXml = ".xml"
    }

    private val accessor = ClassLoaderResourceAccessor()

    private val parserFactory: ChangeLogParserFactory = ChangeLogParserFactory.getInstance()
    private val xmlParser: ChangeLogParser = parserFactory.getParser(ChangeLogExtensionXml, accessor)

    @Test
    fun `xml result in expected class`() {
        val xml = xmlParser.parse("changes/batch-migrate.xml", ChangeLogParameters(OracleDatabase()), accessor)
        assertEquals(1, xml.changeSets.size)
        val changes = xml.changeSets.first().changes
        val customChangeWrapper = changes[0] as CustomChangeWrapper

        assertEquals("cst", customChangeWrapper.getParamValue("tableName"))
        assertEquals("c_phone", customChangeWrapper.getParamValue("fromColumns"))
        assertEquals("c_phone_new", customChangeWrapper.getParamValue("toColumns"))
        assertEquals("1000", customChangeWrapper.getParamValue("chunkSize"))
        assertEquals(BatchMigrationChange::class.java, customChangeWrapper.customChange.javaClass)

        val m = customChangeWrapper.customChange as BatchMigrationChange

        val conn = mockk<JdbcConnection>(relaxed = true)
        val db = mockk<OracleDatabase>()
        val stmt = mockk<PreparedStatement>(relaxed = true)
        val md = mockk<DatabaseMetaData>()
        every { md.rowIdLifetime } returns RowIdLifetime.ROWID_VALID_FOREVER
        every { conn.metaData } returns md
        every { stmt.executeLargeUpdate() } returns 0L // implies done
        every { conn.prepareStatement(any(), any<Int>()) } returns stmt
        every { db.connection } returns conn
        // should make sure that all is set
        customChangeWrapper.generateStatements(db)

        assertEquals("cst", m.tableName)
        assertEquals(1000L, m.chunkSize)
        assertEquals("c_phone", m.fromColumns)
        assertEquals("c_phone_new", m.toColumns)
    }
}
