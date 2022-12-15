package mappers

import SourceClassInfo

interface ClassUsageResolver<File, Cls> {
    fun isSourceClassUsed(testFile: File, testClass: Cls, sourceClass: SourceClassInfo): Boolean
}