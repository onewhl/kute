package mappers

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import parsers.Lang
import parsers.StaticJavaFileParser
import parsers.StaticKotlinFileParser
import java.io.File

sealed interface MetaFactory {
    fun parseClasses(file: File, content: String): List<ClassMeta>

    fun parseMethods(file: File): List<MethodMeta>
}

private object KotlinMetaFactory: MetaFactory {
    override fun parseClasses(file: File, content: String): List<ClassMeta> {
        val ktFile = StaticKotlinFileParser.parse(file.name, content)
        return PsiTreeUtil.findChildrenOfType(ktFile, KtClass::class.java)
            .mapNotNull { cls -> cls.name?.let { KotlinClassMeta(cls) } }
    }

    override fun parseMethods(file: File): List<MethodMeta> {
        val ktFile = StaticKotlinFileParser.parse(file)
        return PsiTreeUtil.findChildrenOfType(ktFile, KtNamedFunction::class.java)
            .mapNotNull { method -> method.name?.let { KotlinMethodMeta(method) } }
    }
}

private object JavaMetaFactory: MetaFactory {
    override fun parseClasses(file: File, content: String) =
        StaticJavaFileParser.parse(content, file)?.let { compilationUnit ->
            compilationUnit.types.mapNotNull { classDecl ->
                (classDecl as? ClassOrInterfaceDeclaration)?.let { JavaClassMeta(compilationUnit, it) }
            }
        } ?: emptyList()

    override fun parseMethods(file: File): List<MethodMeta> =
        StaticJavaFileParser.parse(file)?.let { compilationUnit ->
            compilationUnit.findAll(MethodDeclaration::class.java)
                .map { JavaMethodMeta(it) }
        } ?: emptyList()
}

fun getMetaFactoryByLanguage(language: Lang): MetaFactory = when(language) {
    Lang.JAVA -> JavaMetaFactory
    Lang.KOTLIN -> KotlinMetaFactory
}