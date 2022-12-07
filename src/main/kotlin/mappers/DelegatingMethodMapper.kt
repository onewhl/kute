package mappers

import SourceClassInfo
import SourceMethodInfo
import parsers.Lang
import java.io.File

object DelegatingMethodMapper {
    private val mapperByFileType: Map<String, MethodMapper<File>> = mapOf(
        Lang.JAVA.extension to JavaMethodMapper,
        Lang.KOTLIN.extension to KotlinMethodMapper
    )

    fun findSourceMethod(
        testMethod: MethodMeta,
        sourceClass: SourceClassInfo,
        file: File
    ): SourceMethodInfo? = mapperByFileType[file.extension]?.findSourceMethod(testMethod, sourceClass, file)
}

