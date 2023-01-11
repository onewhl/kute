package parsers

import ModuleInfo
import TestMethodInfo
import java.io.File

interface Parser {
    fun process(files: List<File>): List<TestMethodInfo>
}

fun createParser(lang: Lang, path: File, module: ModuleInfo, classNameToFile: Map<String, List<Pair<File, ModuleInfo>>>) =
    JvmTestsParser(path, module, classNameToFile, lang)