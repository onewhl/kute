package mappers

import SourceClassInfo
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile

object KotlinClassUsageResolver : ClassUsageResolver<KtFile, KtClass> {
    override fun isSourceClassUsed(testFile: KtFile, testClass: KtClass, sourceClass: SourceClassInfo): Boolean {
        // TODO("must implement similar to Java")
        return true
    }
}