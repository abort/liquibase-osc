package liquibase.ext.base

import liquibase.database.Database
import liquibase.exception.ValidationErrors
import liquibase.ext.rewrites.WrapperStatement
import liquibase.sql.Sql
import liquibase.sqlgenerator.SqlGeneratorChain
import liquibase.statement.SqlStatement

abstract class RewriteBaseSqlGenerator<T> : BaseSqlGenerator<T>() where T : SqlStatement, T : WrapperStatement {
    protected fun generateOriginal(stmt: T, db: Database): Array<Sql> =
        getGeneratorFactory().generateSql(stmt.original, db)
    // No support if the original LB has no support for it
    override fun supports(stmt: T, db: Database): Boolean = getGeneratorFactory().supports(stmt.original, db)

    // Delegate validations to original generators
    override fun validate(stmt: T, db: Database, chain: SqlGeneratorChain<T>): ValidationErrors = run {
        val originalErrors = getGeneratorFactory().validate(stmt.original, db)
        originalErrors.addAll(validateWrapper(stmt, db, chain))
        originalErrors
    }

    protected open fun validateWrapper(stmt: T, db: Database, chain: SqlGeneratorChain<T>): ValidationErrors =
        ValidationErrors()
}
