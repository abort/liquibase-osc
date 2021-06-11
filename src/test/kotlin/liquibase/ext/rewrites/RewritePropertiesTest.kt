package liquibase.ext.rewrites

import io.kotest.core.spec.style.StringSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.merge
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.of
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.stringPattern
import io.kotest.property.arbitrary.take
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.exhaustive
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import liquibase.change.AddColumnConfig
import liquibase.change.Change
import liquibase.change.core.AddPrimaryKeyChange
import liquibase.change.core.CreateIndexChange
import liquibase.change.core.DropColumnChange
import liquibase.change.core.RenameColumnChange
import liquibase.change.core.RenameTableChange
import liquibase.changelog.ChangeLogParameters
import liquibase.changelog.ChangeSet
import liquibase.changelog.DatabaseChangeLog
import liquibase.database.Database
import liquibase.database.core.MSSQLDatabase
import liquibase.database.core.MySQLDatabase
import liquibase.database.core.OracleDatabase
import liquibase.database.core.PostgresDatabase
import liquibase.sqlgenerator.SqlGeneratorFactory
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import kotlin.reflect.KClass
import kotlin.test.assertEquals

class RewritePropertiesTest : StringSpec({
    val supportedChangesArb = Arb.of(
        setOf<KClass<out Change>>(
            CreateIndexOnline::class,
            DropColumnOnline::class,
            DropForeignKeyConstraintOnline::class,
            DropIndexOnline::class,
            DropPrimaryKeyConstraintOnline::class,
            DropUniqueConstraintOnline::class
        )
    )
    // Examples (non exhaustive)
    val unsupportedChangesArb = Arb.of(
        setOf<KClass<out Change>>(
            RenameColumnChange::class,
            RenameTableChange::class,
            AddPrimaryKeyChange::class
        )
    )

    val combinedChangesArb = supportedChangesArb.merge(unsupportedChangesArb)
    val alphaNumArb = Arb.stringPattern("[a-zA-Z0-9]+")
    val columnsArb = arbitrary { rs ->
        val n = Arb.int(1, 3).next(rs)
        val cols = alphaNumArb.take(n, rs).toList()
        cols.map { c ->
            AddColumnConfig().apply {
                name = c
            }
        }
    }

    fun Change.createChangeSet(shouldRewrite: Boolean = true): ChangeSet = run {
        val changeLog = mockk<DatabaseChangeLog>(relaxed = true)
        spyk(ChangeSet(changeLog)).apply {
            every { changes } returns listOf(this@createChangeSet)
            val params = ChangeLogParameters()
            params.set(RewritableChange.PropertyRewriteDDL, shouldRewrite.toString())
            every { changeLogParameters } returns params
        }
    }
    val createIndexCombinedArb = Arb.bind(alphaNumArb.orNull(), alphaNumArb, alphaNumArb, columnsArb) { schema, table, index, cols ->
        Pair(
            CreateIndexOnline().apply {
                changeSet = createChangeSet()
                schemaName = schema
                tableName = table
                indexName = index
                columns = cols
            },
            CreateIndexChange().apply {
                changeSet = createChangeSet()
                schemaName = schema
                tableName = table
                indexName = index
                columns = cols
            }
        )
    }
    val dropColumnCombinedArb = Arb.bind(alphaNumArb.orNull(), alphaNumArb, columnsArb) { schema, table, cols ->
        Pair(
            DropColumnOnline().apply {
                changeSet = createChangeSet()
                schemaName = schema
                tableName = table
                columns = cols
            },
            DropColumnChange().apply {
                changeSet = createChangeSet()
                schemaName = schema
                tableName = table
                columns = cols
            }
        )
    }

    val otherDatabases = listOf(MySQLDatabase(), PostgresDatabase(), MSSQLDatabase()).exhaustive()
    val incompatibleOracleSpy = spyk<OracleDatabase>().apply {
        every { databaseMajorVersion } returns 12
        every { databaseProductVersion } returns "Oracle Database 12c Enterprise Edition Release"
    }
    val compatibleOracleSpy = spyk<OracleDatabase>().apply {
        every { databaseMajorVersion } returns 19
        every { databaseProductVersion } returns "Oracle Database 19c Enterprise Edition Release"
    }
    val generator: SqlGeneratorFactory = SqlGeneratorFactory.getInstance()

    fun Change.toSql(db: Database) = generator.generateSql(this, db).map { it.toSql() }
    fun Change.toRollbackSql(db: Database) = generator.generateSql(this.generateRollbackStatements(db), db).map { it.toSql() }

    val changeArb = Arb.choice(createIndexCombinedArb, dropColumnCombinedArb)
    "rewrites should only occur when Oracle is of version 19c" {
        checkAll(changeArb) { (c, off) ->
            // equality and hashcode implementation are not enforced for changes, hence check on Sql
            val rewritten = c.toSql(compatibleOracleSpy)
            val nonRewritten = c.toSql(incompatibleOracleSpy)
            val original = off.toSql(incompatibleOracleSpy)

            assertThat("statements should be rewritten for Oracle 19c only", rewritten, not(hasItem(`in`(nonRewritten))))
            assertThat("all non-rewritten statements are equivalent to the original", nonRewritten, hasItem(`in`(original)))
        }
    }
    "changes will remain identical on non-Oracle databases" {
        checkAll(otherDatabases, changeArb) { db, (c, off) ->
            val nonRewritten = c.toSql(db)
            val original = off.toSql(db)

            assertThat("all non-rewritten statements are equivalent to the original", nonRewritten, hasItem(`in`(original)))
        }
    }
    "rollback rewrites should not occur when Oracle is not version 19c" {
        checkAll(changeArb.filter { it.first.supportsRollback(compatibleOracleSpy) }) { (c, off) ->
            val nonRewrittenRollback = c.toRollbackSql(incompatibleOracleSpy)
            val originalRollback = off.toRollbackSql(incompatibleOracleSpy)
            assertThat("none of the statements should be rewritten", nonRewrittenRollback, hasItem(`in`(originalRollback)))
        }
    }
    "rollbacks for Online rewritten statements should also use Online DDL" {
        checkAll(changeArb.filter { it.first.supportsRollback(compatibleOracleSpy) }) { (c, off) ->
            val rewrittenRollback = c.toRollbackSql(compatibleOracleSpy)
            val originalRollback = off.toRollbackSql(incompatibleOracleSpy)
            assertThat("all rewritten statements are different from the original statements", rewrittenRollback, not(hasItem(`in`(originalRollback))))
        }
    }
    "rollback remain identical on non-Oracle databases" {
        checkAll(otherDatabases, changeArb) { db, (c, off) ->
            val hasRollback = off.supportsRollback(db)
            assertEquals(hasRollback, c.supportsRollback(db), "Support of rollback should be equal to original")

            if (hasRollback) {
                val nonRewrittenRollback = c.toRollbackSql(db)
                val originalRollback = off.toRollbackSql(db)
                assertThat("none of the statements should be rewritten", nonRewrittenRollback, hasItem(`in`(originalRollback)))
            }
        }
    }
})
