import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
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
        logger.info { "Start processing project $path" }

        //extract all test methods from Java test files in the project
        logger.info { "Start collecting Java test files..." }

        val javaFiles: List<File> = extractJavaFiles(path)

        classNameToFile = javaFiles.groupBy { it.name.removeSuffix(".java") }

        val classesInTestDir = findJavaFilesInTestDirectory(javaFiles)

        val parsedFilesInTestDir: List<CompilationUnit> = parseFiles(classesInTestDir)

        val filesWithTestImports: List<CompilationUnit> = getTestFiles(parsedFilesInTestDir)

        val testMethodInfos = filesWithTestImports
            .flatMap { parseTestMethodsFromClass(it) }
            .toList()

        logger.info { "Found ${testMethodInfos.size} test methods." }

        logger.info { "Finished processing files in $path" }
    }

    private fun parseTestMethodsFromClass(testClass: CompilationUnit): List<TestMethodInfo> {
        val sourceClass = findSourceClass(testClass)
        val testClassInfo = TestClassInfo(testClass.primaryTypeName.orElse(""), module.projectInfo, module, sourceClass)
        val methodDeclarations = testClass.findAll(MethodDeclaration::class.java)
        return methodDeclarations.filter { m -> m.annotations.any { it.nameAsString == "Test" || it.nameAsString == "ParameterizedTest" } }
            .map { m ->
                val sourceMethodInfo = findSourceMethod(m, sourceClass)
                TestMethodInfo(
                    m.nameAsString,
                    getBody(m),
                    getComment(m),
                    getDisplayName(m),
                    checkParameterized(m),
                    testClassInfo,
                    sourceMethodInfo
                )
            }
    }

    // TODO implement
    private fun checkParameterized(method: MethodDeclaration): Boolean = false

    private fun findSourceClass(testClass: CompilationUnit) = testClass.primaryTypeName.orElse(null)
        .let { if (it.startsWith("Test", ignoreCase = true)) it.substring("Test".length) else it }
        .let { if (it.endsWith("Test", ignoreCase = true)) it.substring(0, it.length - "Test".length) else it }
        .let { if (it.endsWith("ITCase", ignoreCase = true)) it.substring(0, it.length - "ITCase".length) else it }
        .let { if (it.endsWith("Case", ignoreCase = true)) it.substring(0, it.length - "Case".length) else it }
        .takeIf { classNameToFile.containsKey(it) }
        ?.let { SourceClassInfo(it, module) }
        .also {
            if (it != null && classNameToFile[it.name]!!.size > 1) {
                logger.warn { "Multiple classes found with name $it.name: ${classNameToFile[it.name]}" }
            }
        }

    private fun findSourceMethod(testMethod: MethodDeclaration, sourceClass: SourceClassInfo?): SourceMethodInfo? {
        sourceClass?.name?.let { className ->
            val files = classNameToFile[className]!!
            parser.parse(files[0]).result.orElse(null)?.let {
                val expectedSourceMethod =
                    testMethod.nameAsString.removePrefix("test").replaceFirstChar { it.lowercase() }
                val methods = it.findAll(MethodDeclaration::class.java)
                    .groupBy { it.nameAsString }

                val innerClasses = emptyMap<String, ClassOrInterfaceDeclaration>() // TODO find inner classes correctly
                // 1. if name of the test method equals name of source method
                methods[expectedSourceMethod]?.takeIf { it.size == 1 }?.let {
                    return SourceMethodInfo(expectedSourceMethod, getBody(it[0]), sourceClass)
                }

                methods.forEach {
                    if (expectedSourceMethod.contains(it.key)) {
                        return SourceMethodInfo(expectedSourceMethod, getBody(it.value[0]), sourceClass)
                    }
                }

                // 2. if name of the inner class equals name of the test method, e.g. testBuilder
                innerClasses[expectedSourceMethod]?.let {
                    return SourceMethodInfo(it.nameAsString, "", sourceClass)
                }

                // 3. if method in a source class is called from test method
                val methodCallVisitor = MethodCallVisitor(className, methods)
                testMethod.accept(methodCallVisitor, null)
                val resolvedSourceMethod = methodCallVisitor.sourceMethodCall
                if (resolvedSourceMethod != null) {
                    return SourceMethodInfo(expectedSourceMethod, getBody(resolvedSourceMethod), sourceClass)
                }

            }
        }
        return null
    }

    internal class MethodCallVisitor(
        private val className: String,
        private val sourceMethods: Map<String, List<MethodDeclaration>>
    ) : VoidVisitorAdapter<Void?>() {
        var sourceMethodCall: MethodDeclaration? = null

        override fun visit(methodCall: MethodCallExpr, arg: Void?) {
            val nameAsString =
                methodCall.nameAsString //TODO: it gets the last method call in chain, we should process all of them
            sourceMethods[nameAsString]?.let {
                it.firstOrNull { it.parameters.size == methodCall.arguments.size }?.let {
                    sourceMethodCall = it
                }
            }
            super.visit(methodCall, arg)
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
     * Collect all Java files from the project.
     */
    private fun extractJavaFiles(project: File): List<File> = project
        .walkTopDown()
        .filter { it.extension == "java" }
        .toList()
        .also { logger.info { "Found ${it.size} Java files." } }

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