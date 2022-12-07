package parsers

enum class Lang(val extension: String) {
    JAVA("java"),
    KOTLIN("kt");
}

fun detectLangByExtension(extension: String): Lang = when(extension) {
    Lang.JAVA.extension -> Lang.JAVA
    Lang.KOTLIN.extension -> Lang.KOTLIN
    else -> throw IllegalArgumentException("Unsupported extension: $extension")
}