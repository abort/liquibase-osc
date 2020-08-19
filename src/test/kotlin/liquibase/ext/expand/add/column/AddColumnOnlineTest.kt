package liquibase.ext.expand.add.column

import liquibase.changelog.ChangeLogParameters
import liquibase.database.core.OracleDatabase
import liquibase.parser.ChangeLogParserFactory
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.sqlgenerator.SqlGeneratorFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class AddColumnOnlineTest {
    private val changeLog = "expand/add/column/changes.xml"
    private val db = OracleDatabase()
    private val accessor = ClassLoaderResourceAccessor()
    private val lb = ChangeLogParserFactory.getInstance().getParser(changeLog, accessor).parse(changeLog,
            ChangeLogParameters(db), accessor)
    private val changeSets = lb.changeSets
    private val generator = SqlGeneratorFactory.getInstance()

    @Test
    fun validate() {
        val errors = changeSets.first().changes.flatMap { it.validate(db).errorMessages.toSet() }
        Assertions.assertEquals(1, errors.count())
    }
}