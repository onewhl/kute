package mappers

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtClass
import parsers.Lang
import parsers.StaticJavaFileParser
import parsers.StaticKotlinFileParser
import java.io.File

sealed interface MetaFactory {
    fun parse(file: File, content: String): List<ClassMeta>
}

private object KotlinMetaFactory: MetaFactory {
    override fun parse(file: File, content: String): List<ClassMeta> {
        val ktFile = StaticKotlinFileParser.parse(file.name, content)
        return PsiTreeUtil.findChildrenOfType(ktFile, KtClass::class.java).map { KotlinClassMeta(it) }
    }
}

private object JavaMetaFactory: MetaFactory {
    override fun parse(file: File, content: String) =
        StaticJavaFileParser.parse(content, file)?.let { compilationUnit ->
            compilationUnit.types.mapNotNull { classDecl ->
                (classDecl as? ClassOrInterfaceDeclaration)?.let { JavaClassMeta(compilationUnit, it) } }
        } ?: emptyList()
}

fun getMetaFactoryByLanguage(language: Lang): MetaFactory = when(language) {
    Lang.JAVA -> JavaMetaFactory
    Lang.KOTLIN -> KotlinMetaFactory
}