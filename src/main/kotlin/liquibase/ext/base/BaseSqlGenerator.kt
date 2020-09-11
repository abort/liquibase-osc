package liquibase.ext.base

import liquibase.database.Database
import liquibase.sql.Sql
import liquibase.sqlgenerator.SqlGeneratorChain
import liquibase.sqlgenerator.core.AbstractSqlGenerator
import liquibase.statement.SqlStatement

abstract class BaseSqlGenerator<T : SqlStatement> : AbstractSqlGenerator<T>() {
    final override fun generateSql(stmt: T, db: Database, generatorChain: SqlGeneratorChain<T>): Array<Sql> = run {
        val e = validate(stmt, db, generatorChain)
        if (e.hasErrors()) {
            throw IllegalStateException("Can not generate due to errors: ${e.errorMessages.joinToString(separator = ", ")}")
        } else {
            generate(stmt, db, generatorChain)
        }
    }

    abstract fun generate(stmt: T, db: Database, generatorChain: SqlGeneratorChain<T>): Array<Sql>
}