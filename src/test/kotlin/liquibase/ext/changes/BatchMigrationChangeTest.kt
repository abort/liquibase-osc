package liquibase.ext.changes

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.property.checkAll
import io.kotest.property.forAll
import io.mockk.every
import io.mockk.mockk
import liquibase.database.core.MySQLDatabase
import liquibase.database.core.OracleDatabase
import liquibase.database.jvm.JdbcConnection
import org.junit.jupiter.api.Assertions.*
import java.sql.DatabaseMetaData
import java.sql.RowIdLifetime
import liquibase.ext.generators.BatchMigrationGenerator as gen

class BatchMigrationChangeTest : ShouldSpec({
    context("Validation") {
        should("Result in errors when not all arguments meet the constraints") {
            forAll(gen.invalidMigrationGenerator) { c ->
                println(c)
                c.validate(OracleDatabase()).hasErrors()
            }
        }
        should("Pass when all arguments do not meet the constraints but Oracle is not used") {
            forAll(gen.invalidMigrationGenerator) { c ->
                !c.validate(MySQLDatabase()).hasErrors()
            }
        }
        should("Pass when all arguments meet the constraints") {
            forAll(gen.validMigrationGenerator) { c ->
                !c.validate(OracleDatabase()).hasErrors()
            }
        }
        should("Succeed when all arguments meet the constraints but Oracle is not used") {
            forAll(gen.validMigrationGenerator) { c ->
                !c.validate(MySQLDatabase()).hasErrors()
            }
        }
    }
    context("Changes") {
        should("Generate proper code for valid migrations") {
            checkAll(gen.validMigrationGenerator) { c ->
                val conn = mockk<JdbcConnection>()
                val md =  mockk<DatabaseMetaData>()
                val db = mockk<OracleDatabase>()
                every { db.connection } returns conn
                every { conn.isClosed } returns false
                every { md.rowIdLifetime } returns RowIdLifetime.ROWID_VALID_FOREVER
                every { conn.metaData } returns md

                c.execute(db)
            }
        }
    }
})
