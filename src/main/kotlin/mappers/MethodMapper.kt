package mappers

import SourceClassInfo
import SourceMethodInfo

interface MethodMapper<Input> {
    fun findSourceMethod(
        testMethod: MethodMeta,
        sourceClass: SourceClassInfo,
        input: Input
    ): SourceMethodInfo?
}
