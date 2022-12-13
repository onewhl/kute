package parsers

import ModuleInfo
import TestMethodInfo
import java.io.File

interface Parser {
    fun process(files: List<File>): List<TestMethodInfo>
    val language: Lang
}

fun createParser(lang: Lang, path: File, module: ModuleInfo, classNameToFile: Map<String, List<File>>) = when (lang) {
    Lang.JAVA -> JavaTestParser(path, module, classNameToFile)
    Lang.KOTLIN -> KotlinTestParser(path, module, classNameToFile)
}