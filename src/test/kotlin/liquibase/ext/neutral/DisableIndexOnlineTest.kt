package liquibase.ext.neutral

import liquibase.changelog.ChangeLogParameters
import liquibase.database.core.OracleDatabase
import liquibase.parser.ChangeLogParserFactory
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.sqlgenerator.SqlGeneratorFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalStateException

class DisableIndexOnlineTest {
    private val changeLog = "neutral/disable-index.xml"
    private val db = OracleDatabase()
    private val accessor = ClassLoaderResourceAccessor()
    private val lb = ChangeLogParserFactory.getInstance().getParser(changeLog, accessor).parse(changeLog,
            ChangeLogParameters(db), accessor)
    private val changeSets = lb.changeSets
    private val generator = SqlGeneratorFactory.getInstance()

    @Test
    fun checkValidity() {
        val errors = changeSets.first().changes.flatMap { it.validate(db).errorMessages.toSet() }
        Assertions.assertEquals(0, errors.count())
    }

    @Test
    fun checkValidFull() {
        Assertions.assertEquals(0, computeErrors(0).count())
    }


    @Test
    fun checkOutputFull() {
        val sql = generator.generateSql(lb.changeSets.first().changes.first().generateStatements(db), db)
        Assertions.assertEquals("ALTER INDEX my_schema.my_index UNUSABLE ONLINE", sql.first().toSql().trim())
    }

    @Test
    fun checkValidNoSchema() {
        Assertions.assertEquals(0, computeErrors(1).count())
    }

    @Test
    fun checkOutputNoSchema() {
        val sql = generator.generateSql(lb.changeSets[1].changes.first().generateStatements(db), db)
        Assertions.assertEquals("ALTER INDEX foo UNUSABLE ONLINE", sql.first().toSql().trim())
    }

    @Test
    fun checkInvalidNoIndex() {
        Assertions.assertEquals(1, computeErrors(2).count())
    }

    @Test
    fun checkNoOutputMissingIndex() {
        assertThrows<IllegalStateException> {
            generator.generateSql(lb.changeSets[2].changes.first().generateStatements(db), db)
        }
    }

    private fun computeErrors(i : Int) = changeSets[i].changes.flatMap { it.validate(db).errorMessages.toSet() }
}