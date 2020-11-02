package liquibase.ext.util

object TestUtils {
    val DropConstraintOnlineRegex =
        """alter\s+table\s+\w+(\.\w+)*\s+drop\s+constraint\s+\w+\s+((drop|keep)\s+index\s+)?online"""
            .toRegex(RegexOption.IGNORE_CASE).toPattern()
}
