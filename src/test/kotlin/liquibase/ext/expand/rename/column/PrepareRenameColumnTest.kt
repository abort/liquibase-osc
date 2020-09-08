package liquibase.ext.expand.rename.column

import liquibase.changelog.ChangeLogParameters
import liquibase.database.core.OracleDatabase
import liquibase.parser.ChangeLogParserFactory
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.sqlgenerator.SqlGeneratorFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class PrepareRenameColumnTest {
    private val changeLog = "expand/rename/column/changes.xml"
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
    fun checkOutputFull() {
        val sql = generator.generateSql(lb.changeSets.first().changes.first().generateStatements(db), db)
        Assertions.assertTrue(sql.first().toSql().contains("ALTER TABLE"))
        Assertions.assertTrue(sql[1].toSql().startsWith("UPDATE"))
        Assertions.assertTrue(sql[2].toSql().startsWith("CREATE OR REPLACE TRIGGER"))
    }

    @Test
    fun checkRollbackFull() {
        val sql = generator.generateSql(lb.changeSets.first().changes.first().generateRollbackStatements(db), db)
        val first = sql.first().toSql()
        Assertions.assertTrue(first.startsWith("ALTER TABLE"))
        Assertions.assertTrue(first.contains("SET UNUSED COLUMN"))
        Assertions.assertTrue(sql[1].toSql().startsWith("DROP TRIGGER"))
    }
}