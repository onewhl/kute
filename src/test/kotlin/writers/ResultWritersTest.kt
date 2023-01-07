package writers

import BuildSystem
import ModuleInfo
import ProjectInfo
import SourceClassInfo
import SourceMethodInfo
import TestClassInfo
import TestFramework
import TestMethodInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import parsers.Lang
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import kotlin.io.path.absolutePathString
import kotlin.io.path.readText

class ResultWritersTest {
    private fun provideTestMethods(): List<TestMethodInfo> {
        val projectInfo = ProjectInfo("My Project", BuildSystem.MAVEN)
        val moduleInfo = ModuleInfo("main", projectInfo)
        val sourceClass = SourceClassInfo("Simple", "com.test", moduleInfo, Lang.JAVA)
        val testClassInfo =
            TestClassInfo("SimpleTest", "com.test", projectInfo, moduleInfo, Lang.JAVA, TestFramework.JUNIT4, sourceClass)
        val sourceMethod = SourceMethodInfo("run", "", sourceClass)
        val testMethodInfo = TestMethodInfo(
            name = "test",
            body = "assertTrue(true);",
            comment = "",
            displayName = "Simple Test Method",
            isParametrised = false,
            isDisabled = false,
            classInfo = testClassInfo,
            sourceMethod = sourceMethod
        )
        val testMethodInfoNew =
            testMethodInfo.copy(
                name = "testNoSourceMethod",
                displayName = "Simple Test Method 2",
                body = "assertEquals(\"true\", value);",
                sourceMethod = null,
                isParametrised = true,
                isDisabled = true
            )
        val testMethodInfoNoSourceClass =
            testMethodInfo.copy(
                name = "testNoSourceClass",
                displayName = "",
                body = "assertEquals(\"true\", value);",
                classInfo = testClassInfo.copy(name = "NoSourceSimpleTest", sourceClass = null),
                sourceMethod = null,
                isParametrised = false,
                isDisabled = false
            )
        return listOf(testMethodInfo, testMethodInfoNew, testMethodInfoNoSourceClass)
    }

    @Test
    fun testCsvResult(@TempDir tmpDir: Path) {
        tmpDir.resolve("results.csv").let { results ->
            CsvResultWriter(results).use { writer ->
                writer.writeTestMethods(provideTestMethods())
            }
            val expectedContent = """
                name,body,comment,displayName,isParametrised,isDisabled,classInfo.name,classInfo.package,classInfo.projectInfo.name,classInfo.projectInfo.buildSystem,classInfo.moduleInfo.name,classInfo.moduleInfo.projectInfo.name,classInfo.moduleInfo.projectInfo.buildSystem,classInfo.language,classInfo.testFramework,classInfo.sourceClass.name,classInfo.sourceClass.package,classInfo.sourceClass.moduleInfo.name,classInfo.sourceClass.moduleInfo.projectInfo.name,classInfo.sourceClass.moduleInfo.projectInfo.buildSystem,classInfo.sourceClass.language,sourceMethod.name,sourceMethod.body,sourceMethod.sourceClass.name,sourceMethod.sourceClass.package,sourceMethod.sourceClass.moduleInfo.name,sourceMethod.sourceClass.moduleInfo.projectInfo.name,sourceMethod.sourceClass.moduleInfo.projectInfo.buildSystem,sourceMethod.sourceClass.language
                test,assertTrue(true);,,Simple Test Method,false,false,SimpleTest,com.test,My Project,MAVEN,main,My Project,MAVEN,JAVA,JUNIT4,Simple,com.test,main,My Project,MAVEN,JAVA,run,,Simple,com.test,main,My Project,MAVEN,JAVA
                testNoSourceMethod,"assertEquals(""true"", value);",,Simple Test Method 2,true,true,SimpleTest,com.test,My Project,MAVEN,main,My Project,MAVEN,JAVA,JUNIT4,Simple,com.test,main,My Project,MAVEN,JAVA,,,,,,,,
                testNoSourceClass,"assertEquals(""true"", value);",,,false,false,NoSourceSimpleTest,com.test,My Project,MAVEN,main,My Project,MAVEN,JAVA,JUNIT4,,,,,,,,,,,,,,
            """.trimIndent()
            assertEquals(expectedContent, results.readText().replace("\r\n", "\n"))
        }
    }

    @Test
    fun testJsonResult(@TempDir tmpDir: Path) {
        tmpDir.resolve("results.json").let { results ->
            JsonResultWriter(results).use { writer ->
                writer.writeTestMethods(provideTestMethods())
            }
            val expectedContent = """
               [
               {"name":"test","body":"assertTrue(true);","comment":"","displayName":"Simple Test Method","isParametrised":false,"isDisabled":false,"classInfo":{"name":"SimpleTest","package":"com.test","projectInfo":{"name":"My Project","buildSystem":"MAVEN"},"moduleInfo":{"name":"main","projectInfo":{"name":"My Project","buildSystem":"MAVEN"}},"language":"JAVA","testFramework":"JUNIT4","sourceClass":{"name":"Simple","package":"com.test","moduleInfo":{"name":"main","projectInfo":{"name":"My Project","buildSystem":"MAVEN"}},"language":"JAVA"}},"sourceMethod":{"name":"run","body":"","sourceClass":{"name":"Simple","package":"com.test","moduleInfo":{"name":"main","projectInfo":{"name":"My Project","buildSystem":"MAVEN"}},"language":"JAVA"}}},
               {"name":"testNoSourceMethod","body":"assertEquals(\"true\", value);","comment":"","displayName":"Simple Test Method 2","isParametrised":true,"isDisabled":true,"classInfo":{"name":"SimpleTest","package":"com.test","projectInfo":{"name":"My Project","buildSystem":"MAVEN"},"moduleInfo":{"name":"main","projectInfo":{"name":"My Project","buildSystem":"MAVEN"}},"language":"JAVA","testFramework":"JUNIT4","sourceClass":{"name":"Simple","package":"com.test","moduleInfo":{"name":"main","projectInfo":{"name":"My Project","buildSystem":"MAVEN"}},"language":"JAVA"}},"sourceMethod":null},
               {"name":"testNoSourceClass","body":"assertEquals(\"true\", value);","comment":"","displayName":"","isParametrised":false,"isDisabled":false,"classInfo":{"name":"NoSourceSimpleTest","package":"com.test","projectInfo":{"name":"My Project","buildSystem":"MAVEN"},"moduleInfo":{"name":"main","projectInfo":{"name":"My Project","buildSystem":"MAVEN"}},"language":"JAVA","testFramework":"JUNIT4","sourceClass":null},"sourceMethod":null}
               ]
            """.trimIndent()
            assertEquals(expectedContent, results.readText().replace("\r\n", "\n"))
        }
    }

    @Test
    fun testDBResult(@TempDir tmpDir: Path) {
        val connectionString = "jdbc:sqlite:${tmpDir.absolutePathString()}/test.db"
        DBResultWriter(connectionString).use { writer ->
            writer.writeTestMethods(provideTestMethods())
        }
        DriverManager.getConnection(connectionString).let { connection ->
            checkDatabaseTableContent(
                connection, "Projects", listOf(
                    mapOf("id" to 1, "name" to "My Project", "build_system" to "MAVEN")
                )
            )
            checkDatabaseTableContent(
                connection, "Modules", listOf(
                    mapOf("id" to 1, "name" to "main", "project" to 1)
                )
            )
            checkDatabaseTableContent(
                connection, "TestClasses", listOf(
                    mapOf(
                        "id" to 1,
                        "name" to "SimpleTest",
                        "project" to 1,
                        "module" to 1,
                        "source_class" to 1,
                        "package" to "com.test",
                        "language" to "JAVA",
                        "test_framework" to "JUNIT4"
                    ),
                    mapOf(
                        "id" to 2,
                        "name" to "NoSourceSimpleTest",
                        "project" to 1,
                        "module" to 1,
                        "source_class" to null,
                        "package" to "com.test",
                        "language" to "JAVA",
                        "test_framework" to "JUNIT4"
                    )
                )
            )
            checkDatabaseTableContent(
                connection, "SourceClasses", listOf(
                    mapOf("id" to 1, "name" to "Simple", "module" to 1, "package" to "com.test", "language" to "JAVA")
                )
            )
            checkDatabaseTableContent(
                connection, "SourceMethods", listOf(
                    mapOf("id" to 1, "name" to "run", "body" to "", "source_class" to 1)
                )
            )
            checkDatabaseTableContent(
                connection, "TestMethods", listOf(
                    mapOf(
                        "id" to 1,
                        "name" to "test",
                        "body" to "assertTrue(true);",
                        "comment" to "",
                        "display_name" to "Simple Test Method",
                        "is_parametrised" to 0,
                        "is_disabled" to 0,
                        "test_class" to 1,
                        "source_method" to 1
                    ),
                    mapOf(
                        "id" to 2,
                        "name" to "testNoSourceMethod",
                        "body" to "assertEquals(\"true\", value);",
                        "comment" to "",
                        "display_name" to "Simple Test Method 2",
                        "is_parametrised" to 1,
                        "is_disabled" to 1,
                        "test_class" to 1,
                        "source_method" to null
                    ),
                    mapOf(
                        "id" to 3,
                        "name" to "testNoSourceClass",
                        "body" to "assertEquals(\"true\", value);",
                        "comment" to "",
                        "display_name" to "",
                        "is_parametrised" to 0,
                        "is_disabled" to 0,
                        "test_class" to 2,
                        "source_method" to null
                    )
                )
            )
        }
    }

    private fun checkDatabaseTableContent(
        conn: Connection,
        tableName: String,
        expectedResult: List<Map<String, Any?>>
    ) {
        assertEquals(expectedResult, fetchDbTable(conn, tableName).toMaps(), "Comparing $tableName")
    }

    private data class TableContent(val columnNames: List<String>, val rows: List<List<Any?>>) {
        fun toMaps(): List<Map<String, Any?>> = rows.map {
            it.mapIndexed { index, value ->
                columnNames[index] to value
            }.toMap()
        }
    }

    private fun collectResultSet(rs: ResultSet): TableContent {
        val meta = rs.metaData
        val columnNames = (1..meta.columnCount).map { meta.getColumnLabel(it) }
        val rows = generateSequence {
            if (rs.next()) (1..meta.columnCount).map { rs.getObject(it) } else null
        }.toList()
        return TableContent(columnNames, rows)
    }

    private fun fetchDbTable(conn: Connection, tableName: String): TableContent = conn.createStatement().use { stmt ->
        stmt.executeQuery("SELECT * FROM $tableName").use { collectResultSet(it) }
    }
}