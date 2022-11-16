package parsers

import BuildSystem
import TestMethodInfo
import java.io.File
import java.util.stream.Collectors
import java.util.stream.Stream

interface Parser {
    fun process(files: List<File>): List<TestMethodInfo>
    val language: Lang
}

fun findFilesInTestDir(language: Lang, buildSystem: BuildSystem, path: File, javaFiles: Stream<File>): List<File> =
    if (buildSystem == BuildSystem.MAVEN || buildSystem == BuildSystem.GRADLE) {
        val sep = File.separator
        val testDirectory = File(path, "src${sep}test${sep}${language.name.lowercase()}")
        javaFiles.filter { it.startsWith(testDirectory) }
    } else {
        javaFiles
    }.filter { it.extension == language.extension }
        .collect(Collectors.toList())