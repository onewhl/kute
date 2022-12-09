package mappers

import SourceClassInfo
import com.github.javaparser.ast.CompilationUnit
import org.jetbrains.kotlin.psi.KtClass
import parsers.Lang

interface ClassMeta {
    val name: String
    val packageName: String
    val language: Lang
    fun hasClassUsage(sourceClass: SourceClassInfo): Boolean
}

class JavaClassMeta(private val testClass: CompilationUnit) : ClassMeta {
    override val name: String = testClass.primaryTypeName.orElse("")
    override val packageName: String = testClass.packageDeclaration.map { it.nameAsString }.orElse("")
    override val language: Lang
        get() = Lang.JAVA

    override fun hasClassUsage(sourceClass: SourceClassInfo): Boolean =
        JavaClassUsageResolver.isSourceClassUsed(testClass, sourceClass)
}

class KotlinClassMeta(private val testClass: KtClass) : ClassMeta {
    override val name: String = testClass.name!!
    override val packageName: String = testClass.containingKtFile.packageFqName.asString()
    override val language: Lang
        get() = Lang.KOTLIN

    override fun hasClassUsage(sourceClass: SourceClassInfo): Boolean =
        KotlinClassUsageResolver.isSourceClassUsed(testClass, sourceClass)
}