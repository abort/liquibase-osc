package liquibase.ext.contract.rename.column

import liquibase.changelog.ChangeLogParameters
import liquibase.database.core.OracleDatabase
import liquibase.exception.RollbackImpossibleException
import liquibase.parser.ChangeLogParserFactory
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.sqlgenerator.SqlGeneratorFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalStateException

internal class RenameDropColumnTest {
    private val changeLog = "contract/rename/column/changes.xml"
    private val db = OracleDatabase()
    private val accessor = ClassLoaderResourceAccessor()
    private val lb = ChangeLogParserFactory.getInstance().getParser(changeLog, accessor).parse(changeLog,
            ChangeLogParameters(db), accessor)
    private val changeSets = lb.changeSets
    private val generator = SqlGeneratorFactory.getInstance()

    private val valid = changeSets.first().changes.first()
    private val invalid = changeSets.drop(1).first().changes.first()


    @Test
    fun validate() {
        val errors = invalid.validate(db).errorMessages.toSet()
        Assertions.assertNotEquals(0, errors.count())

        val successful = valid.validate(db).errorMessages.toSet()
        Assertions.assertEquals(0, successful.count())
    }

    @Test
    fun generate() {
        val sql = generator.generateSql(valid, db)
        Assertions.assertTrue(sql.first().toSql().trimStart().toLowerCase().startsWith("drop trigger"))
        Assertions.assertEquals("alter table my_table set unused column (old_col) online;", sql[1].toSql().trim().toLowerCase())
    }

    @Test
    fun rollback() {
        assertThrows<RollbackImpossibleException> {
            valid.generateRollbackStatements(db)
        }
        Assertions.assertFalse(valid.supportsRollback(db))
    }

    @Test
    fun supports() {
        Assertions.assertTrue(valid.supports(db))
    }

    @Test
    fun generateInvalidShouldFail() {
        assertThrows<IllegalStateException> {
            generator.generateSql(invalid, db)
        }
    }
}