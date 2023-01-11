package mappers

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.comments.LineComment
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.visitor.VoidVisitorAdapter

data class JavaMethodMeta(val method: MethodDeclaration) : MethodMeta {
    override val name = method.nameAsString
    override val parameters = method.parameters as List<Any>
    override val body: String
        get() = method.body.orElse(null)?.toString() ?: ""
    override val isPublic = method.isPublic

    // JavaParser binds at most one line comment to block, so we need to check orphanComments to find other lines
    override val comment: String
        get() =  method.comment.orElse(null)?.let { comment ->
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

    private fun LineComment.line(): Int = this.range.orElse(null)?.end?.line ?: -1

    override fun findLastMethodCall(sourceMethods: Map<String, List<MethodMeta>>): MethodMeta? {
        val methodCallVisitor = SourceMethodFinder(sourceMethods)
        method.accept(methodCallVisitor, null)
        return methodCallVisitor.lastMatchedMethod
    }

    override fun hasAnnotation(name: String): Boolean =
        method.annotations.any { it.name.identifier == name }

    override fun getAnnotationValue(name: String, key: String?): String? =
        getAnnotationValue(method.annotations, name, key)

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

    private class SourceMethodFinder(
        private val sourceMethods: Map<String, List<MethodMeta>>
    ) : VoidVisitorAdapter<Void?>() {
        var lastMatchedMethod: MethodMeta? = null

        private fun isAnyMethod(name: String, args: List<*>) =
            args.isEmpty() && (name == "equals" || name == "hashCode" || name == "toString")

        override fun visit(methodCall: MethodCallExpr, arg: Void?) {
            val nameAsString = methodCall.nameAsString
            sourceMethods[nameAsString]?.find { it.parameters.size == methodCall.arguments.size }
                ?.takeIf { !isAnyMethod(nameAsString, methodCall.arguments) }
                ?.also { lastMatchedMethod = it }
            super.visit(methodCall, arg)
        }
    }

}