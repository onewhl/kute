import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.MemoryTypeSolver
import mu.KotlinLogging
import java.io.File

class TestExtractor(
    val path: File,
    val module: ModuleInfo
) : Runnable {
    private val logger = KotlinLogging.logger {}

    private val buildSystem = module.projectInfo.buildSystem
    private lateinit var classNameToFile: Map<String, List<File>>
    private val parser = JavaParser(ParserConfiguration().setSymbolResolver(JavaSymbolSolver(MemoryTypeSolver())))

    override fun run() {
        logger.info { "Start processing files in module: $path." }

        val javaFiles: List<File> = extractJavaFiles(path)

        classNameToFile = javaFiles.groupBy { it.name.removeSuffix(".java") }

        val classesInTestDir = findJavaFilesInTestDirectory(javaFiles)

        val parsedFilesInTestDir: List<CompilationUnit> = parseFiles(classesInTestDir)

        val testFiles: List<CompilationUnit> = getTestFiles(parsedFilesInTestDir)

        val testMethodInfos = testFiles
            .flatMap { parseTestMethodsFromClass(it) }
            .toList()
            .also { logger.info { "Found ${it.size} test methods." } }

        //TODO: come up with a way to store results
        logger.info { "Finished processing files in module: $path." }
    }

    //TODO: write results to DB after each creation of TestMethodInfo?
    //TODO: isolate mapping algorithm, make it configurable -- run mapping or not
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
                    sourceClass?.let { MethodMapper.findSourceMethod(m, it, classNameToFile, parser) }
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
        .map { parser.parse(it).result }
        .filter { it.isPresent }
        .map { it.get() }.toList()
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

    /**
     * Collect all Java files from the module.
     */
    private fun extractJavaFiles(module: File): List<File> = module
        .walkTopDown()
        .filter { it.extension == "java" }
        .toList()
        .also { logger.info { "Found ${it.size} Java files in module ${module.path}." } }

    private fun findJavaFilesInTestDirectory(javaFiles: Collection<File>): Collection<File> =
        if (buildSystem == BuildSystem.MAVEN || buildSystem == BuildSystem.GRADLE) {
            val sep = File.separator
            val testDirectory = File(path, "src${sep}test${sep}java")
            javaFiles.filter { it.startsWith(testDirectory) }
        } else {
            javaFiles
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