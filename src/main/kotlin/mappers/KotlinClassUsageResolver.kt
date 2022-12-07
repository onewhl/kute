package mappers

import SourceClassInfo
import org.jetbrains.kotlin.psi.KtClass

object KotlinClassUsageResolver : ClassUsageResolver<KtClass> {
    override fun isSourceClassUsed(testClass: KtClass, sourceClass: SourceClassInfo): Boolean {
        // TODO("must implement similar to Java")
        return true
    }
}