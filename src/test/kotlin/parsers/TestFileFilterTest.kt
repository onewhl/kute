package parsers

import BuildSystem
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import kotlin.test.assertEquals

class TestFileFilterTest {
    @ParameterizedTest
    @CsvSource(value = ["/,ANT", "/,OTHER", "\\,ANT", "\\,OTHER"])
    fun `test that heuristics does not work for non-MAVEN nor-GRADLE projects`(
        separator: String,
        buildSystem: BuildSystem
    ) {
        val filter = TestFileFilter(separator)
        val sourceFiles = listOf("Test1.java", "Test2.java", "Entity.java").map { File(it) }
        assertEquals(sourceFiles, filter.findFilesInTestDirImpl(Lang.JAVA, buildSystem, sourceFiles))
    }

    @ParameterizedTest
    @ValueSource(strings = ["/", "\\"])
    fun `test that heuristics considers predefined test directory for MAVEN projects`(pathSeparator: String) {
        val filter = TestFileFilter(pathSeparator)
        val sourceFiles = listOf(
            "Test1.java",
            "src/main/Entity.java",
            "src/test/Test2.java",
            "/home/dev/project/src/test/Test3.java",
            "src/nonJvmTest/Test4.java",
            "/home/dev/project/src/commonTest/Test5.java"
        ).map { File(it.replace("/", pathSeparator)) }
        val expectedOutput = listOf("src/test/Test2.java",  "/home/dev/project/src/test/Test3.java")
            .map { File(it.replace("/", pathSeparator)) }
        assertEquals(expectedOutput, filter.findFilesInTestDirImpl(Lang.JAVA, BuildSystem.MAVEN, sourceFiles))
    }

    @ParameterizedTest
    @ValueSource(strings = ["/", "\\"])
    fun `test that heuristics supports Kotlin Multiplatform test directories for GRADLE projects`(pathSeparator: String) {
        val filter = TestFileFilter(pathSeparator)
        val sourceFiles = listOf(
            "Test1.java",
            "src/main/Entity.java",
            "src/test/Test2.java",
            "/home/dev/project/src/test/Test3.java",
            "src/nonJvmTest/Test4.java",
            "/home/dev/project/src/commonTest/Test5.java"
        ).map { File(it.replace("/", pathSeparator)) }
        val expectedOutput = listOf(
            "src/test/Test2.java",
            "/home/dev/project/src/test/Test3.java",
            "src/nonJvmTest/Test4.java",
            "/home/dev/project/src/commonTest/Test5.java"
        ).map { File(it.replace("/", pathSeparator)) }
        assertEquals(expectedOutput, filter.findFilesInTestDirImpl(Lang.JAVA, BuildSystem.GRADLE, sourceFiles))
    }
}