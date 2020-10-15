package liquibase.ext.rewrites

import liquibase.statement.AbstractSqlStatement
import liquibase.statement.SqlStatement
import liquibase.statement.core.*

sealed class WrapperStatement<out T : SqlStatement> : AbstractSqlStatement() {
    protected abstract val original : T

    fun original() : T = original


    data class CreateIndexOnlineWrapperStatement(override val original : CreateIndexStatement) : WrapperStatement<CreateIndexStatement>()
    data class DropColumnOnlineWrapperStatement(override val original : DropColumnStatement) : WrapperStatement<DropColumnStatement>()
    data class DropIndexOnlineWrapperStatement(override val original : DropIndexStatement) : WrapperStatement<DropIndexStatement>()
    data class DropPrimaryKeyOnlineWrapperStatement(override val original: DropPrimaryKeyStatement) : WrapperStatement<DropPrimaryKeyStatement>()
    data class DropForeignKeyOnlineWrapperStatement(override val original: DropForeignKeyConstraintStatement) : WrapperStatement<DropForeignKeyConstraintStatement>()
    data class DropUniqueConstraintOnlineWrapperStatement(override val original : DropUniqueConstraintStatement) : WrapperStatement<DropUniqueConstraintStatement>()
}