package parsers

import ModuleInfo
import SourceMethodInfo
import TestClassInfo
import TestMethodInfo
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.AnnotationExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.NormalAnnotationExpr
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr
import com.github.javaparser.ast.expr.StringLiteralExpr
import mappers.*
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Extracts tests files, finds test method in them, and collects metadata.
 */
class JavaTestParser(
    path: File,
    module: ModuleInfo,
    classNameToFile: Map<String, List<File>>
) : AbstractParser<CompilationUnit, CompilationUnit, MethodDeclaration>(path, module, classNameToFile, logger) {
    override val language = Lang.JAVA
    override val metaFactory = JavaMetaFactory

    override fun findTestMethods(testClass: CompilationUnit): List<MethodDeclaration> =
        testClass.findAll(MethodDeclaration::class.java).asSequence()
            .filter { it.annotations.any { isTestMethodMarker(it.name.identifier) } }
            //TODO: make it configurable
            .filter { it.annotations.none { it.nameAsString == "Disabled" || it.nameAsString == "Ignored" } }
            .toList()

    override fun createTestMethodInfo(method: MethodDeclaration, classInfo: TestClassInfo, source: SourceMethodInfo?) =
        TestMethodInfo(
            method.nameAsString,
            getBody(method),
            getComment(method),
            getDisplayName(method),
            isParameterized(method),
            classInfo,
            source
        )

    private fun isParameterized(method: MethodDeclaration): Boolean =
        method.annotations.any { it.name.identifier == "ParameterizedTest" }

    override fun doParseSource(fileSource: File, content: String): CompilationUnit? =
        StaticJavaFileParser.parse(content, fileSource)

    override fun mapFileToClass(file: CompilationUnit): CompilationUnit = file

    /**
     * Checks if a file is a test file or not by searching for JUnit or TestNG imports.
     */
    override fun isTestFile(file: CompilationUnit): Boolean {
        return file.imports.any {
            it.nameAsString.startsWith("org.junit") ||
                    it.nameAsString.startsWith("junit.framework") ||
                    it.nameAsString.startsWith("org.testng")
        }
    }

    private fun getComment(method: MethodDeclaration) = method.comment.orElse(null)?.content ?: ""

    private fun getBody(method: MethodDeclaration): String = method.body.orElse(null)?.toString() ?: ""

    private fun getDisplayName(method: MethodDeclaration): String =
        method.annotations.find { a -> a.name.identifier == "DisplayName" } ?.getValue() ?: ""

    private fun Expression.toUnescapedString() = if (this is StringLiteralExpr) this.asString() else this.toString()

    private fun AnnotationExpr.getValue(): String? = when(this) {
        is SingleMemberAnnotationExpr -> this.memberValue.toUnescapedString()
        is NormalAnnotationExpr -> this.pairs.find { it.name.asString() == "value" } ?.value?.toUnescapedString()
        else -> null
    }

}