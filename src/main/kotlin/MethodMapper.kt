import com.github.javaparser.JavaParser
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import java.io.File

/**
 * Responsible for mapping test methods on corresponding source methods.
 */
object MethodMapper {
    /**
     * To identify the source method for test, we use two heuristics:
     * 1: test name usually starts with "test" and contains the name of source method;
     * 2: test method has a call of source methods inside its body.
     */
    fun findSourceMethod(
        testMethod: MethodDeclaration,
        sourceClass: SourceClassInfo,
        classNameToFile: Map<String, List<File>>,
        parser: JavaParser
    ): SourceMethodInfo? {
        sourceClass.name.let { className ->
            val files = classNameToFile[className] ?: return null
            val parsedFile = parser.parse(files[0]).result.orElse(null)

            // get all methods from source class
            parsedFile?.let {
                val expectedSourceMethod =
                    testMethod.nameAsString.removePrefix("test").replaceFirstChar { it.lowercase() }
                val methods = it.findAll(MethodDeclaration::class.java)

                //use heuristic 1
                val methodsWithMatchedNames = methods.filter { expectedSourceMethod.contains(it.nameAsString) }

                //use heuristic 2
                methodsWithMatchedNames.forEach {
                    if (hasMethodCall(testMethod, it)) {
                        return SourceMethodInfo(expectedSourceMethod, getBody(it), sourceClass)
                    }
                }
            }
        }
        return null
    }

    private fun hasMethodCall(
        testMethod: MethodDeclaration,
        methodToSearch: MethodDeclaration
    ): Boolean {
        val methodCallVisitor = MethodCallVisitor(methodToSearch)
        testMethod.accept(methodCallVisitor, null)
        return methodCallVisitor.result
    }

    /**
     * Walks on method call expressions inside test method searching for call of source method.
     */
    internal class MethodCallVisitor(
        private val sourceMethod: MethodDeclaration
    ) : VoidVisitorAdapter<Void?>() {
        var result: Boolean = false

        override fun visit(methodCall: MethodCallExpr, arg: Void?) {
            val nameAsString = methodCall.nameAsString
            if (nameAsString == sourceMethod.nameAsString && sourceMethod.parameters.size == methodCall.arguments.size) {
                result = true
            }
            super.visit(methodCall, arg)
        }
    }

    private fun getBody(m: MethodDeclaration): String = if (m.body.isPresent) {
        m.body.get().toString()
    } else {
        ""
    }
}