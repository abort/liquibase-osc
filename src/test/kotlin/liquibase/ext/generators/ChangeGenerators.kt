package liquibase.ext.generators

import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.bool
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.orNull
import io.kotest.property.arbitrary.stringPattern
import io.kotest.property.arbitrary.take
import io.kotest.property.exhaustive.exhaustive
import io.mockk.every
import io.mockk.spyk
import liquibase.change.AddColumnConfig
import liquibase.change.core.CreateIndexChange
import liquibase.change.core.DropColumnChange
import liquibase.change.core.DropForeignKeyConstraintChange
import liquibase.change.core.DropIndexChange
import liquibase.change.core.DropPrimaryKeyChange
import liquibase.change.core.DropUniqueConstraintChange
import liquibase.database.core.DerbyDatabase
import liquibase.database.core.FirebirdDatabase
import liquibase.database.core.H2Database
import liquibase.database.core.HsqlDatabase
import liquibase.database.core.MSSQLDatabase
import liquibase.database.core.MySQLDatabase
import liquibase.database.core.OracleDatabase
import liquibase.database.core.PostgresDatabase
import liquibase.database.core.SQLiteDatabase
import liquibase.database.core.SybaseDatabase
import liquibase.ext.rewrites.CreateIndexOnline
import liquibase.ext.rewrites.DropColumnOnline
import liquibase.ext.rewrites.DropForeignKeyConstraintOnline
import liquibase.ext.rewrites.DropIndexOnline
import liquibase.ext.rewrites.DropPrimaryKeyConstraintOnline
import liquibase.ext.rewrites.DropUniqueConstraintOnline

object ChangeGenerators {
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
    val createIndexCombinedArb = Arb.bind(alphaNumArb.orNull(), alphaNumArb, alphaNumArb, columnsArb) {
        schema, table, index, cols ->

        Pair(
            CreateIndexOnline().apply {
                schemaName = schema
                tableName = table
                indexName = index
                columns = cols
            },
            CreateIndexChange().apply {
                schemaName = schema
                tableName = table
                indexName = index
                columns = cols
            }
        )
    }
    val dropColumnCombinedArb = Arb.bind(alphaNumArb.orNull(), alphaNumArb, columnsArb) {
        schema, table, cols ->

        Pair(
            DropColumnOnline().apply {
                schemaName = schema
                tableName = table
                columns = cols
            },
            DropColumnChange().apply {
                schemaName = schema
                tableName = table
                columns = cols
            }
        )
    }
    val dropForeignKeyCombinedArb = Arb.bind(alphaNumArb.orNull(), alphaNumArb, alphaNumArb) {
        schema, table, constraint ->

        Pair(
            DropForeignKeyConstraintOnline().apply {
                baseTableSchemaName = schema
                baseTableName = table
                constraintName = constraint
            },
            DropForeignKeyConstraintChange().apply {
                baseTableSchemaName = schema
                baseTableName = table
                constraintName = constraint
            }
        )
    }
    val dropIndexCombinedArb = Arb.bind(alphaNumArb.orNull(), alphaNumArb, alphaNumArb) {
        schema, table, index ->

        Pair(
            DropIndexOnline().apply {
                schemaName = schema
                tableName = table
                indexName = index
            },
            DropIndexChange().apply {
                schemaName = schema
                tableName = table
                indexName = index
            }
        )
    }
    val dropPrimaryKeyCombinedArb = Arb.bind(alphaNumArb.orNull(), alphaNumArb, alphaNumArb, Arb.bool()) {
        schema, table, constraint, dropIdx ->

        Pair(
            DropPrimaryKeyConstraintOnline().apply {
                schemaName = schema
                tableName = table
                constraintName = constraint
                dropIndex = dropIdx
            },
            DropPrimaryKeyChange().apply {
                schemaName = schema
                tableName = table
                constraintName = constraint
                dropIndex = dropIdx
            }
        )
    }
    val dropUniqueConstraintCombinedArb = Arb.bind(alphaNumArb.orNull(), alphaNumArb, alphaNumArb) {
        schema, table, constraint ->

        Pair(
            DropUniqueConstraintOnline().apply {
                schemaName = schema
                tableName = table
                constraintName = constraint
            },
            DropUniqueConstraintChange().apply {
                schemaName = schema
                tableName = table
                constraintName = constraint
            }
        )
    }

    val otherDatabases = listOf(
        MySQLDatabase(), PostgresDatabase(), MSSQLDatabase(), H2Database(), HsqlDatabase(), SybaseDatabase(),
        FirebirdDatabase(), DerbyDatabase(), SQLiteDatabase()
    ).exhaustive()

    val compatibleDatabases = Arb.int(19, 30).map { createOracleSpy(it) }
    val incompatibleDatabases = Arb.int(1, 18).map { createOracleSpy(it) }
    val incompatibleOracleSpy = createOracleSpy(12, 'c')
    val compatibleOracleSpy = createOracleSpy(19, 'c')

    private fun createOracleSpy(ver: Int, suffix: Char? = null) = spyk<OracleDatabase>().apply {
        every { databaseMajorVersion } returns ver
        every { databaseProductVersion } returns "Oracle Database ${ver}$suffix Enterprise Edition Release"
    }
}
