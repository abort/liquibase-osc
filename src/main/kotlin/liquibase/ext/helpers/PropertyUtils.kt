package liquibase.ext.helpers

import liquibase.changelog.ChangeSet

object PropertyUtils {
    fun getProperty(change : ChangeSet, property : String, default : String) : String = run {
        val log = change.changeLog
        val parameters = log.changeLogParameters
        if (parameters.hasValue(property, log)) {
            parameters.getValue(property, log) as String
        }
        else {
            default
        }
    }
}