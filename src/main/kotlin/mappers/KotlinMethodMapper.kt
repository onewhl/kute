package mappers

import SourceClassInfo
import SourceMethodInfo
import java.io.File

object KotlinMethodMapper : MethodMapper<File> {
    override fun findSourceMethod(
        testMethod: MethodMeta,
        sourceClass: SourceClassInfo,
        input: File
    ): SourceMethodInfo? {
        TODO()
    }
}