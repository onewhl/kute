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
import parsers.Lang
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import kotlin.io.path.absolutePathString
import kotlin.io.path.readText

class ResultWritersTest {
    private fun provideTestMethods(): List<TestMethodInfo> {
        val projectInfo = ProjectInfo("My Project", BuildSystem.MAVEN, id = 1)
        val moduleInfo = ModuleInfo("main", projectInfo, id = 5)
        val sourceClass = SourceClassInfo("Simple", "com.test", moduleInfo, Lang.JAVA, id = 10)
        val testClassInfo =
            TestClassInfo("SimpleTest", "com.test", projectInfo, moduleInfo, sourceClass, Lang.JAVA, id = 30)
        val sourceMethod = SourceMethodInfo("run", "", sourceClass, id = 50)
        val testMethodInfo = TestMethodInfo(
            name = "test",
            body = "assertTrue(true);",
            comment = "",
            displayName = "Simple Test Method",
            isParametrised = false,
            classInfo = testClassInfo,
            sourceMethod = sourceMethod,
            id = 101
        )
        val testMethodInfoNew =
            testMethodInfo.copy(
                name = "testNew",
                displayName = "Simple Test Method 2",
                body = "assertEquals(\"true\", value);",
                id = 102,
                sourceMethod = null
            )
        return listOf(testMethodInfo, testMethodInfoNew)
    }

    @Test
    fun testCsvResult(@TempDir tmpDir: Path) {
        tmpDir.resolve("results.csv").let { results ->
            CsvResultWriter(results).use { writer ->
                writer.writeTestMethods(provideTestMethods())
            }
            val expectedContent = """
                name,body,comment,displayName,isParametrised,classInfo.name,classInfo.package,classInfo.projectInfo.name,classInfo.projectInfo.buildSystem,classInfo.projectInfo.id,classInfo.moduleInfo.name,classInfo.moduleInfo.projectInfo.name,classInfo.moduleInfo.projectInfo.buildSystem,classInfo.moduleInfo.projectInfo.id,classInfo.moduleInfo.id,classInfo.sourceClass.name,classInfo.sourceClass.package,classInfo.sourceClass.moduleInfo.name,classInfo.sourceClass.moduleInfo.projectInfo.name,classInfo.sourceClass.moduleInfo.projectInfo.buildSystem,classInfo.sourceClass.moduleInfo.projectInfo.id,classInfo.sourceClass.moduleInfo.id,classInfo.sourceClass.language,classInfo.sourceClass.id,classInfo.language,classInfo.id,sourceMethod.name,sourceMethod.body,sourceMethod.sourceClass.name,sourceMethod.sourceClass.package,sourceMethod.sourceClass.moduleInfo.name,sourceMethod.sourceClass.moduleInfo.projectInfo.name,sourceMethod.sourceClass.moduleInfo.projectInfo.buildSystem,sourceMethod.sourceClass.moduleInfo.projectInfo.id,sourceMethod.sourceClass.moduleInfo.id,sourceMethod.sourceClass.language,sourceMethod.sourceClass.id,sourceMethod.id,id
                test,assertTrue(true);,,Simple Test Method,false,SimpleTest,com.test,My Project,MAVEN,1,main,My Project,MAVEN,1,5,Simple,com.test,main,My Project,MAVEN,1,5,JAVA,10,JAVA,30,run,,Simple,com.test,main,My Project,MAVEN,1,5,JAVA,10,50,101
                testNew,"assertEquals(""true"", value);",,Simple Test Method 2,false,SimpleTest,com.test,My Project,MAVEN,1,main,My Project,MAVEN,1,5,Simple,com.test,main,My Project,MAVEN,1,5,JAVA,10,JAVA,30,,102
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
                {"name":"test","body":"assertTrue(true);","comment":"","displayName":"Simple Test Method","isParametrised":false,"classInfo":{"name":"SimpleTest","package":"com.test","projectInfo":{"name":"My Project","buildSystem":"MAVEN"},"moduleInfo":{"name":"main","projectInfo":{"name":"My Project","buildSystem":"MAVEN","id":1},"id":5},"sourceClass":{"name":"Simple","package":"com.test","moduleInfo":{"name":"main","projectInfo":{"name":"My Project","buildSystem":"MAVEN","id":1},"id":5},"language":"JAVA","id":10},"language":"JAVA","id":30},"sourceMethod":{"name":"run","body":"","sourceClass":{"name":"Simple","package":"com.test","moduleInfo":{"name":"main","projectInfo":{"name":"My Project","buildSystem":"MAVEN","id":1},"id":5},"language":"JAVA","id":10},"id":50},"id":101},
                {"name":"testNew","body":"assertEquals(\"true\", value);","comment":"","displayName":"Simple Test Method 2","isParametrised":false,"classInfo":{"name":"SimpleTest","package":"com.test","projectInfo":{"name":"My Project","buildSystem":"MAVEN","id":1},"moduleInfo":{"name":"main","projectInfo":{"name":"My Project","buildSystem":"MAVEN","id":1},"id":5},"sourceClass":{"name":"Simple","package":"com.test","moduleInfo":{"name":"main","projectInfo":{"name":"My Project","buildSystem":"MAVEN","id":1}},"language":"JAVA","id":10},"language":"JAVA","id":30},"sourceMethod":null,"id":102}
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
                    mapOf("id" to 5, "name" to "main", "project" to 1)
                )
            )
            checkDatabaseTableContent(
                connection, "TestClasses", listOf(
                    mapOf(
                        "id" to 30,
                        "name" to "SimpleTest",
                        "project" to 1,
                        "module" to 5,
                        "source_class" to 10,
                        "package" to "com.test",
                        "language" to "JAVA"
                    )
                )
            )
            checkDatabaseTableContent(
                connection, "SourceClasses", listOf(
                    mapOf("id" to 10, "name" to "Simple", "module" to 5, "package" to "com.test", "language" to "JAVA")
                )
            )
            checkDatabaseTableContent(
                connection, "SourceMethods", listOf(
                    mapOf("id" to 50, "name" to "run", "body" to "", "source_class" to 10)
                )
            )
            checkDatabaseTableContent(
                connection, "TestMethods", listOf(
                    mapOf(
                        "id" to 101,
                        "name" to "test",
                        "body" to "assertTrue(true);",
                        "comment" to "",
                        "display_name" to "Simple Test Method",
                        "is_parametrised" to 0,
                        "test_class" to 30,
                        "source_method" to 50
                    ),
                    mapOf(
                        "id" to 102,
                        "name" to "testNew",
                        "body" to "assertEquals(\"true\", value);",
                        "comment" to "",
                        "display_name" to "Simple Test Method 2",
                        "is_parametrised" to 0,
                        "test_class" to 30,
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