package parsers

import BuildSystem
import java.io.File
import java.util.function.Predicate

class TestFileFilter(separator: String) {
    private val simpleTestPathPrefix = "src${separator}test${separator}"
    private val testPathPrefixRegex = "src${separator}[^${separator}tT]*[Tt]est${separator}.*?"
        .replace("\\", "\\\\")
        .toPattern()

    internal fun findFilesInTestDirImpl(language: Lang, buildSystem: BuildSystem, sourceFiles: List<File>): List<File> =
        when (buildSystem) {
            BuildSystem.GRADLE -> Predicate<File> { testPathPrefixRegex.matcher(it.toString()).find() }
            BuildSystem.MAVEN -> Predicate<File> { it.toString().contains(simpleTestPathPrefix) }
            else -> Predicate<File> { true }
        }.let { predicate -> sourceFiles.filter { it.extension == language.extension && predicate.test(it) } }

    companion object {
        private val INSTANCE = TestFileFilter(File.separator)

        fun findFilesInTestDir(language: Lang, buildSystem: BuildSystem, sourceFiles: List<File>): List<File> =
            INSTANCE.findFilesInTestDirImpl(language, buildSystem, sourceFiles)
    }
}