package parsers

import ModuleInfo
import ResultWriter
import java.io.File

class ParserRunner(
    private val supportedLangs: Array<Lang>,
    private val path: File,
    private val module: ModuleInfo,
    private val writer: ResultWriter
) : Runnable {
    override fun run() {
        val classFiles = extractFiles(path, supportedLangs.map { it.extension }.toSet())
        val classNameToFile = classFiles.groupBy { it.name.substringBeforeLast('.') }
        supportedLangs.map {
            when (it) {
                Lang.JAVA -> JavaTestParser(path, module, classNameToFile)
                Lang.KOTLIN -> KotlinTestParser(path, module, classNameToFile)
            }
        }.forEach {
            val testMethods = it.process(classFiles)
            writer.writeTestMethods(testMethods)
        }
    }

    private fun extractFiles(module: File, extensions: Set<String>): List<File> = module
        .walkTopDown()
        .filter { extensions.contains(it.extension) }
        .toList()
}