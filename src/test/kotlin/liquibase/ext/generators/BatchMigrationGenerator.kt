package liquibase.ext.generators

import io.kotest.property.Arb
import io.kotest.property.Exhaustive
import io.kotest.property.RandomSource
import io.kotest.property.arbitrary.*
import io.kotest.property.exhaustive.azstring
import liquibase.ext.changes.BatchMigrationChange
import java.util.*

object BatchMigrationGenerator {
    val identifierGen = { min : Int -> Exhaustive.azstring(min..16).toArb() }

    val validMigrationGenerator = arbitrary { rs: RandomSource ->
        val change = BatchMigrationChange()

        val colCount = Arb.int(1, 5).next(rs)
        val colGen = fixedColumnListNoDupsGenerator(colCount, colCount)

        change.table = identifierGen(1).next(rs)
        change.chunkSize = Arb.int(1, 10000).next(rs)
        val from = colGen.next(rs)
        val fromSet = from.toSet()
        change.fromColumns = from.toColumnList()

        // Make sure we do not have overlapping or crossing columns between from and to
        change.toColumns = colGen.filterNot { l -> fromSet.any { l.toSet().contains(it) } }.next(rs).toColumnList()
        change.primaryKeyColumns = fixedColumnListNoDupsGenerator(1, 5).next(rs).toColumnList()
        change
    }

    val sampleMigrationGenerator = arbitrary { rs: RandomSource ->
        val change = BatchMigrationChange()
        change.table = identifierGen(1).orNull().next(rs)
        change.chunkSize = Arb.int(-100, 10000).orNull().next(rs)
        val upperBound = Arb.int(0, 5).next(rs)
        val minBound = Arb.int(0, 5).filter { it <= upperBound }.next(rs)
        change.fromColumns = fixedColumnStringSequenceGenerator(minBound, upperBound).orNull().next(rs)
        change.toColumns = fixedColumnStringSequenceGenerator(minBound, upperBound).orNull().next(rs)
        change.primaryKeyColumns = fixedColumnStringSequenceGenerator(minBound, upperBound).orNull().next(rs)
        change
    }

    val invalidMigrationGenerator = sampleMigrationGenerator.filter { c: BatchMigrationChange ->
        val simplePredicate = c.primaryKeyColumns.isNullOrEmpty() || c.fromColumns.isNullOrEmpty()
                || c.toColumns.isNullOrEmpty() || (c.chunkSize ?: -1) <= 0
        if (simplePredicate) return@filter true
        else {
            val from = c.fromColumns!!.split(",")
            val to = c.toColumns!!.split(",").toSet()
            // check whether from and to columns are equal somewhere or crossing
            from.size != to.size || from.any { it in to }
        }
    }

    private fun List<String>.toColumnList() : String = joinToString(separator = ",") { it }

    private val fixedColumnListGenerator = { lowerBound: Int, inclusiveUpperBound: Int ->
        Arb.list(identifierGen(1), IntRange(lowerBound, inclusiveUpperBound))
    }

    private val fixedColumnListNoDupsGenerator = { lowerBound: Int, inclusiveUpperBound: Int ->
        fixedColumnListGenerator(lowerBound, inclusiveUpperBound).filterNot { l ->
            l.toSet().size != l.size
        }
    }
    private val fixedColumnStringSequenceGenerator = { lowerBound: Int, inclusiveUpperBound: Int ->
        fixedColumnListGenerator(lowerBound, inclusiveUpperBound).map { l -> l.joinToString(",") { it } }
    }
}
