package liquibase.ext.helpers

import liquibase.changelog.ChangeSet
import liquibase.changelog.DatabaseChangeLog

object PropertyUtils {
    fun getProperty(change : ChangeSet, property : String, default : String) : String =
            getProperty(change.changeLog, property, default)

    // Find property starting closeby and going to the outerlogs
    tailrec fun getProperty(log : DatabaseChangeLog, property: String, default: String) : String {
        val parameters = log.changeLogParameters
        return if (parameters.hasValue(property, log)) {
            parameters.getValue(property, log) as String
        }
        else {
            if (log.parentChangeLog == null || log.parentChangeLog == log) {
                default
            } else {
                getProperty(log.parentChangeLog, property, default)
            }
        }
    }
}