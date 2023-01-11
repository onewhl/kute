package mappers

import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.forEachDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier

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
    override val isPublic = method.visibilityModifier()?.let { it.text == "public" } ?: true

    private fun isAnyMethod(name: String, args: List<*>) =
        args.isEmpty() && (name == "equals" || name == "hashCode" || name == "toString")

    override fun findLastMethodCall(sourceMethods: Map<String, List<MethodMeta>>): MethodMeta? {
        var lastMethod: MethodMeta? = null
        method.bodyExpression?.forEachDescendantOfType<KtCallExpression> {
            it.calleeExpression?.let { calleeExpression ->
                val name = calleeExpression.text
                val args = it.valueArguments
                sourceMethods[name]
                    ?.find { sourceMethod -> args.size == sourceMethod.parameters.size }
                    ?.takeIf { !isAnyMethod(name, args) }
                    ?.also { matchedMethod -> lastMethod = matchedMethod }
            }
        }
        return lastMethod
    }

    override fun hasAnnotation(name: String): Boolean = hasAnnotation(method.annotationEntries, name)

    override fun getAnnotationValue(name: String, key: String?): String? =
        getAnnotationValue(method.annotationEntries, name, key)
}