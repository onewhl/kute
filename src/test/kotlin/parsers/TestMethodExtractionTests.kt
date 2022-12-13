package parsers

import BuildSystem
import ModuleInfo
import ProjectInfo
import TestClassInfo
import TestFramework
import TestMethodInfo
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Disabled
import org.junitpioneer.jupiter.cartesian.CartesianTest
import org.junitpioneer.jupiter.cartesian.CartesianTest.Enum
import java.io.File


@Disabled("Should be disabled until all features implemented")
class TestMethodExtractionTests {
    @CartesianTest(name = "Canonical {0} tests in {1} can be extracted")
    fun `canonical tests can be extracted`(
        @Enum framework: TestFramework,
        @Enum lang: Lang
    ) {
        runTest("Canonical", framework, lang) { classInfo ->
            listOf(
                TestMethodInfo(
                    "testSimpleCase",
                    formatBlock("assertEquals(1, 1)", lang),
                    "",
                    "",
                    false,
                    classInfo,
                    null,
                    1
                )
            )
        }
    }

    @CartesianTest(name = "{0} tests in {1} defined using fqcn instead of import should be extracted")
    fun `tests defined using fqcn instead of import should be extracted`(
        @Enum framework: TestFramework,
        @Enum lang: Lang
    ) {
        runTest("NoImport", framework, lang) { classInfo ->
            listOf(
                TestMethodInfo(
                    "testWithoutImports",
                    formatBlock("assertEquals(1, 1)", lang),
                    "",
                    "",
                    false,
                    classInfo,
                    null,
                    1
                )
            )
        }
    }

    @CartesianTest(name = "Display name for {0} tests in {1} should be extracted")
    fun `displayName is correctly extracted from tests`(
        @Enum(names = ["JUNIT3", "KOTLIN_TEST"], mode = Enum.Mode.EXCLUDE) framework: TestFramework,
        @Enum lang: Lang
    ) {
        runTest("DisplayName", framework, lang) { classInfo ->
            listOf(
                TestMethodInfo(
                    "testWithDisplayName",
                    formatBlock("assertEquals(1, 1)", lang),
                    "",
                    "Test with DisplayName",
                    false,
                    classInfo,
                    null,
                    1
                )
            )
        }
    }


    private fun formatBlock(code: String, lang: Lang) = if (lang == Lang.KOTLIN) code else {
        """
        {
            ${code.replace("TODO", "throw new UnsupportedOperationException")};
        }
        """.trimIndent()
    }

    private fun runTest(
        classNamePrefix: String, framework: TestFramework, lang: Lang,
        expectedResultProvider: (TestClassInfo) -> List<TestMethodInfo>
    ) {
        Assumptions.assumeFalse(
            lang == Lang.JAVA && framework == TestFramework.KOTLIN_TEST,
            "Kotlin.test not supported for Java"
        )

        val className = "${classNamePrefix}${lang.name.lowercase().capitalizeAsciiOnly()}Test"
        val pathToModule = frameworkToModule[framework]!!
        val moduleInfo = createModule(pathToModule)
        val parser = createParser(lang, pathToModule, moduleInfo, emptyMap())
        val testFile = File(pathToModule, "src/test/${lang.name.lowercase()}/project/${className}.${lang.extension}")
        val classInfo = TestClassInfo(className, "project", moduleInfo.projectInfo, moduleInfo, null, lang, 1)

        assertThat(parser.process(listOf(testFile))).usingRecursiveComparison()
            .ignoringFields("id", "classInfo.id")
            .isEqualTo(expectedResultProvider(classInfo))
    }

    private fun createModule(path: File) =
        ModuleInfo(path.nameWithoutExtension, ProjectInfo("test_project", BuildSystem.GRADLE, id = 1), id = 1)

    companion object {
        val frameworkToModule = TestFramework.values().associateWith {
            File("test_project", it.name.replace("_", "").lowercase())
        }
    }
}