package parsers

import ModuleInfo
import SourceMethodInfo
import TestClassInfo
import TestMethodInfo
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.comments.LineComment
import com.github.javaparser.ast.expr.*
import mappers.JavaMetaFactory
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
            .toList()

    override fun createTestMethodInfo(method: MethodDeclaration, classInfo: TestClassInfo, source: SourceMethodInfo?) =
        TestMethodInfo(
            method.nameAsString,
            getBody(method),
            getComment(method),
            getDisplayName(method),
            isParameterized(method),
            isDisabled(method),
            classInfo,
            source
        )

    private fun isDisabled(method: MethodDeclaration): Boolean =
        method.annotations.any { it.name.identifier == "Disabled" || it.name.identifier == "Ignored" }

    private fun isParameterized(method: MethodDeclaration): Boolean =
        method.annotations.any { it.name.identifier == "ParameterizedTest" }

    override fun doParseSource(fileSource: File, content: String): CompilationUnit? =
        StaticJavaFileParser.parse(content, fileSource)

    override fun mapFileToClass(file: CompilationUnit): CompilationUnit = file

    private fun LineComment.line(): Int = this.range.orElse(null)?.end?.line ?: -1

    // JavaParser binds at most one line comment to block, so we need to check orphanComments to find other lines
    private fun getComment(method: MethodDeclaration) = method.comment.orElse(null)?.let { comment ->
        if (comment is LineComment) {
            method.parentNode.orElse(null)?.orphanComments?.takeIf { it.isNotEmpty() }
                ?.let {
                    val comments = ArrayDeque<String>()
                    comments.addLast(comment.toString())
                    var prevLine = comment.line()
                    for (orphanComment in it.reversed()) {
                        if (orphanComment is LineComment && orphanComment.line() == prevLine - 1) {
                            --prevLine
                            comments.addFirst(orphanComment.toString())
                        }
                    }
                    comments.joinToString(separator = "")
                } ?: comment.toString()
        } else {
            comment.toString()
        }
    }?.trim() ?: ""

    private fun getBody(method: MethodDeclaration): String = method.body.orElse(null)?.toString() ?: ""

    private fun getDisplayName(method: MethodDeclaration): String =
        method.annotations.find { a -> a.name.identifier == "DisplayName" } ?.getValue() ?: ""

    private fun Expression.toUnescapedString() = if (this is StringLiteralExpr) this.asString() else this.toString()

    private fun AnnotationExpr.getValue(name: String): String? =
        (this as? NormalAnnotationExpr)?.pairs?.find { it.name.asString() == name }?.value?.toUnescapedString()

    private fun AnnotationExpr.getValue(): String? = when(this) {
        is SingleMemberAnnotationExpr -> this.memberValue.toUnescapedString()
        is NormalAnnotationExpr -> this.pairs.find { it.name.asString() == "value" } ?.value?.toUnescapedString()
        else -> null
    }

}