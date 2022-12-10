package mappers

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.MethodDeclaration
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction

sealed interface MetaFactory<Cls, Func> {
    fun createClassMeta(cls: Cls): ClassMeta
    fun createMethodMeta(func: Func): MethodMeta
}

object KotlinMetaFactory: MetaFactory<KtClass, KtNamedFunction> {
    override fun createClassMeta(cls: KtClass): ClassMeta = KotlinClassMeta(cls)
    override fun createMethodMeta(func: KtNamedFunction): MethodMeta = KotlinMethodMeta(func)
}

object JavaMetaFactory: MetaFactory<CompilationUnit, MethodDeclaration> {
    override fun createClassMeta(cls: CompilationUnit): ClassMeta = JavaClassMeta(cls)
    override fun createMethodMeta(func: MethodDeclaration): MethodMeta = JavaMethodMeta(func)
}