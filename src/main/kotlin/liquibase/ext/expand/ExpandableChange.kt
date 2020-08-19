package liquibase.ext.expand

import liquibase.change.AbstractChange
import liquibase.change.Change
import liquibase.change.core.*
import liquibase.database.Database
import liquibase.exception.ValidationErrors

abstract class ExpandableChange : AbstractChange() {
    final override fun validate(db: Database): ValidationErrors {
        val errors = super.validate(db)
        val localChanges = changeSet.changes
        localChanges.filter { it.isContraction() }.forEach {
            errors.addError("${it.javaClass.canonicalName} is a contracting change, which should not be combined with expanding ones")
        }

        localChanges.filter { it.isPossibleContractingModify() }.forEach {
            errors.addWarning("${it.javaClass.canonicalName} might be a contracting change depending on the current database state")
        }

        errors.addAll(validateAdditionalConstraints(db))
        return errors
    }

    private fun Change.isContraction() : Boolean =
            this !is ExpandableChange && javaClass in list

    private fun Change.isPossibleContractingModify() : Boolean = when (this) {
                is ModifyDataTypeChange -> true // possible contraction.... add more processing later?
                else -> false
            }

    // TODO: probably not necessary as we can move this to the Generator?
    protected open fun validateAdditionalConstraints(db: Database) : ValidationErrors = ValidationErrors()

    private val list : List<Class<out Change>> = listOf(
            DropTableChange::class.java,
            DropColumnChange::class.java,
            RenameTableChange::class.java,
            RenameColumnChange::class.java,
            AddNotNullConstraintChange::class.java
    )
}