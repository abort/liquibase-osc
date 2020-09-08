package liquibase.ext.contract

import liquibase.changelog.ChangeLogParameters
import liquibase.database.core.OracleDatabase
import liquibase.exception.RollbackImpossibleException
import liquibase.parser.ChangeLogParserFactory
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.sqlgenerator.SqlGeneratorFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class PrepareDropColumnTest {
    private val changeLog = "contract/drop/column/prepare.xml"
    private val db = OracleDatabase()
    private val accessor = ClassLoaderResourceAccessor()
    private val lb = ChangeLogParserFactory.getInstance().getParser(changeLog, accessor).parse(changeLog,
            ChangeLogParameters(db), accessor)
    private val changeSets = lb.changeSets
    private val generator = SqlGeneratorFactory.getInstance()

    private val incomplete = changeSets.first().changes.first()
    private val multiColumn = changeSets.drop(1).first().changes.first()
    private val singleColumn = changeSets.drop(2).first().changes.first()
    private val invalidCombination = changeSets.drop(3).first().changes.first()

    @Test
    fun validate() {
        val errors = incomplete.validate(db).errorMessages.toSet()
        Assertions.assertNotEquals(0, errors.count())

        val successful = multiColumn.validate(db).errorMessages.toSet()
        Assertions.assertEquals(0, successful.count())

        Assertions.assertNotEquals(0, invalidCombination.validate(db).errorMessages.toSet())
    }

    @Test
    fun generate() {
        val sql = generator.generateSql(multiColumn, db)
        Assertions.assertEquals("alter table my_table set unused column (col1,col2);", sql.first().toSql().trim().toLowerCase())
    }

    @Test
    fun rollback() {
        assertThrows<RollbackImpossibleException> {
            multiColumn.generateRollbackStatements(db)
        }
        Assertions.assertFalse(multiColumn.supportsRollback(db))
    }

    @Test
    fun supports() {
        Assertions.assertTrue(multiColumn.supports(db))
    }

    @Test
    fun generateSingle() {
        val sql = generator.generateSql(singleColumn, db)
        Assertions.assertEquals("alter table my_table set unused column (col);", sql.first().toSql().trim().toLowerCase())
    }

    @Test
    fun generateInvalidCombination() {
        val sql = generator.generateSql(invalidCombination, db)
        println(sql.first().toSql().trim().toLowerCase())
    }
}