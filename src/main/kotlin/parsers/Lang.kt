package parsers

import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly

enum class Lang(val extension: String) {
    JAVA("java"),
    KOTLIN("kt");

    val displayName = name.lowercase().capitalizeAsciiOnly()
}

fun detectLangByExtension(extension: String): Lang = when(extension) {
    Lang.JAVA.extension -> Lang.JAVA
    Lang.KOTLIN.extension -> Lang.KOTLIN
    else -> throw IllegalArgumentException("Unsupported extension: $extension")
}