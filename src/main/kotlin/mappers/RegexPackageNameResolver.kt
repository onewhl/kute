package mappers

import java.io.File

object RegexPackageNameResolver : PackageNameResolver {
    private val PACKAGE_REGEX = "(?<=package )\\s*[^;\\s]+".toRegex()

    fun extractPackageName(content: String): String = PACKAGE_REGEX.find(content)?.value?.trim() ?: ""

    override fun extractPackageName(file: File): String = extractPackageName(file.readText())

}