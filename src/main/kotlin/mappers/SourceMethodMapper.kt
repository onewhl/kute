package mappers

import SourceClassInfo
import SourceMethodInfo

object SourceMethodMapper {
    fun findSourceMethod(
        testMethod: MethodMeta,
        sourceClass: SourceClassInfo,
        candidateMethods: List<MethodMeta>
    ): SourceMethodInfo? {
        return candidateMethods.find { testMethod.hasMethodCall(it) }
            ?.let { SourceMethodInfo(it.name, it.body, sourceClass) }
    }
}