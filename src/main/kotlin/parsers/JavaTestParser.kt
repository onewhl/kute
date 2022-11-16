package parsers

import ModuleInfo
import SourceClassInfo
import TestClassInfo
import TestMethodInfo
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.MethodDeclaration
import mappers.JavaMethodMeta
import mappers.DelegatingMethodMapper
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Extracts tests files, finds test method in them, and collects metadata.
 */
class JavaTestParser(
    private val path: File,
    private val module: ModuleInfo,
    private val classNameToFile: Map<String, List<File>>
) : Parser {
    override val language = Lang.JAVA
    override fun process(files: List<File>): List<TestMethodInfo> {
        logger.info { "Start processing files in module: $path." }
        files.count { it.extension == language.extension }
            .let { logger.info { "Found: $it Java classes." } }

        val filesInTestDir = findFilesInTestDir(language, module.projectInfo.buildSystem, path, files.stream())
            .also { logger.info { "Found: ${it.size} Java test classes." } }

        val parsedFilesInTestDir: List<CompilationUnit> = parseFiles(filesInTestDir)

        val testFiles: List<CompilationUnit> = getTestFiles(parsedFilesInTestDir)

        return testFiles
            .flatMap { parseTestMethodsFromClass(it) }
            .toList()
            .also { logger.info { "Finished processing files in module: $path. Found ${it.size} test methods." } }
    }

    private fun parseTestMethodsFromClass(testClass: CompilationUnit): List<TestMethodInfo> {
        val sourceClass = findSourceClass(testClass)
        val testClassInfo = TestClassInfo(testClass.primaryTypeName.orElse(""), module.projectInfo, module, sourceClass)
        val methodDeclarations = testClass.findAll(MethodDeclaration::class.java)

        return methodDeclarations
            .filter { it.annotations.any { it.nameAsString == "Test" || it.nameAsString == "ParameterizedTest" } }
            //TODO: make it configurable
            .filter { it.annotations.none { it.nameAsString == "Disabled" || it.nameAsString == "Ignored" } }
            .map { m ->
                val sourceMethodInfo =
                    sourceClass?.let {
                        DelegatingMethodMapper.findSourceMethod(JavaMethodMeta(m), it, classNameToFile)
                    }

                TestMethodInfo(
                    m.nameAsString,
                    getBody(m),
                    getComment(m),
                    getDisplayName(m),
                    isParameterized(m),
                    testClassInfo,
                    sourceMethodInfo
                )
            }
            .also { logger.debug { "Parsed test methods in test class ${testClass.primaryTypeName}." } }
    }

    private fun isParameterized(method: MethodDeclaration): Boolean =
        method.annotations.any { it.nameAsString == "ParametrisedTest" }

    private fun findSourceClass(testClass: CompilationUnit) = testClass.primaryTypeName.orElse(null)
        .let { if (it.startsWith("Test", ignoreCase = true)) it.substring("Test".length) else it }
        .let { if (it.endsWith("Test", ignoreCase = true)) it.substring(0, it.length - "Test".length) else it }
        .let { if (it.endsWith("ITCase", ignoreCase = true)) it.substring(0, it.length - "ITCase".length) else it }
        .let { if (it.endsWith("Case", ignoreCase = true)) it.substring(0, it.length - "Case".length) else it }
        .takeIf { classNameToFile.containsKey(it) }
        ?.let { SourceClassInfo(it, module) }
        .also {
            if (it != null && classNameToFile[it.name]!!.size > 1) {
                logger.warn { "Multiple classes found with name $it.name: ${classNameToFile[it.name]}." }
            }
        }

    private fun getTestFiles(projectFiles: List<CompilationUnit>): List<CompilationUnit> =
        projectFiles
            .filter { isTestFile(it) }
            .also { logger.info { "Found ${it.size} test files." } }

    private fun parseFiles(projectFiles: Collection<File>) = projectFiles
        .mapNotNull { StaticJavaFileParser.parse(it) }
        .also { logger.debug { "Parsed files in /test/ dir." } }

    /**
     * Checks if a file is a test file or not by searching for JUnit or TestNG imports.
     */
    private fun isTestFile(file: CompilationUnit): Boolean {
        return file.imports.any {
            it.nameAsString.startsWith("org.junit") ||
                    it.nameAsString.startsWith("junit.framework") ||
                    it.nameAsString.startsWith("org.testng")
        }
    }

    private fun getComment(method: MethodDeclaration) = if (method.javadocComment.isPresent) {
        method.javadocComment.get().content
    } else {
        ""
    }

    private fun getBody(m: MethodDeclaration): String = if (m.body.isPresent) {
        m.body.get().toString()
    } else {
        ""
    }

    private fun getDisplayName(method: MethodDeclaration): String =
        (method.annotations.find { a -> a.nameAsString == "DisplayName" }?.nameAsString) ?: ""
}