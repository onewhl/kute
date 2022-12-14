package mappers

import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType

data class KotlinMethodMeta(val method: KtFunction) : MethodMeta {
    override val name = method.name!!
    override val parameters = method.valueParameters as List<Any>
    override val comment: String
        get() = method.allChildren
            .filter { it is PsiComment }
            .joinToString(separator = "\n") { it.text }
            .trim()
    override val body: String
        get() = method.bodyExpression?.text ?: ""

    override fun hasMethodCall(sourceMethod: MethodMeta): Boolean {
        val callExpressions = PsiTreeUtil.findChildrenOfType(method.bodyBlockExpression, KtCallExpression::class.java)
        callExpressions.forEach {
            if (it.calleeExpression != null &&
                it.calleeExpression!!.text == sourceMethod.name &&
                it.valueArguments.size == sourceMethod.parameters.size
            ) {
                return true
            }
        }
        return false
    }

    override fun hasAnnotation(name: String): Boolean =
        method.annotationEntries.any { it.shortName?.asString() == name }

    override fun getAnnotationValue(name: String, key: String?): String? =
        method.annotationEntries.find { it.shortName?.asString() == name }?.let {
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
}