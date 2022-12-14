package parsers

import ModuleInfo
import TestClassInfo
import TestFramework
import TestMethodInfo
import mappers.ClassMapper
import mappers.DelegatingMethodMapper
import mappers.MetaFactory
import mappers.MethodMeta
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
            .flatMap { context ->
                mapFileToClass(context.srcFile)?.let { parseTestMethodsFromClass(it, context.testFramework) }
                    ?: emptyList()
            }
            .also { logger.info { "Finished processing $language files in module: $path. Found ${it.size} test methods." } }
    }

    protected fun isTestMethodMarker(annotation: String?) = testMethodMarkers.contains(annotation)

    private fun fastFilterTest(content: String) =
        (content.contains("junit") || content.contains("testng") || content.contains("kotlin.test"))
                && content.contains("Test")

    private fun parseTestFiles(filePaths: List<File>): List<Context<SrcFile>> =
        filePaths.parallelStream()
            .map { file -> tryParse(file) }
            .filter { it != null }
            .collect(Collectors.toList<Context<SrcFile>>())
            .also { logger.debug { "Parsed ${it.size} $language test files in /test/ dir." } }

    private fun matches(content: String, index: Int, target: String) =
        index > 0 && content.regionMatches(index + 5, target, 0, target.length)

    private fun tryParse(file: File): Context<SrcFile>? {
        val content = file.readText()
        if (content.contains("Test")) {
            val indexOfJunit = content.indexOf("junit")
            return when {
                matches(content, indexOfJunit, ".jupiter") -> TestFramework.JUNIT5
                matches(content, indexOfJunit, ".framework") -> TestFramework.JUNIT3
                indexOfJunit > 0 -> TestFramework.JUNIT4
                content.contains("testng") -> TestFramework.TESTNG
                content.contains("kotlin.test") -> TestFramework.KOTLIN_TEST
                else -> null
            }?.let { framework -> doParseSource(file, content)?.let { Context(framework, it) } }
        }
        return null
    }

    private fun parseTestMethodsFromClass(testClass: Cls, testFramework: TestFramework): List<TestMethodInfo> {
        val classMeta = metaFactory.createClassMeta(testClass)
        val sourceClass = classMapper.findSourceClass(classMeta)
        val testClassInfo = TestClassInfo(
            classMeta.name,
            classMeta.packageName,
            module.projectInfo,
            module,
            sourceClass,
            language,
            testFramework
        )

        return findMethods(testClass)
            .mapNotNull { method ->
                metaFactory.createMethodMeta(method)
                    .takeIf { isTestMethod(it, testFramework) } ?.let { methodMeta ->
                        val sourceMethodInfo = sourceClass?.let {
                            DelegatingMethodMapper.findSourceMethod(methodMeta, it, sourceClass.file)
                        }
                        TestMethodInfo(
                            methodMeta.name,
                            methodMeta.body,
                            methodMeta.comment,
                            getDisplayName(methodMeta, testFramework),
                            isParameterized(methodMeta, testFramework),
                            isDisabled(methodMeta, testFramework),
                            testClassInfo,
                            sourceMethodInfo
                        )
                    }
            }
            .also { logger.debug { "Parsed ${it.size} $language test methods in test class ${classMeta.name}." } }
    }

    private fun isTestMethod(methodMeta: MethodMeta, testFramework: TestFramework): Boolean = when (testFramework) {
        TestFramework.JUNIT3 -> methodMeta.name.startsWith("test")
        TestFramework.JUNIT5 -> methodMeta.hasAnnotation("Test") || methodMeta.hasAnnotation("ParameterizedTest")
        else -> methodMeta.hasAnnotation("Test")
    }

    protected abstract fun findMethods(testClass: Cls): List<Func>

    protected abstract fun doParseSource(fileSource: File, content: String): SrcFile?

    protected abstract fun mapFileToClass(file: SrcFile): Cls?

    private fun getDisplayName(methodMeta: MethodMeta, testFramework: TestFramework): String {
        var displayName = methodMeta.getAnnotationValue("DisplayName")
        if (displayName == null && testFramework == TestFramework.TESTNG) {
            displayName = methodMeta.getAnnotationValue("Test", "description")
        }
        return displayName ?: ""
    }

    private fun isParameterized(methodMeta: MethodMeta, testFramework: TestFramework): Boolean = when(testFramework) {
        TestFramework.JUNIT5 -> methodMeta.hasAnnotation("ParameterizedTest")
        else -> false
    }

    private fun isDisabled(methodMeta: MethodMeta, testFramework: TestFramework): Boolean = when(testFramework) {
        TestFramework.JUNIT5 -> methodMeta.hasAnnotation("Disabled")
        TestFramework.TESTNG -> methodMeta.getAnnotationValue("Test", "enabled") == "false"
        TestFramework.JUNIT4, TestFramework.KOTLIN_TEST -> methodMeta.hasAnnotation("Ignore")
        else -> false
    }

    private data class Context<SrcFile>(val testFramework: TestFramework, val srcFile: SrcFile)

    companion object {
        private val testMethodMarkers = arrayOf("Test", "ParameterizedTest")
    }
}