package liquibase.ext.neutral.constraints

import liquibase.changelog.ChangeLogParameters
import liquibase.database.core.*
import liquibase.exception.RollbackImpossibleException
import liquibase.ext.neutral.DropUniqueConstraintOnline
import liquibase.parser.ChangeLogParserFactory
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.sqlgenerator.SqlGeneratorFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalStateException

internal class DropUniqueConstraintOnlineTest {
    private val changeLog = "neutral/constraints/drop-unique.xml"
    private val db = OracleDatabase()
    private val otherDb = PostgresDatabase()
    private val accessor = ClassLoaderResourceAccessor()
    private val lb = ChangeLogParserFactory.getInstance().getParser(changeLog, accessor).parse(changeLog,
            ChangeLogParameters(db), accessor)
    private val changeSets = lb.changeSets
    private val generator = SqlGeneratorFactory.getInstance()

    private val valid = changeSets.first().changes.first()
    private val invalid = changeSets.drop(1).first().changes.first()
    private val valid2 = changeSets.drop(2).first().changes.first()

    @Test
    fun rollback() {
        assertThrows<RollbackImpossibleException> {
            valid.generateRollbackStatements(db)
        }
        assertThrows<RollbackImpossibleException> {
            valid2.generateRollbackStatements(db)
        }
        assertFalse(valid.supportsRollback(db))
        assertFalse(valid2.supportsRollback(db))
    }

    @Test
    fun supports() {
        assertTrue(valid.supports(db))
        assertTrue(valid2.supports(db))
    }

    @Test
    fun generateInvalidShouldFail() {
        assertThrows<IllegalStateException> {
            generator.generateSql(invalid, db)
        }
    }
    @Test
    fun generateTableAndSchemaOnly() {
        val dbs = listOf(db, otherDb)
        for (db in dbs) {
            val sql= generator.generateSql(valid, db)
            val x = sql.first().toSql().trim().toLowerCase()
            assertTrue(x.startsWith("alter table"))
            assertTrue(x.contains("drop constraint"))
            assertTrue(x.contains("my_unique"))

            if (db == this.db) assertTrue(x.contains("online"))
            else assertFalse(x.contains("online"))
        }
    }

    @Test
    fun generateFullyQualified() {
        val dbs = listOf(db, otherDb)
        for (db in dbs) {
            val sql= generator.generateSql(valid2, db)
            val x = sql.first().toSql().trim().toLowerCase()
            assertTrue(x.startsWith("alter table"))
            assertTrue(x.contains("drop constraint"))
            assertTrue(x.contains("my_unique"))
            println(x)
            if (db == this.db) assertTrue(x.contains("online"))
            else assertFalse(x.contains("online"))
        }
    }
}