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
    ) = runTest("Canonical", framework, lang) { classInfo ->
        listOf(
            TestMethodInfo(
                name = "testSimpleCase",
                body = formatBlock("assertEquals(1, 1)", lang),
                comment = "",
                displayName = "",
                isParametrised = false,
                isDisabled = false,
                classInfo = classInfo,
                sourceMethod = null
            )
        )
    }

    @CartesianTest(name = "{0} tests in {1} defined using fqcn instead of import should be extracted")
    fun `tests defined using fqcn instead of import should be extracted`(
        @Enum framework: TestFramework,
        @Enum lang: Lang
    ) = when(framework) {
        TestFramework.JUNIT3 -> "assertEquals(1, 1)"
        TestFramework.JUNIT4 -> "org.junit.Assert.assertEquals(1, 1)"
        TestFramework.JUNIT5 -> "org.junit.jupiter.api.Assertions.assertEquals(1, 1)"
        TestFramework.TESTNG -> "org.testng.Assert.assertEquals(1, 1)"
        TestFramework.KOTLIN_TEST -> "kotlin.test.assertEquals(1, 1)"
    }.let { body ->
        runTest("NoImport", framework, lang) { classInfo ->
            listOf(
                TestMethodInfo(
                    name = "testWithoutImports",
                    body = formatBlock(body, lang),
                    comment = "",
                    displayName = "",
                    isParametrised = false,
                    isDisabled = false,
                    classInfo = classInfo,
                    sourceMethod = null
                )
            )
        }
    }

    @CartesianTest(name = "Display name for {0} tests in {1} should be extracted")
    fun `displayName is correctly extracted from tests`(
        @Enum(names = ["JUNIT3", "KOTLIN_TEST"], mode = Enum.Mode.EXCLUDE) framework: TestFramework,
        @Enum lang: Lang
    ) = runTest("DisplayName", framework, lang) { classInfo ->
        listOf(
            TestMethodInfo(
                name = "testWithDisplayName",
                body = formatBlock("assertEquals(1, 1)", lang),
                comment = "",
                displayName = "Test with DisplayName",
                isParametrised = false,
                isDisabled = false,
                classInfo = classInfo,
                sourceMethod = null
            )
        )
    }

    @CartesianTest(name = "All types of comments supported in {0} tests")
    fun `all types of comments supported`(@Enum lang: Lang) = mapOf(
        "testSlashComment" to "// This is a first line of comments\n// This is a second line of comments",
        "testMultiLineComment" to when(lang) {
            Lang.JAVA ->   "/*\n     This is a multiline comment\n     */"
            Lang.KOTLIN -> "/*\n    This is a multiline comment\n    */"
        },
        "testJavadocComment" to when(lang) {
            Lang.JAVA ->   "/**\n *     This is Javadoc comment\n */"
            Lang.KOTLIN -> "/**\n    This is Javadoc comment\n    */"
        }).let { testNameToComment -> runTest("Comment", TestFramework.JUNIT4, lang) { classInfo ->
        testNameToComment.map {
            TestMethodInfo(
                name = it.key,
                body = formatBlock("assertEquals(1, 1)", lang),
                comment = it.value,
                displayName = "",
                isParametrised = false,
                isDisabled = false,
                classInfo = classInfo,
                sourceMethod = null
            )
            }
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
        val classInfo = TestClassInfo(className, "project", moduleInfo.projectInfo, moduleInfo, null, lang, framework)

        assertThat(parser.process(listOf(testFile))).usingRecursiveComparison()
            .isEqualTo(expectedResultProvider(classInfo))
    }

    private fun createModule(path: File) =
        ModuleInfo(path.nameWithoutExtension, ProjectInfo("test_project", BuildSystem.GRADLE))

    companion object {
        val frameworkToModule = TestFramework.values().associateWith {
            File("test_project", it.name.replace("_", "").lowercase())
        }
    }
}