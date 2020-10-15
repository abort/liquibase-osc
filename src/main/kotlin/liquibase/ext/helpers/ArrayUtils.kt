package liquibase.ext.helpers

import liquibase.database.Database
import kotlin.reflect.KClass

object ArrayUtils {
    inline fun <reified T, X : T> Array<T>.mapFirstIf(condition: Boolean, f: (element: T) -> X): Array<T> = if (condition) {
        mapFirst(f)
    } else {
        this
    }

    inline fun <reified T , X : T> Array<T>.mapFirst(f: (element: T) -> X): Array<T> = mapIndexed { i, element ->
        if (i == 0) {
            f(element)
        } else {
            element
        }
    }.toTypedArray()

    inline fun <reified T, X : T> Array<T>.mapFirst(db : Database, f : (Database, T) -> X): Array<T> = mapIndexed { i, element ->
        if (i == 0) {
            f(db, element)
        }
        else {
            element
        }
    }.toTypedArray()
}