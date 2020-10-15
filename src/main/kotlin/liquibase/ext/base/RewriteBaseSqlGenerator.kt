package liquibase.ext.base

import liquibase.database.Database
import liquibase.exception.LiquibaseException
import liquibase.exception.ValidationErrors
import liquibase.ext.rewrites.WrapperStatement
import liquibase.sql.Sql
import liquibase.sqlgenerator.SqlGenerator
import liquibase.sqlgenerator.SqlGeneratorChain
import liquibase.statement.SqlStatement

abstract class RewriteBaseSqlGenerator<T : WrapperStatement<SqlStatement>> : BaseSqlGenerator<T>() {
    protected fun generateOriginal(stmt : T, db : Database): Array<Sql> = generatorFactory.generateSql(stmt.original(), db)

    override fun getPriority(): Int = 2

    // No support if it is false
    override fun supports(stmt: T, db: Database): Boolean = run {
        println("support check for ${stmt.original()}")
        generatorFactory.supports(stmt.original(), db)
    }

    // Delegate validations to original generators
    override fun validate(stmt: T, db: Database, chain: SqlGeneratorChain<T>): ValidationErrors = run {
        val originalErrors = generatorFactory.validate(stmt.original(), db)
        originalErrors.addAll(validateWrapper(stmt, db, chain))
        originalErrors
    }

    protected open fun validateWrapper(stmt : T, db: Database, chain: SqlGeneratorChain<T>) : ValidationErrors = ValidationErrors()
}