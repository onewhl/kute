package mappers

import SourceClassInfo
import SourceMethodInfo
import com.github.javaparser.ast.body.MethodDeclaration
import parsers.StaticJavaFileParser
import java.io.File

/**
 * Responsible for mapping test methods on corresponding source methods.
 */
object JavaMethodMapper : MethodMapper<File> {
    /**
     * To identify the source method for test, we use two heuristics:
     * 1: test name usually starts with "test" and contains the name of source method;
     * 2: test method has a call of source methods inside its body.
     */
    override fun findSourceMethod(
        testMethod: MethodMeta,
        sourceClass: SourceClassInfo,
        input: File
    ): SourceMethodInfo? {
        // get all methods from source class
        StaticJavaFileParser.parse(input)?.let { parsedFile ->
            val expectedSourceMethod =
                testMethod.name.removePrefix("test").replaceFirstChar { it.lowercase() }
            val methods = parsedFile.findAll(MethodDeclaration::class.java)

            //use heuristic 1
            val methodsWithMatchedNames = methods.filter { expectedSourceMethod.contains(it.nameAsString) }

            //use heuristic 2
            methodsWithMatchedNames.forEach { sourceMethod ->
                if (testMethod.hasMethodCall(JavaMethodMeta(sourceMethod))) {
                    val body = sourceMethod.body.map { it.toString() }.orElse("")
                    return SourceMethodInfo(expectedSourceMethod, body, sourceClass)
                }
            }
        }
        return null
    }
}