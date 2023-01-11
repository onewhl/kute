package mappers

import SourceClassInfo
import SourceMethodInfo

object SourceMethodMapper {
    fun findSourceMethod(
        testMethod: MethodMeta,
        sourceClass: SourceClassInfo,
        candidateMethods: Map<String, List<MethodMeta>>
    ): SourceMethodInfo? =
        testMethod.findLastMethodCall(candidateMethods)
            ?.let { SourceMethodInfo(it.name, it.body, sourceClass) }
}