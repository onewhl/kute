package mappers

import SourceClassAndLocation
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import parsers.Lang

interface ClassMeta {
    val name: String
    val packageName: String
    val language: Lang
    val methods: Iterable<MethodMeta>
    fun hasClassUsage(sourceClassAndLocation: SourceClassAndLocation): Boolean
    fun hasAnnotation(name: String): Boolean
    fun getAnnotationValue(name: String, key: String? = null): String?
}

class JavaClassMeta(private val testFile: CompilationUnit, private val testClass: ClassOrInterfaceDeclaration) : ClassMeta {
    override val name: String = testFile.primaryTypeName.orElse("")
    override val packageName: String = testFile.packageDeclaration.map { it.nameAsString }.orElse("")
    override val language: Lang
        get() = Lang.JAVA
    override val methods: Iterable<MethodMeta>
        get() = testClass.findAll(MethodDeclaration::class.java).map { JavaMethodMeta(it) }

    override fun hasClassUsage(sourceClassAndLocation: SourceClassAndLocation): Boolean {
        val sourceClass = sourceClassAndLocation.sourceClass
        val fqcn = sourceClass.fqcn
        val expectPackageNameOnUsage = packageName != sourceClass.pkg && testFile.hasImport(fqcn, sourceClass.pkg)
        return testClass.stream()
            .filter { it.hasClassUsage(sourceClass, expectPackageNameOnUsage) }
            .findAny()
            .isPresent
    }

    override fun hasAnnotation(name: String): Boolean = hasAnnotation(testClass.annotations, name)

    override fun getAnnotationValue(name: String, key: String?): String? =
        getAnnotationValue(testClass.annotations, name, key)
}

class KotlinClassMeta(private val testClass: KtClass) : ClassMeta {
    override val name: String = testClass.name!!
    override val packageName: String = testClass.containingKtFile.packageFqName.asString()
    override val language: Lang
        get() = Lang.KOTLIN
    override val methods: Iterable<MethodMeta>
        get() = testClass.declarations.mapNotNull { (it as? KtNamedFunction)?.let { KotlinMethodMeta(it)} }

    override fun hasClassUsage(sourceClassAndLocation: SourceClassAndLocation): Boolean {
        val sourceClass = sourceClassAndLocation.sourceClass
        val fqcn = sourceClass.fqcn
        val ktFile = testClass.containingKtFile
        val expectPackageNameOnUsage = packageName != sourceClass.pkg && ktFile.hasImport(fqcn, sourceClass.pkg)
        return testClass.findDescendantOfType<KtNameReferenceExpression> {
            it.name == sourceClass.name && (!expectPackageNameOnUsage ||
                    it.parent is KtDotQualifiedExpression && it.parent.text.startsWith(fqcn))
        } != null
    }

    override fun hasAnnotation(name: String): Boolean = hasAnnotation(testClass.annotationEntries, name)

    override fun getAnnotationValue(name: String, key: String?): String? =
        getAnnotationValue(testClass.annotationEntries, name, key)
}