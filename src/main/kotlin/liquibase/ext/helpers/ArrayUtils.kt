package liquibase.ext.helpers

import liquibase.database.Database

object ArrayUtils {
    inline fun <reified T, X : T> Array<T>.mapIf(condition: Boolean, f: (element: T) -> X): Array<T> = if (condition) {
        map(f).toTypedArray()
    } else {
        this
    }

    inline fun <reified T, X : T> Array<T>.mapFirst(
        db: Database,
        f: (Database, T) -> X
    ): Array<T> = mapIndexed { i, element ->
        if (i == 0) {
            f(db, element)
        } else {
            element
        }
    }.toTypedArray()
}
