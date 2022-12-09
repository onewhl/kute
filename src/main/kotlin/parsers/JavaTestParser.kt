package parsers

import ModuleInfo
import TestClassInfo
import TestMethodInfo
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.MethodDeclaration
import mappers.ClassMapper
import mappers.JavaMethodMeta
import mappers.DelegatingMethodMapper
import mappers.JavaClassMeta
import mu.KotlinLogging
import parsers.TestFileFilter.Companion.findFilesInTestDir
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Extracts tests files, finds test method in them, and collects metadata.
 */
class JavaTestParser(
    private val path: File,
    private val module: ModuleInfo,
    classNameToFile: Map<String, List<File>>
) : Parser {
    override val language = Lang.JAVA
    private val classMapper = ClassMapper(module, classNameToFile)

    override fun process(files: List<File>): List<TestMethodInfo> {
        logger.info { "Start processing files in module: $path." }
        files.count { it.extension == language.extension }
            .let { logger.info { "Found: $it Java classes." } }

        val filesInTestDir = findFilesInTestDir(language, module.projectInfo.buildSystem, files)
            .also { logger.info { "Found: ${it.size} Java test classes." } }

        val parsedFilesInTestDir: List<CompilationUnit> = parseFiles(filesInTestDir)

        val testFiles: List<CompilationUnit> = getTestFiles(parsedFilesInTestDir)

        return testFiles
            .flatMap { parseTestMethodsFromClass(it) }
            .toList()
            .also { logger.info { "Finished processing Java files in module: $path. Found ${it.size} test methods." } }
    }

    private fun parseTestMethodsFromClass(testClass: CompilationUnit): List<TestMethodInfo> {
        val javaClassMeta = JavaClassMeta(testClass)
        val sourceClass = classMapper.findSourceClass(javaClassMeta)
        val testClassInfo = TestClassInfo(
            javaClassMeta.name,
            javaClassMeta.packageName,
            module.projectInfo,
            module,
            sourceClass,
            language
        )
        val methodDeclarations = testClass.findAll(MethodDeclaration::class.java)

        return methodDeclarations
            .filter { it.annotations.any { it.nameAsString == "Test" || it.nameAsString == "ParameterizedTest" } }
            //TODO: make it configurable
            .filter { it.annotations.none { it.nameAsString == "Disabled" || it.nameAsString == "Ignored" } }
            .map { m ->
                val sourceMethodInfo =
                    sourceClass?.let {
                        DelegatingMethodMapper.findSourceMethod(JavaMethodMeta(m), it, sourceClass.file)
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