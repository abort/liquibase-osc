package liquibase.ext.rewrites

import liquibase.statement.AbstractSqlStatement
import liquibase.statement.SqlStatement

// Unfortunately by default we can not have generics here because the ServiceLocator doesn't like those
// We can also not have an intermediate layer between AbstractSqlStatement and the wrappers
interface WrapperStatement {
    val original: SqlStatement
}

data class CreateIndexOnlineWrapperStatement(override val original: SqlStatement) :
    AbstractSqlStatement(), WrapperStatement
data class DropIndexOnlineWrapperStatement(override val original: SqlStatement) :
    AbstractSqlStatement(), WrapperStatement
data class DropPrimaryKeyOnlineWrapperStatement(override val original: SqlStatement) :
    AbstractSqlStatement(), WrapperStatement
data class DropForeignKeyOnlineWrapperStatement(override val original: SqlStatement) :
    AbstractSqlStatement(), WrapperStatement
data class DropUniqueConstraintOnlineWrapperStatement(override val original: SqlStatement) :
    AbstractSqlStatement(), WrapperStatement
