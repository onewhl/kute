package mappers

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.*
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType

fun hasAnnotation(annotations: NodeList<AnnotationExpr>, name: String): Boolean =
    annotations.any { it.name.identifier == name }

fun getAnnotationValue(annotations: NodeList<AnnotationExpr>, name: String, key: String?) =
    annotations.find { it.name.identifier == name }?.let {
        if (key == null) it.getValue() else it.getValue(key)
    }

private fun Expression.toUnescapedString() = if (this is StringLiteralExpr) this.asString() else this.toString()

private fun AnnotationExpr.getValue(name: String): String? =
    (this as? NormalAnnotationExpr)?.pairs?.find { it.name.asString() == name }?.value?.toUnescapedString()

private fun AnnotationExpr.getValue(): String? = when(this) {
    is SingleMemberAnnotationExpr -> this.memberValue.toUnescapedString()
    is NormalAnnotationExpr -> this.pairs.find { it.name.asString() == "value" } ?.value?.toUnescapedString()
    else -> null
}

fun hasAnnotation(annotations: List<KtAnnotationEntry>, name: String): Boolean =
    annotations.any { it.shortName?.asString() == name }

fun getAnnotationValue(annotations: List<KtAnnotationEntry>, name: String, key: String?): String? =
    annotations.find { it.shortName?.asString() == name }?.let {
        if (key == null) it.getValue() else it.getValue(key)
    }

private fun ValueArgument.toUnescapedString() =
    this.getArgumentExpression()?.findDescendantOfType<LeafPsiElement> { !it.textMatches("\"") } ?.text

private fun KtAnnotationEntry.getValue(name: String): String? =
    this.valueArguments.find { arg ->
        arg.getArgumentName().let { it != null && it.asName.asString() == name }
    } ?.toUnescapedString()

private fun KtAnnotationEntry.getValue(): String? =
    this.valueArguments.find {
        it.getArgumentName().let { name -> name == null || name.asName.asString() == "value" }
    } ?.toUnescapedString()