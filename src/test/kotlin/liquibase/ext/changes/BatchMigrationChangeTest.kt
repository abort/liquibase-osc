package liquibase.ext.changes

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.exhaustive
import io.kotest.property.forAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import liquibase.database.core.OracleDatabase
import liquibase.database.jvm.JdbcConnection
import liquibase.exception.CustomChangeException
import liquibase.ext.generators.ChangeGenerators.otherDatabases
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.assertDoesNotThrow
import java.sql.DatabaseMetaData
import java.sql.PreparedStatement
import java.sql.RowIdLifetime
import java.sql.SQLException
import java.sql.Statement
import liquibase.ext.generators.BatchMigrationGenerator as gen

class BatchMigrationChangeTest : ShouldSpec({
    context("Validation") {
        should("result in errors when not all arguments meet the constraints") {
            forAll(gen.invalidMigrationGenerator) { c ->
                val validated = c.validate(OracleDatabase())
                val errors = validated.errorMessages
                errors.forEach { println(it) }

                validated.hasErrors()
            }
        }
        should("pass when all arguments do not meet the constraints but Oracle is not used") {
            forAll(otherDatabases, gen.invalidMigrationGenerator) { db, c ->
                !c.validate(db).hasErrors()
            }
        }
        should("pass when all arguments meet the constraints") {
            forAll(gen.validMigrationWithSleepsGenerator) { c ->
                !c.validate(OracleDatabase()).hasErrors()
            }
        }
        should("pass when all arguments meet the constraints but Oracle is not used") {
            forAll(otherDatabases, gen.validMigrationWithSleepsGenerator) { db, c ->
                !c.validate(db).hasErrors()
            }
        }
    }
    context("Execution") {
        should("do nothing on non-Oracle databases") {
            checkAll(otherDatabases, gen.validMigrationGenerator) { db, c ->
                val conn = mockk<JdbcConnection>()
                val spyDb = spyk(db)
                every { spyDb.connection } returns conn

                c.execute(spyDb)

                verify(exactly = 0) { conn.prepareStatement(any(), any<Int>()) }
                verify(exactly = 0) { conn.prepareStatement(any()) }
                verify(exactly = 0) { conn.commit() }
            }
        }
        should("stop when rowIds are mutable") {
            checkAll(gen.rowIdLifeTimeInvalidGenerator, gen.validMigrationGenerator) { lt, c ->
                val conn = mockk<JdbcConnection>()
                val md = mockk<DatabaseMetaData>()
                val db = mockk<OracleDatabase>()
                every { db.connection } returns conn
                every { conn.isClosed } returns false
                every { md.rowIdLifetime } returns lt
                every { conn.metaData } returns md

                assertThrows(CustomChangeException::class.java) {
                    c.execute(db)
                }
            }
        }
        should("continue when rowIds are immutable") {
            checkAll(gen.validMigrationGenerator) { c ->
                val conn = mockk<JdbcConnection>(relaxed = true)
                val md = mockk<DatabaseMetaData>()
                val db = mockk<OracleDatabase>()
                every { db.connection } returns conn
                every { conn.isClosed } returns false
                every { md.rowIdLifetime } returns RowIdLifetime.ROWID_VALID_FOREVER
                every { conn.metaData } returns md

                assertDoesNotThrow {
                    c.execute(db)
                }
            }
        }
        should("close statements as many times as commits when execution goes fine") {
            checkAll(gen.validMigrationGenerator) { c ->
                val conn = mockk<JdbcConnection>(relaxed = true)
                val md = mockk<DatabaseMetaData>()
                val db = mockk<OracleDatabase>()
                val stmt = mockk<PreparedStatement>()
                val expectedUpdates = listOf(c.chunkSize!!, c.chunkSize!!, 0L)

                every { db.connection } returns conn
                every { conn.isClosed } returns false
                every { md.rowIdLifetime } returns RowIdLifetime.ROWID_VALID_FOREVER
                every { conn.metaData } returns md
                every { stmt.close() } returns Unit
                every { stmt.executeLargeUpdate() } returnsMany expectedUpdates
                every { conn.prepareStatement(any(), Statement.RETURN_GENERATED_KEYS) } returns stmt

                c.execute(db)

                verify(exactly = expectedUpdates.size) { conn.commit() }
                verify(exactly = expectedUpdates.size) { stmt.close() }
            }
        }
        should("close statements as many times as they are prepared when exceptions may occur") {
            checkAll(gen.validMigrationGenerator) { c ->
                val conn = mockk<JdbcConnection>(relaxed = true)
                val md = mockk<DatabaseMetaData>()
                val db = mockk<OracleDatabase>()
                val stmt = mockk<PreparedStatement>()
                val expectedUpdates = listOf(c.chunkSize!!, c.chunkSize!!, 0L)

                every { db.connection } returns conn
                every { conn.isClosed } returns false
                every { md.rowIdLifetime } returns RowIdLifetime.ROWID_VALID_FOREVER
                every { conn.metaData } returns md
                every { stmt.close() } returns Unit
                every { stmt.executeLargeUpdate() } returnsMany expectedUpdates
                every { conn.commit() } returns Unit andThen Unit andThenThrows SQLException("Anything")
                every { conn.prepareStatement(any(), Statement.RETURN_GENERATED_KEYS) } returns stmt

                try {
                    c.execute(db)
                } catch (e: CustomChangeException) {
                }

                verify(exactly = expectedUpdates.size) { conn.commit() }
                verify(exactly = expectedUpdates.size) { stmt.close() }
            }
        }
        should("stop when the connection is not set") {
            val db = mockk<OracleDatabase>()
            every { db.connection } returns null

            assertThrows(CustomChangeException::class.java) {
                BatchMigrationChange().execute(db)
            }
        }
        should("stop when the connection is closed") {
            val db = mockk<OracleDatabase>()
            val conn = mockk<JdbcConnection>()
            every { conn.isClosed } returns true
            every { db.connection } returns null

            assertThrows(CustomChangeException::class.java) {
                BatchMigrationChange().execute(db)
            }
        }
        should("update in the expected chunks") {
            checkAll(
                listOf(0L, 1L, 2L, 50000L, 912345L).exhaustive(),
                gen.validMigrationGenerator
            ) { allRowCount, migration ->
                val conn = mockk<JdbcConnection>()
                val md = mockk<DatabaseMetaData>()
                val db = mockk<OracleDatabase>()
                val stmt = mockk<PreparedStatement>()

                // We split the batch in chunks where we need one extra update to make sure we migrated all
                val requiredUpdates = (allRowCount / migration.chunkSize!! + 1).toInt()
                // Last result should return 0
                var updateResults = listOf(0L)
                repeat(requiredUpdates - 1) {
                    updateResults = listOf(migration.chunkSize!!) + updateResults
                }

                every { db.connection } returns conn
                every { conn.isClosed } returns false
                every { md.rowIdLifetime } returns RowIdLifetime.ROWID_VALID_FOREVER
                every { conn.metaData } returns md
                every { stmt.executeLargeUpdate() } returnsMany updateResults
                every { conn.prepareStatement(any(), Statement.RETURN_GENERATED_KEYS) } returns stmt
                every { conn.commit() } returns Unit
                every { stmt.close() } returns Unit

                migration.execute(db)

                verify(exactly = requiredUpdates) { conn.prepareStatement(any(), Statement.RETURN_GENERATED_KEYS) }
                verify(exactly = requiredUpdates) { stmt.executeLargeUpdate() }
                verify(exactly = requiredUpdates) { conn.commit() }
            }
        }
        should("only construct expected update statements") {
            checkAll(gen.validMigrationGenerator) { migration ->
                val conn = mockk<JdbcConnection>()
                val md = mockk<DatabaseMetaData>()
                val db = mockk<OracleDatabase>()
                val stmt = mockk<PreparedStatement>()

                val n = migration.chunkSize!!

                val slot = mutableListOf<String>()
                // Should always be 5 updates
                val tail = kotlin.math.max(n / 2L, 1L)

                every { db.connection } returns conn
                every { conn.isClosed } returns false
                every { md.rowIdLifetime } returns RowIdLifetime.ROWID_VALID_FOREVER
                every { conn.metaData } returns md
                every { stmt.executeLargeUpdate() } returnsMany listOf(n, n, n, tail, 0L)
                every { conn.prepareStatement(capture(slot), Statement.RETURN_GENERATED_KEYS) } returns stmt
                every { conn.commit() } returns Unit
                every { stmt.close() } returns Unit

                migration.execute(db)
                verify(exactly = 5) { conn.prepareStatement(any(), Statement.RETURN_GENERATED_KEYS) }
            }
        }
    }
})
