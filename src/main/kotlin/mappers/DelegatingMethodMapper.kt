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
        classNameToFiles: Map<String, List<File>>
    ): SourceMethodInfo? = classNameToFiles[sourceClass.name]?.firstNotNullOfOrNull {
        mapperByFileType[it.extension]?.findSourceMethod(testMethod, sourceClass, it)
    }
}

