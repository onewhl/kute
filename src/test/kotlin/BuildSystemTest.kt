import com.google.common.io.Files
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class BuildSystemTest {
    @Test
    fun testDetectGradleModules(@TempDir tempDir: File) {
        val projectPath = File(tempDir, "test").also { it.mkdir() }
        val settings = """
            rootProject.name = "prj-root"

            include "project:projectj"
            include "project:core"
            include(":project:server")
            include 'project:prj1', 'project:prj2'
            include("project:prj3", 
                "project:prj4"
            )
        """.trimIndent()
        File(projectPath, "settings.gradle").writeText(settings)
        val resolvedModules = BuildSystem.GRADLE.getProjectModules(projectPath)
        val expectedModules = mapOf(
            "test/project/projectj" to File(projectPath, "project/projectj"),
            "test/project/core" to File(projectPath, "project/core"),
            "test/project/server" to File(projectPath, "project/server"),
            "test/project/prj1" to File(projectPath, "project/prj1"),
            "test/project/prj2" to File(projectPath, "project/prj2"),
            "test/project/prj3" to File(projectPath, "project/prj3"),
            "test/project/prj4" to File(projectPath, "project/prj4"),
        )
        assertThat(resolvedModules).containsExactlyEntriesOf(expectedModules)
    }

    private fun createPom(name: String, modules: List<String>): String {
        val modulesStr = if (modules.isEmpty()) "" else """
              <modules>
                <module>${modules.joinToString("</module><module>")}</module>
              </modules>
        """
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
              <modelVersion>4.0.0</modelVersion>
            
              <artifactId>$name</artifactId>
              <packaging>pom</packaging>
            
              <name>$name</name>
              <url>https://example.org</url>
              <description>Test Java project</description>
            
              $modulesStr
            </project>
        """.trimIndent()
    }

    @Test
    fun testDetectMavenModules(@TempDir tempDir: File) {
        val projectPath = File(tempDir, "test-parent").also { it.mkdir() }
        val pomXml = createPom("test-parent", listOf("android", "maven-plugin"))
        File(projectPath, "pom.xml").writeText(pomXml)
        val resolvedModules = BuildSystem.MAVEN.getProjectModules(projectPath)
        val expectedModules = mapOf(
            "test-parent/android" to File(projectPath, "android"),
            "test-parent/maven-plugin" to File(projectPath, "maven-plugin"),
        )
        assertThat(resolvedModules).containsExactlyEntriesOf(expectedModules)
    }

    @Test
    fun testDetectMavenModulesWithSeveralParents(@TempDir tempDir: File) {
        val projectPath = File(tempDir, "grandparent").also { it.mkdir() }
        val grandParentPom = createPom("grandparent", listOf("parent", "shared"))
        val parentPom = createPom("parent", listOf("child1", "child2"))
        File(projectPath, "pom.xml").writeText(grandParentPom)
        File(projectPath, "parent/pom.xml").apply {
            Files.createParentDirs(this)
            writeText(parentPom)
        }
        val resolvedModules = BuildSystem.MAVEN.getProjectModules(projectPath)
        val expectedModules = mapOf(
            "grandparent/parent/child1" to File(File(projectPath, "parent"), "child1"),
            "grandparent/parent/child2" to File(File(projectPath, "parent"), "child2"),
            "grandparent/shared" to File(projectPath, "shared"),
        )
        assertThat(resolvedModules).containsExactlyEntriesOf(expectedModules)
    }
}