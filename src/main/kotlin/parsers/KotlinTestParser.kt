package parsers

import ModuleInfo
import SourceMethodInfo
import TestClassInfo
import TestMethodInfo
import mappers.KotlinMetaFactory
import mu.KotlinLogging
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import java.io.File

private val logger = KotlinLogging.logger {}

class KotlinTestParser(
    path: File,
    module: ModuleInfo,
    classNameToFile: Map<String, List<File>>
) : AbstractParser<KtFile, KtClass, KtNamedFunction>(path, module, classNameToFile, logger) {
    override val language = Lang.KOTLIN
    override val metaFactory = KotlinMetaFactory

    override fun mapFileToClass(file: KtFile): KtClass? =
        PsiTreeUtil.findChildrenOfType(file, KtClass::class.java).firstOrNull()

    override fun doParseSource(fileSource: File, content: String): KtFile =
        StaticKotlinFileParser.parse(fileSource.name, content)

    /**
     * Checks if a file is a test file or not by searching for JUnit, TestNG, kotlin.test imports.
     */
    override fun isTestFile(file: KtFile): Boolean = hasImport(file) {
        it.contains("org.junit") || it.contains("junit.framework") ||
                it.contains("org.testng") || it.contains("kotlin.test")
    }

    private inline fun hasImport(ktFile: KtFile, predicate: (String) -> Boolean): Boolean =
        ktFile.importList?.imports?.any { it.importedFqName?.let { name -> predicate(name.asString()) } ?: false }
            ?: false
    override fun findTestMethods(testClass: KtClass) : List<KtNamedFunction> = testClass.declarations.asSequence()
        .filterIsInstance<KtNamedFunction>()
        .filter { method -> method.annotationEntries.any { isTestMethodMarker(it.shortName?.asString()) } }
        //TODO: make it configurable
        .filter { it.annotationEntries.none { it.text == "@Disabled" || it.text == "@Ignored" } }
        .toList()

    override fun createTestMethodInfo(method: KtNamedFunction, classInfo: TestClassInfo, source: SourceMethodInfo?) =
        TestMethodInfo(
            method.name!!,
            getBody(method),
            getComment(method),
            getDisplayName(method),
            isParameterized(method),
            classInfo,
            source
        )

    private fun isParameterized(method: KtNamedFunction): Boolean =
        method.annotationEntries.any { it.shortName?.asString() == "ParameterizedTest" }

    private fun getComment(method: KtNamedFunction): String = method.allChildren
        .filter { it is PsiComment }
        .joinToString(separator = "\n") { it.text }
        .trim()

    private fun getBody(method: KtNamedFunction): String = method.bodyExpression?.text ?: ""

    private fun getDisplayName(method: KtNamedFunction): String = method.annotationEntries
        .find { it.shortName?.asString() == "DisplayName" }
        ?.getAnnotationValue() ?: ""

    private fun KtAnnotationEntry.getAnnotationValue(): String? =
        this.valueArguments.find {
            it.getArgumentName().let { name -> name == null || name.asName.asString() == "value" }
        } ?.getArgumentExpression()?.findDescendantOfType<KtLiteralStringTemplateEntry>()?.text
}