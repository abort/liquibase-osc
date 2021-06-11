package liquibase.ext.rewrites

import io.kotest.core.spec.style.StringSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import liquibase.change.Change
import liquibase.changelog.ChangeLogParameters
import liquibase.changelog.ChangeSet
import liquibase.changelog.DatabaseChangeLog
import liquibase.database.Database
import liquibase.ext.generators.ChangeGenerators.compatibleDatabases
import liquibase.ext.generators.ChangeGenerators.compatibleOracleSpy
import liquibase.ext.generators.ChangeGenerators.createIndexCombinedArb
import liquibase.ext.generators.ChangeGenerators.dropColumnCombinedArb
import liquibase.ext.generators.ChangeGenerators.dropForeignKeyCombinedArb
import liquibase.ext.generators.ChangeGenerators.dropIndexCombinedArb
import liquibase.ext.generators.ChangeGenerators.dropPrimaryKeyCombinedArb
import liquibase.ext.generators.ChangeGenerators.dropUniqueConstraintCombinedArb
import liquibase.ext.generators.ChangeGenerators.incompatibleDatabases
import liquibase.ext.generators.ChangeGenerators.incompatibleOracleSpy
import liquibase.ext.generators.ChangeGenerators.otherDatabases
import liquibase.sqlgenerator.SqlGeneratorFactory
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`in`
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.not
import kotlin.test.assertEquals

class RewritePropertiesTest : StringSpec({
    val generator = SqlGeneratorFactory.getInstance()

    fun Change.toSql(db: Database) = generator.generateSql(this, db).map { it.toSql() }
    fun Change.toRollbackSql(db: Database) = generator.generateSql(this.generateRollbackStatements(db), db).map {
        it.toSql()
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

    fun Pair<Change, Change>.addContext(rewrites: Boolean = true) = Pair(
        first.apply { changeSet = createChangeSet(rewrites) },
        second.apply { changeSet = createChangeSet(rewrites) }
    )

    val rewritableChangesArb = Arb.choice(
        createIndexCombinedArb, dropColumnCombinedArb, dropForeignKeyCombinedArb,
        dropIndexCombinedArb, dropPrimaryKeyCombinedArb, dropUniqueConstraintCombinedArb
    )
    val rewriteChangeArb = rewritableChangesArb.map { it.addContext() }
    val noRewriteChangeArb = rewritableChangesArb.map { it.addContext(rewrites = false) }
    "rewrites should only occur when Oracle is of at least version 19" {
        checkAll(compatibleDatabases, incompatibleDatabases, rewriteChangeArb) { compDb, incompDb, (c, off) ->
            // equality and hashcode implementation are not enforced for changes, hence check on Sql
            val rewritten = c.toSql(compDb)
            val nonRewritten = c.toSql(incompDb)
            val original = off.toSql(incompDb)

            assertThat(
                "statements should be rewritten for Oracle >=19 only",
                rewritten,
                not(hasItem(`in`(nonRewritten)))
            )
            assertThat(
                "all non-rewritten statements are equivalent to the original",
                nonRewritten,
                hasItem(`in`(original))
            )
        }
    }
    "changes will remain identical on non-Oracle databases" {
        checkAll(otherDatabases, rewriteChangeArb) { db, (c, off) ->
            if (off.supports(db)) {
                val nonRewritten = c.toSql(db)
                val original = off.toSql(db)

                assertThat(
                    "all non-rewritten statements are equivalent to the original",
                    nonRewritten,
                    hasItem(`in`(original))
                )
            }
        }
    }
    "rollback rewrites should not occur when Oracle is not version 19" {
        checkAll(incompatibleDatabases, rewriteChangeArb) { db, (c, off) ->
            val hasRollback = off.supportsRollback(db)
            assertEquals(
                hasRollback,
                c.supportsRollback(db),
                "Support of rollback should be equal to original"
            )

            if (hasRollback) {
                val nonRewrittenRollback = c.toRollbackSql(db)
                val originalRollback = off.toRollbackSql(db)
                assertThat(
                    "none of the statements should be rewritten",
                    nonRewrittenRollback,
                    hasItem(`in`(originalRollback))
                )
            }
        }
    }
    "rollbacks for Online rewritten statements should also use Online DDL" {
        checkAll(rewriteChangeArb.filter { it.first.supportsRollback(compatibleOracleSpy) }) { (c, off) ->
            val rewrittenRollback = c.toRollbackSql(compatibleOracleSpy)
            val originalRollback = off.toRollbackSql(incompatibleOracleSpy)
            assertThat(
                "all rewritten statements are different from the original statements",
                rewrittenRollback,
                not(hasItem(`in`(originalRollback)))
            )
        }
    }
    "rollback remain identical on non-Oracle databases" {
        checkAll(otherDatabases, rewriteChangeArb) { db, (c, off) ->
            if (off.supports(db)) {
                val hasRollback = off.supportsRollback(db)
                assertEquals(
                    hasRollback,
                    c.supportsRollback(db),
                    "Support of rollback should be equal to original"
                )

                if (hasRollback) {
                    val nonRewrittenRollback = c.toRollbackSql(db)
                    val originalRollback = off.toRollbackSql(db)
                    assertThat(
                        "none of the statements should be rewritten",
                        nonRewrittenRollback,
                        hasItem(`in`(originalRollback))
                    )
                }
            }
        }
    }
    "checksums for rewritten statements are identical to the original statement" {
        checkAll(rewriteChangeArb) { (c, off) ->
            assertEquals(
                off.generateCheckSum(),
                c.generateCheckSum(),
                "Checksum of rewrite and original should be equal"
            )
        }
    }
    "rewrites should not occur when rewrite property is not set to true" {
        checkAll(compatibleDatabases, noRewriteChangeArb) { db, (c, off) ->
            val nonRewritten = c.toSql(db)
            val original = off.toSql(db)
            assertThat(
                "statements should not be rewritten",
                nonRewritten,
                hasItem(`in`(original))
            )
        }
    }
    "rollback rewrites should not occur when rewrite property is not set to true" {
        checkAll(compatibleDatabases, noRewriteChangeArb) { db, (c, off) ->
            val hasRollback = off.supportsRollback(db)
            assertEquals(
                hasRollback,
                c.supportsRollback(db),
                "Support of rollback should be equal to original"
            )

            if (hasRollback) {
                val nonRewritten = c.toRollbackSql(db)
                val original = off.toRollbackSql(db)
                assertThat(
                    "rollback statements should not be rewritten",
                    nonRewritten,
                    hasItem(`in`(original))
                )
            }
        }
    }
})
