package mappers

import java.io.File

fun interface PackageNameResolver {
    fun extractPackageName(file: File): String
}