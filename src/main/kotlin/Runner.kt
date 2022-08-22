import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.MethodDeclaration
import mu.KotlinLogging
import java.io.File
import java.io.FileOutputStream

class Runner : CliktCommand() {
    private val logger = KotlinLogging.logger {}

    //TODO: provide a path to the file with projects
    //TODO: add an argument with list of test frameworks to work with
    private val path by option(help = "Path to project").file(mustExist = true, canBeDir = true).required()
    private val output by option(help = "Path to output directory").file(canBeFile = true).required()

    override fun run() {
        //extract all test methods from Java test files in the project
        logger.info { "Start collecting Java test files..." }

        val javaFiles = extractJavaFiles(path)
        logger.info { "Found ${javaFiles.size} Java files." }

        val testFiles = getTestFiles(javaFiles)
        logger.info { "Found ${testFiles.size} test files." }

        val testMethods = getTestMethods(testFiles)
        logger.info { "Found ${testMethods.size} test methods." }

        //collect metadata about all test methods in the project
        logger.info { "Start collecting metadata about test methods..." }
        val methodInfos = mutableListOf<MethodInfo>()
        testMethods.forEach { m ->
            methodInfos.add(
                MethodInfo(m.nameAsString, getBody(m), getComment(m), getDisplayName(m))
            )
        }

        //write results to the provided file
        logger.info { "Writing results to the file." }
        FileOutputStream(output).apply {
            writeCsv(
                methodInfos
            )
        }
    }

    /**
     * Collects methods that have annotation Test.
     */
    private fun getTestMethods(testClasses: List<CompilationUnit>): MutableList<MethodDeclaration> {
        val testMethods = mutableListOf<MethodDeclaration>()

        testClasses
            .map { it.findAll(MethodDeclaration::class.java) }
            .forEach { methods ->
                testMethods += methods
                    .filter { m -> m.annotations.any { it.nameAsString == "Test" } }
                    .toCollection(mutableListOf())
            }
        return testMethods
    }

    private fun getTestFiles(projectFiles: List<File>): List<CompilationUnit> = projectFiles
        .map { JavaParser().parse(it).result }
        .filter { it.isPresent && isTestFile(it.get()) }
        .map { it.get() }

    /**
     * Checks if a file is a test file or not by searching for JUnit or TestNG imports.
     */
    private fun isTestFile(file: CompilationUnit): Boolean {
        return file.imports.any {
            it.nameAsString.contains("org.junit") ||
                    it.nameAsString.contains("org.testng")
        }
    }

    /**
     * Collect all Java files from the project.
     */
    private fun extractJavaFiles(project: File): List<File> = project
        .walkTopDown()
        .filter { it.extension == "java" }
        .toList()

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

fun main(args: Array<String>) = Runner().main(args)