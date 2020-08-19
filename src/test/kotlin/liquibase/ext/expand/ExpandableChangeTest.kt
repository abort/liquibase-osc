package liquibase.ext.expand

import liquibase.changelog.ChangeLogParameters
import liquibase.database.Database
import liquibase.database.core.OracleDatabase
import liquibase.exception.ValidationErrors
import liquibase.parser.ChangeLogParserFactory
import liquibase.resource.ClassLoaderResourceAccessor
import liquibase.statement.SqlStatement
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class ExpandableChangeTest {
    private val changeLog = "changelog-expandable.xml"
    private val db = OracleDatabase()
    private val accessor = ClassLoaderResourceAccessor()
    private val lb = ChangeLogParserFactory.getInstance().getParser(changeLog, accessor).parse(changeLog,
            ChangeLogParameters(db), accessor)

    @Test
    fun `expanding changes do not raise errors`() {
        val changeSets = lb.changeSets
        val errors = changeSets.first().changes.flatMap { it.validate(db).errorMessages.toSet() }
        assertEquals(0, errors.count())
    }

    @Test
    fun `contracting changes raise errors`() {
        val changeSets = lb.changeSets
        val errors = changeSets[1].changes.flatMap { it.validate(db).errorMessages.toSet() }
        assertEquals(1, errors.count())
    }
}