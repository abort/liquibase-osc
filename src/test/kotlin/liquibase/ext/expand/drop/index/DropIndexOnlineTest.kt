package liquibase.ext.expand.drop.index

import liquibase.changelog.ChangeLogParameters
import liquibase.database.core.OracleDatabase
import liquibase.parser.ChangeLogParserFactory
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.sqlgenerator.SqlGeneratorFactory
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.lang.IllegalStateException

class DropIndexOnlineTest {
    private val changeLog = "expand/drop/index/changes.xml"
    private val db = OracleDatabase()
    private val accessor = ClassLoaderResourceAccessor()
    private val lb = ChangeLogParserFactory.getInstance().getParser(changeLog, accessor).parse(changeLog,
            ChangeLogParameters(db), accessor)
    private val changeSets = lb.changeSets
    private val generator = SqlGeneratorFactory.getInstance()

    @Test
    fun checkValidity() {
        val errors = changeSets.first().changes.flatMap { it.validate(db).errorMessages.toSet() }
        assertEquals(0, errors.count())
    }

    @Test
    fun checkValidFull() {
        assertEquals(0, computeErrors(0).count())
    }


    @Test
    fun checkOutputFull() {
        val sql = generator.generateSql(lb.changeSets.first().changes.first().generateStatements(db), db)
        assertEquals("DROP INDEX my_schema.my_index ONLINE", sql.first().toSql().trim())
    }

    @Test
    fun checkValidNoSchema() {
        assertEquals(0, computeErrors(1).count())
    }

    @Test
    fun checkOutputNoSchema() {
        val sql = generator.generateSql(lb.changeSets[1].changes.first().generateStatements(db), db)
        assertEquals("DROP INDEX foo ONLINE", sql.first().toSql().trim())
    }

    @Test
    fun checkInvalidNoIndex() {
        assertEquals(1, computeErrors(2).count())
    }

    @Test
    fun checkNoOutputMissingIndex() {
        assertThrows(IllegalStateException::class.java) {
            generator.generateSql(lb.changeSets[2].changes.first().generateStatements(db), db)
        }
    }

    private fun computeErrors(i : Int) = changeSets[i].changes.flatMap { it.validate(db).errorMessages.toSet() }
}