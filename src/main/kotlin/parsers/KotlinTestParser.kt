package parsers

import ModuleInfo
import TestClassInfo
import TestMethodInfo
import mappers.ClassMapper
import mappers.DelegatingMethodMapper
import mappers.KotlinClassMeta
import mappers.KotlinMethodMeta
import mu.KotlinLogging
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import java.io.File

private val logger = KotlinLogging.logger {}

class KotlinTestParser(
    private val path: File,
    private val module: ModuleInfo,
    classNameToFile: Map<String, List<File>>
) : Parser {
    override val language = Lang.KOTLIN
    private val classMapper = ClassMapper(module, classNameToFile)

    override fun process(files: List<File>): List<TestMethodInfo> {
        logger.info { "Start processing files in module: $path." }

        files.count { it.extension == language.extension }
            .let { logger.info { "Found: $it Kotlin files." } }

        val classesInTestDir = findFilesInTestDir(language, module.projectInfo.buildSystem, path, files.stream())
            .also { logger.info { "Found: ${it.size} Kotlin test classes." } }

        val parsedFilesInTestDir: List<KtFile> = parseFiles(classesInTestDir)

        val testFiles: List<KtFile> = getTestFiles(parsedFilesInTestDir)

        return testFiles
            .map { PsiTreeUtil.findChildrenOfType(it, KtClass::class.java).first() }
            .flatMap { parseTestMethodsFromClass(it as KtClass) }
            .toList()
            .also { logger.info { "Finished processing Kotlin files in module: $path. Found ${it.size} test methods." } }
    }

    private fun parseFiles(filePaths: List<File>): List<KtFile> =
        filePaths.map { StaticKotlinFileParser.parse(it) }.toList()
            .also { logger.debug { "Parsed ${it.size} files in /test/ dir." } }

    private fun getImports(ktFile: KtFile): List<String> =
        ktFile.importList?.imports?.map {
            it.importedFqName!!.asString()
        } ?: emptyList()

    private fun parseTestMethodsFromClass(testClass: KtClass): List<TestMethodInfo> {
        val classMeta = KotlinClassMeta(testClass)
        val sourceClass = classMapper.findSourceClass(classMeta)
        val testClassInfo = TestClassInfo(
            classMeta.name,
            classMeta.packageName,
            module.projectInfo,
            module,
            sourceClass,
            language
        )

        return testClass.declarations
            .filterIsInstance<KtNamedFunction>()
            .filter { it.annotationEntries.any { it.text == "@Test" || it.text == "@ParameterizedTest" } }
            //TODO: make it configurable
            .filter { it.annotationEntries.none { it.text == "@Disabled" || it.text == "@Ignored" } }
            .map { m ->
                val sourceMethodInfo = sourceClass?.let {
                    DelegatingMethodMapper.findSourceMethod(KotlinMethodMeta(m), it, sourceClass.file)
                }

                TestMethodInfo(
                    m.name!!,
                    getBody(m),
                    getComment(m),
                    getDisplayName(m),
                    isParameterized(m),
                    testClassInfo,
                    sourceMethodInfo
                )
            }.toList()
            .also { logger.debug { "Parsed test methods in test class ${testClass.name}." } }
    }

    private fun isParameterized(method: KtNamedFunction): Boolean {
        //TODO implement it
        return false
    }

    private fun getTestFiles(projectFiles: List<KtFile>): List<KtFile> =
        projectFiles
            .filter { isTestFile(it) }
            .also { logger.info { "Found ${it.size} test files." } }

    /**
     * Checks if a file is a test file or not by searching for JUnit or TestNG imports.
     */
    private fun isTestFile(file: KtFile): Boolean = getImports(file).any {
        it.contains("org.junit")
                || it.contains("junit.framework")
                || it.contains("org.testng")
    }

    private fun getComment(method: KtNamedFunction): String = method.docComment?.text ?: ""

    private fun getBody(method: KtNamedFunction): String = method.bodyBlockExpression?.text ?: ""

    private fun getDisplayName(method: KtNamedFunction): String = method.name ?: ""
}