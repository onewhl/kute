package parsers

import ModuleInfo
import ParallelTask
import TestClassInfo
import TestFramework
import TestMethodInfo
import mappers.*
import mu.KotlinLogging
import namedThread
import parsers.TestFileFilter.Companion.findFilesInTestDir
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

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
            "Parsed $cnt/$total ${language.displayName} test files$hint."
        }

        // Use manual operations with ParallelTask instead of .parallelStream() to enable splitting work for each thread
        // by file instead of sequence. Otherwise, if we got test_classes << src_classes, all test classes may be
        // sent to a single thread
        return classesInTestDir.asReversed().map {
            ParallelTask {
                tryParse(it)
                    .also { progressLogger.visit() }
                    ?.let { (framework, classes) -> classes.flatMap { parseTestMethodsFromClass(it, framework) } }
                    ?: emptyList()
            }.fork()
        }.asReversed()
            .flatMap { it.join() }
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
            }?.let { framework ->
                metaFactory.parseClasses(file, content)
                    .takeIf { it.isNotEmpty() }
                    ?.let { Context(framework, it) }
            }
        }
        return null
    }

    private fun parseTestMethodsFromClass(classMeta: ClassMeta, testFramework: TestFramework): List<TestMethodInfo> {
        val sourceClassAndLocation = classMapper.findSourceClass(classMeta)
        val testClassInfo = TestClassInfo(
            classMeta.name,
            classMeta.packageName,
            module.projectInfo,
            module,
            sourceClassAndLocation?.sourceClass,
            language,
            testFramework
        )
        val isClassParameterized = isClassParameterized(classMeta, testFramework)
        val isClassDisabled = isClassDisabled(classMeta, testFramework)
        var sourceMethodCandidates: List<MethodMeta>? = null

        return classMeta.methods
            .mapNotNull { it ->
                it.takeIf { isTestMethod(it, classMeta, testFramework) }?.let { methodMeta ->
                    val sourceMethodInfo = sourceClassAndLocation?.let {
                        if (sourceMethodCandidates == null) {
                            sourceMethodCandidates = getMetaFactoryByLanguage(detectLangByExtension(it.file.extension))
                                .parseMethods(it.file)
                        }
                        SourceMethodMapper.findSourceMethod(methodMeta, it.sourceClass, sourceMethodCandidates!!)
                    }
                    TestMethodInfo(
                        methodMeta.name,
                        methodMeta.body,
                        methodMeta.comment,
                        getDisplayName(methodMeta, testFramework),
                        isTestParameterized(methodMeta, testFramework, isClassParameterized),
                        isDisabled(methodMeta, testFramework, isClassDisabled),
                        testClassInfo,
                        sourceMethodInfo
                    )
                }
            }
            .also { logger.debug { "Parsed ${it.size} ${language.displayName} test methods in test class ${classMeta.name}." } }
    }

    private fun isTestMethod(methodMeta: MethodMeta, classMeta: ClassMeta, testFramework: TestFramework): Boolean =
        when (testFramework) {
            TestFramework.JUNIT3 -> methodMeta.name.startsWith("test")
            TestFramework.JUNIT5 -> methodMeta.hasAnnotation("Test") || methodMeta.hasAnnotation("ParameterizedTest")
            TestFramework.TESTNG -> methodMeta.hasAnnotation("Test") ||
                    (classMeta.hasAnnotation("Test") && methodMeta.isPublic && !methodMeta.hasAnnotation("DataProvider"))
            else -> methodMeta.hasAnnotation("Test")
        }

    private fun getDisplayName(methodMeta: MethodMeta, testFramework: TestFramework): String {
        var displayName = methodMeta.getAnnotationValue("DisplayName")
        if (displayName == null && testFramework == TestFramework.TESTNG) {
            displayName = methodMeta.getAnnotationValue("Test", "description")
        }
        return displayName ?: ""
    }

    private fun isClassParameterized(classMeta: ClassMeta, testFramework: TestFramework): Boolean =
        testFramework == TestFramework.JUNIT4 &&
                classMeta.getAnnotationValue("RunWith")?.let {
                    it.replace("::", ".")
                        .removeSuffix(".class")
                        .substringAfterLast('.') == "Parameterized"
                } ?: false

    private fun isTestParameterized(
        methodMeta: MethodMeta,
        testFramework: TestFramework,
        classParameterized: Boolean
    ): Boolean =
        when (testFramework) {
            TestFramework.JUNIT5 -> methodMeta.hasAnnotation("ParameterizedTest")
            TestFramework.JUNIT4 -> classParameterized ||
                    methodMeta.hasAnnotation("Parameters") ||
                    methodMeta.hasAnnotation("UseDataProvider")

            TestFramework.TESTNG -> methodMeta.hasAnnotation("Parameters") ||
                    methodMeta.getAnnotationValue("Test", "dataProvider") != null

            else -> false
        }

    private fun isClassDisabled(classMeta: ClassMeta, testFramework: TestFramework): Boolean =
        when (testFramework) {
            TestFramework.JUNIT5 -> classMeta.hasAnnotation("Disabled")
            TestFramework.TESTNG -> classMeta.getAnnotationValue("Test", "enabled") == "false"
            TestFramework.JUNIT4, TestFramework.KOTLIN_TEST -> classMeta.hasAnnotation("Ignore")
            else -> false
        }

    private fun isDisabled(methodMeta: MethodMeta, testFramework: TestFramework, classDisabled: Boolean): Boolean =
        when (testFramework) {
            TestFramework.JUNIT5 -> classDisabled || methodMeta.hasAnnotation("Disabled")
            TestFramework.TESTNG -> methodMeta.getAnnotationValue("Test", "enabled") == "false" ||
                    (classDisabled && !methodMeta.hasAnnotation("Test"))

            TestFramework.JUNIT4, TestFramework.KOTLIN_TEST -> classDisabled || methodMeta.hasAnnotation("Ignore")
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