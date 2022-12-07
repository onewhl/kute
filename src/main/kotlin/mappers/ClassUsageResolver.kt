package mappers

import SourceClassInfo

interface ClassUsageResolver<T> {
    fun isSourceClassUsed(testClass: T, sourceClass: SourceClassInfo): Boolean
}