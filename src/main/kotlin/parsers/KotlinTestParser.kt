package parsers

import ModuleInfo
import SourceClassInfo
import TestClassInfo
import TestMethodInfo
import mappers.DelegatingMethodMapper
import mappers.KotlinMethodMeta
import mu.KotlinLogging
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import utils.KotlinEnvironmentManager
import java.io.File

private val logger = KotlinLogging.logger {}

class KotlinTestParser(
    private val path: File,
    private val module: ModuleInfo,
    private val classNameToFile: Map<String, List<File>>
) : Parser {
    override val language = Lang.KOTLIN

    override fun process(files: List<File>): List<TestMethodInfo> {
        logger.info { "Start processing files in module: ." }

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
            .also { logger.info { "Finished processing files in module: $path. Found ${it.size} test methods." } }
    }

    private fun parseFiles(filePaths: List<File>): List<KtFile> =
        filePaths.map {
            KotlinEnvironmentManager.buildPsiFile(
                it.absolutePath, KotlinEnvironmentManager.createKotlinCoreEnvironment(HashSet()),
                it.readText()
            ) as KtFile
        }.toList()
            .also {
                logger.debug { "Parsed files in /test/ dir." }
            }

    private fun getImports(ktFile: KtFile): List<String> {
        val importedTypes: MutableList<String> = ArrayList()
        val importList = ktFile.importList
        if (importList != null) {
            for (importDeclaration in importList.imports) {
                val fqName: FqName? = importDeclaration.importedFqName
                val importName: String = fqName!!.asString()
                importedTypes.add(importName)
            }
        }
        return importedTypes
    }

    private fun parseTestMethodsFromClass(testClass: KtClass): List<TestMethodInfo> {
        val sourceClass = findSourceClass(testClass)
        val testClassInfo = TestClassInfo(testClass.name!!, module.projectInfo, module, sourceClass)

        return testClass.declarations.filterIsInstance<KtNamedFunction>()
            .filter { it.annotationEntries.any { it.text == "@Test" || it.text == "@ParameterizedTest" } }
            //TODO: make it configurable
            .filter { it.annotationEntries.none { it.text == "@Disabled" || it.text == "@Ignored" } }
            .map { m ->
                val sourceMethodInfo = sourceClass?.let {
                    DelegatingMethodMapper.findSourceMethod(KotlinMethodMeta(m), it, classNameToFile)
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

    private fun findSourceClass(testClass: KtClass) =
        testClass.name
            .let { if (it!!.startsWith("Test", ignoreCase = true)) it.substring("Test".length) else it }
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

    private fun getComment(method: KtNamedFunction): String {
        return if (method.docComment != null) {
            method.docComment!!.text
        } else ""
    }

    private fun getBody(method: KtNamedFunction): String {
        return if (method.docComment != null) {
            method.bodyBlockExpression!!.text
        } else ""
    }

    private fun getDisplayName(method: KtNamedFunction): String {
        return if (method.name != null) {
            method.name!!
        } else ""
    }
}