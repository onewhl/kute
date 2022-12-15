package parsers

import ModuleInfo
import TestClassInfo
import TestFramework
import TestMethodInfo
import mappers.*
import mu.KotlinLogging
import namedThread
import parsers.TestFileFilter.Companion.findFilesInTestDir
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors

private val logger = KotlinLogging.logger {}

class JvmTestsParser(
    private val path: File,
    private val module: ModuleInfo,
    classNameToFile: Map<String, List<File>>,
    private val language: Lang
) : Parser {
    private val classMapper = ClassMapper(module, classNameToFile)
    private val metaFactory = getMetaFactoryByLanguage(language)

    override fun process(files: List<File>): List<TestMethodInfo> {
        logger.info { "Start processing ${language.displayName} files in module: $path." }

        files.count { it.extension == language.extension }
            .also { logger.info { "Found: $it ${language.displayName} files." } }

        val classesInTestDir = findFilesInTestDir(language, module.projectInfo.buildSystem, files)
        val hint = if (module.projectInfo.buildSystem.supportsTestDirFiltering) " in test dir" else ""
        logger.info { "Found: ${classesInTestDir.size} ${language.displayName} classes$hint." }

        val progressLogger = ProgressLogger(classesInTestDir.size) { cnt, total ->
            "Parsed $cnt/$total ${language.displayName} test files in /test/ dir."
        }
        return classesInTestDir.parallelStream()
            .map { tryParse(it).also { progressLogger.visit() } }
            .filter { it != null }
            .flatMap { ctx -> ctx!!.classes.flatMap { parseTestMethodsFromClass(it, ctx.testFramework) }.stream() }
            .collect(Collectors.toList())
            .also { logger.info { "Finished processing ${language.displayName} files in module: $path. Found ${it.size} test methods." } }
    }

    private fun matches(content: String, index: Int, target: String) =
        index > 0 && content.regionMatches(index + 5, target, 0, target.length)

    private fun tryParse(file: File): Context? {
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
            }?.let { framework -> metaFactory.parse(file, content)
                .takeIf { it.isNotEmpty() }
                ?.let { Context(framework, it) } }
        }
        return null
    }

    private fun parseTestMethodsFromClass(classMeta: ClassMeta, testFramework: TestFramework): List<TestMethodInfo> {
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

        return classMeta.methods
            .mapNotNull { it ->
                it.takeIf { isTestMethod(it, testFramework) } ?.let { methodMeta ->
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
            .also { logger.debug { "Parsed ${it.size} ${language.displayName} test methods in test class ${classMeta.name}." } }
    }

    private fun isTestMethod(methodMeta: MethodMeta, testFramework: TestFramework): Boolean = when (testFramework) {
        TestFramework.JUNIT3 -> methodMeta.name.startsWith("test")
        TestFramework.JUNIT5 -> methodMeta.hasAnnotation("Test") || methodMeta.hasAnnotation("ParameterizedTest")
        else -> methodMeta.hasAnnotation("Test")
    }

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

    private data class Context(val testFramework: TestFramework, val classes: List<ClassMeta>)

    private class ProgressLogger(private val total: Int, private val msgSupplier: (Int, Int) -> String) {
        private val counter = AtomicInteger()
        private val period = 100
        private val originalThreadName = Thread.currentThread().name

        fun visit() {
            val updatedCount = counter.incrementAndGet()
            if (updatedCount % period == 0 || updatedCount == total) {
                namedThread(originalThreadName) { logger.info { msgSupplier(updatedCount, total) } }
            }
        }
    }
}