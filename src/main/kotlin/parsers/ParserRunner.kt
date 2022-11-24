package parsers

import ModuleInfo
import TestMethodInfo
import java.io.File
import java.util.concurrent.Callable

class ParserRunner(
    private val supportedLangs: Array<Lang>,
    private val path: File,
    private val module: ModuleInfo
) : Callable<List<TestMethodInfo>> {
    override fun call(): List<TestMethodInfo> {
        val classFiles = extractFiles(path, supportedLangs.map { it.extension }.toSet())
        val classNameToFile = classFiles.groupBy { it.name.substringBeforeLast('.') }
        return supportedLangs.map {
            when (it) {
                Lang.JAVA -> JavaTestParser(path, module, classNameToFile)
                Lang.KOTLIN -> KotlinTestParser(path, module, classNameToFile)
            }
        }.flatMap {
            it.process(classFiles)
        }
    }

    private fun extractFiles(module: File, extensions: Set<String>): List<File> = module
        .walkTopDown()
        .filter { extensions.contains(it.extension) }
        .toList()
}