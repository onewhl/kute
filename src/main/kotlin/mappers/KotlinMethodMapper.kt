package mappers

import SourceClassInfo
import SourceMethodInfo
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtNamedFunction
import parsers.StaticKotlinFileParser
import java.io.File

object KotlinMethodMapper : MethodMapper<File> {
    override fun findSourceMethod(
        testMethod: MethodMeta,
        sourceClass: SourceClassInfo,
        input: File
    ): SourceMethodInfo? {
        // get all methods from source class
        StaticKotlinFileParser.parse(input).let { parsedFile ->
            val expectedSourceMethod =
                testMethod.name.removePrefix("test").replaceFirstChar { it.lowercase() }
            val methods = PsiTreeUtil.findChildrenOfType(parsedFile, KtNamedFunction::class.java)

            //use heuristic 1
            val methodsWithMatchedNames = methods.filter { expectedSourceMethod.contains(it.name.toString()) }

            //use heuristic 2
            methodsWithMatchedNames.forEach { sourceMethod ->
                if (testMethod.hasMethodCall(KotlinMethodMeta(sourceMethod))) {
                    val body = if (sourceMethod.bodyBlockExpression != null) {
                        sourceMethod.bodyBlockExpression!!.text
                    } else ""

                    return SourceMethodInfo(expectedSourceMethod, body, sourceClass)
                }
            }
        }
        return null
    }
}