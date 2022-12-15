package mappers

import SourceClassInfo
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import parsers.Lang

interface ClassMeta {
    val name: String
    val packageName: String
    val language: Lang
    val methods: Iterable<MethodMeta>
    fun hasClassUsage(sourceClass: SourceClassInfo): Boolean
}

class JavaClassMeta(private val testFile: CompilationUnit, private val testClass: ClassOrInterfaceDeclaration) : ClassMeta {
    override val name: String = testFile.primaryTypeName.orElse("")
    override val packageName: String = testFile.packageDeclaration.map { it.nameAsString }.orElse("")
    override val language: Lang
        get() = Lang.JAVA
    override val methods: Iterable<MethodMeta>
        get() = testClass.findAll(MethodDeclaration::class.java).map { JavaMethodMeta(it) }

    override fun hasClassUsage(sourceClass: SourceClassInfo): Boolean =
        JavaClassUsageResolver.isSourceClassUsed(testFile, testClass, sourceClass)
}

class KotlinClassMeta(private val testClass: KtClass) : ClassMeta {
    override val name: String = testClass.name!!
    override val packageName: String = testClass.containingKtFile.packageFqName.asString()
    override val language: Lang
        get() = Lang.KOTLIN
    override val methods: Iterable<MethodMeta>
        get() = testClass.declarations.mapNotNull { (it as? KtNamedFunction)?.let { KotlinMethodMeta(it)} }

    override fun hasClassUsage(sourceClass: SourceClassInfo): Boolean =
        KotlinClassUsageResolver.isSourceClassUsed(testClass.containingKtFile, testClass, sourceClass)
}