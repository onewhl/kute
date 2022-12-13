package parsers

import ModuleInfo
import SourceMethodInfo
import TestClassInfo
import TestMethodInfo
import mappers.*
import mu.KLogger
import java.io.File
import java.util.stream.Collectors

abstract class AbstractParser<SrcFile, Cls, Func>(
    private val path: File,
    protected val module: ModuleInfo,
    classNameToFile: Map<String, List<File>>,
    private val logger: KLogger
) : Parser {
    private val classMapper = ClassMapper(module, classNameToFile)

    abstract val metaFactory: MetaFactory<Cls, Func>

    override fun process(files: List<File>): List<TestMethodInfo> {
        logger.info { "Start processing files in module: $path." }

        files.count { it.extension == language.extension }
            .let { logger.info { "Found: $it $language files." } }

        val classesInTestDir = TestFileFilter.findFilesInTestDir(language, module.projectInfo.buildSystem, files)
        val hint = if (module.projectInfo.buildSystem.supportsTestDirFiltering) " in test dir" else ""
        logger.info { "Found: ${classesInTestDir.size} $language classes$hint." }

        return parseTestFiles(classesInTestDir)
            .flatMap { file -> mapFileToClass(file)?.let { parseTestMethodsFromClass(it) } ?: emptyList() }
            .also { logger.info { "Finished processing $language files in module: $path. Found ${it.size} test methods." } }
    }

    protected fun isTestMethodMarker(annotation: String?) = testMethodMarkers.contains(annotation)

    private fun fastFilterTest(content: String) =
        (content.contains("junit") || content.contains("testng") || content.contains("kotlin.test"))
                && content.contains("Test")

    private fun parseTestFiles(filePaths: List<File>): List<SrcFile> =
        filePaths.parallelStream()
            .map { file ->
                file.readText()
                    .takeIf { fastFilterTest(it) }
                    ?.let { doParseSource(file, it) }
                    ?.takeIf { src -> isTestFile(src) }
            }.filter { it != null }
            .collect(Collectors.toList<SrcFile>())
            .also { logger.debug { "Parsed ${it.size} $language test files in /test/ dir." } }

    private fun parseTestMethodsFromClass(testClass: Cls): List<TestMethodInfo> {
        val classMeta = metaFactory.createClassMeta(testClass)
        val sourceClass = classMapper.findSourceClass(classMeta)
        val testClassInfo = TestClassInfo(
            classMeta.name,
            classMeta.packageName,
            module.projectInfo,
            module,
            sourceClass,
            language
        )

        return findTestMethods(testClass)
            .map { method ->
                val sourceMethodInfo = sourceClass?.let {
                    DelegatingMethodMapper.findSourceMethod(metaFactory.createMethodMeta(method), it, sourceClass.file)
                }
                createTestMethodInfo(method, testClassInfo, sourceMethodInfo)
            }
            .also { logger.debug { "Parsed ${it.size} $language test methods in test class ${classMeta.name}." } }
    }

    protected abstract fun findTestMethods(testClass: Cls): List<Func>

    protected abstract fun doParseSource(fileSource: File, content: String): SrcFile?

    protected abstract fun isTestFile(file: SrcFile): Boolean

    protected abstract fun mapFileToClass(file: SrcFile): Cls?
    protected abstract fun createTestMethodInfo(
        method: Func,
        classInfo: TestClassInfo,
        source: SourceMethodInfo?
    ): TestMethodInfo

    companion object {
        private val testMethodMarkers = arrayOf("Test", "ParameterizedTest")
    }
}