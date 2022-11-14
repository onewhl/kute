package writers

import BuildSystem
import ModuleInfo
import ProjectInfo
import SourceClassInfo
import SourceMethodInfo
import TestClassInfo
import TestMethodInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText

class ResultWritersTest {
    private fun provideTestMethods(): List<TestMethodInfo> {
        val projectInfo = ProjectInfo("My Project", BuildSystem.MAVEN, id = 1L)
        val moduleInfo = ModuleInfo("main", projectInfo, id = 1L)
        val sourceClass = SourceClassInfo("Simple", moduleInfo, id = 1L)
        val testClassInfo = TestClassInfo("SimpleTest", projectInfo, moduleInfo, null, id = 1L)
        val sourceMethod = SourceMethodInfo("run", "", sourceClass, id = 1)
        val testMethodInfo = TestMethodInfo(
            name = "test",
            body = "assertTrue(true);",
            comment = "",
            displayName = "Simple Test Method",
            isParametrised = false,
            classInfo = testClassInfo,
            sourceMethod = sourceMethod,
            id = 1L
        )
        val testMethodInfoNew =
            testMethodInfo.copy(name = "testNew", body = "assertFalse(false);", id = 2L, sourceMethod = null)
        return listOf(testMethodInfo, testMethodInfoNew)
    }

    @Test
    fun testCsvResult(@TempDir tmpDir: Path) {
        tmpDir.resolve("results.csv").let { results ->
            CsvResultWriter(results).use { writer ->
                provideTestMethods().forEach { writer.writeTestMethod(it) }
            }
            val expectedContent = """
                name,body,comment,displayName,isParametrised,classInfo.name,classInfo.projectInfo.name,classInfo.projectInfo.buildSystem,classInfo.projectInfo.id,classInfo.moduleInfo.name,classInfo.moduleInfo.projectInfo.name,classInfo.moduleInfo.projectInfo.buildSystem,classInfo.moduleInfo.projectInfo.id,classInfo.moduleInfo.id,classInfo.sourceClass.name,classInfo.sourceClass.moduleInfo.name,classInfo.sourceClass.moduleInfo.projectInfo.name,classInfo.sourceClass.moduleInfo.projectInfo.buildSystem,classInfo.sourceClass.moduleInfo.projectInfo.id,classInfo.sourceClass.moduleInfo.id,classInfo.sourceClass.id,classInfo.id,sourceMethod.name,sourceMethod.body,sourceMethod.sourceClass.name,sourceMethod.sourceClass.moduleInfo.name,sourceMethod.sourceClass.moduleInfo.projectInfo.name,sourceMethod.sourceClass.moduleInfo.projectInfo.buildSystem,sourceMethod.sourceClass.moduleInfo.projectInfo.id,sourceMethod.sourceClass.moduleInfo.id,sourceMethod.sourceClass.id,sourceMethod.id,id
                test,assertTrue(true);,,Simple Test Method,false,SimpleTest,My Project,MAVEN,1,main,My Project,MAVEN,1,1,,1,run,,Simple,main,My Project,MAVEN,1,1,1,1,1
                testNew,assertFalse(false);,,Simple Test Method,false,SimpleTest,My Project,MAVEN,1,main,My Project,MAVEN,1,1,,1,,2
            """.trimIndent()
            assertEquals(expectedContent, results.readText())
        }
    }

    @Test
    fun testJsonResult(@TempDir tmpDir: Path) {
        tmpDir.resolve("results.json").let { results ->
            JsonResultWriter(results).use { writer ->
                provideTestMethods().forEach { writer.writeTestMethod(it) }
            }
            val expectedContent = """
                [{"name":"test","body":"assertTrue(true);","comment":"","displayName":"Simple Test Method","isParametrised":false,"classInfo":{"name":"SimpleTest","projectInfo":{"name":"My Project","buildSystem":"MAVEN"},"moduleInfo":{"name":"main","projectInfo":{"name":"My Project","buildSystem":"MAVEN","id":1}},"sourceClass":null},"sourceMethod":{"name":"run","body":"","sourceClass":{"name":"Simple","moduleInfo":{"name":"main","projectInfo":{"name":"My Project","buildSystem":"MAVEN","id":1},"id":1}}}},{"name":"testNew","body":"assertFalse(false);","comment":"","displayName":"Simple Test Method","isParametrised":false,"classInfo":{"name":"SimpleTest","projectInfo":{"name":"My Project","buildSystem":"MAVEN","id":1},"moduleInfo":{"name":"main","projectInfo":{"name":"My Project","buildSystem":"MAVEN","id":1},"id":1},"sourceClass":null,"id":1},"sourceMethod":null}]
            """.trimIndent()
            assertEquals(expectedContent, results.readText())
        }
    }
}