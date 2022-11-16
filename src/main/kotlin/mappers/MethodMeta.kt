package mappers

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFunction

interface MethodMeta {
    val name: String
    val parameters: List<Any>

    fun hasMethodCall(sourceMethod: MethodMeta): Boolean
}

data class JavaMethodMeta(private val delegate: MethodDeclaration) : MethodMeta {
    override val name = delegate.nameAsString
    override val parameters = delegate.parameters as List<Any>

    override fun hasMethodCall(sourceMethod: MethodMeta): Boolean {
        val methodCallVisitor = MethodCallVisitor(sourceMethod)
        delegate.accept(methodCallVisitor, null)
        return methodCallVisitor.result
    }
}

data class KotlinMethodMeta(private val delegate: KtFunction) : MethodMeta {
    override val name = delegate.name!!
    override val parameters = delegate.valueParameters as List<Any>

    override fun hasMethodCall(sourceMethod: MethodMeta): Boolean {
        val callExpressions = PsiTreeUtil.findChildrenOfType(delegate.bodyBlockExpression, KtCallExpression::class.java)
        callExpressions.forEach {
            TODO()
        }

        return false
    }
}

/**
 * Walks on method call expressions inside test method searching for call of source method.
 */
private class MethodCallVisitor(
    private val sourceMethod: MethodMeta
) : VoidVisitorAdapter<Void?>() {
    var result: Boolean = false

    override fun visit(methodCall: MethodCallExpr, arg: Void?) {
        val nameAsString = methodCall.nameAsString
        if (nameAsString == sourceMethod.name && sourceMethod.parameters.size == methodCall.arguments.size) {
            result = true
        }
        super.visit(methodCall, arg)
    }
}

