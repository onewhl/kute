import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class BuildSystemTest {
    @Test
    fun testDetectGradleModules(@TempDir projectPath: File) {
        val settings = """
            rootProject.name = "solr-root"

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
            "project:projectj" to File(projectPath, "project/projectj"),
            "project:core" to File(projectPath, "project/core"),
            ":project:server" to File(projectPath, "project/server"),
            "project:prj1" to File(projectPath, "project/prj1"),
            "project:prj2" to File(projectPath, "project/prj2"),
            "project:prj3" to File(projectPath, "project/prj3"),
            "project:prj4" to File(projectPath, "project/prj4"),
            )
        assertThat(resolvedModules).containsExactlyEntriesOf(expectedModules)
    }

    @Test
    fun testDetectMavenModules(@TempDir projectPath: File) {
        val pomXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
              <modelVersion>4.0.0</modelVersion>
            
              <artifactId>test-parent</artifactId>
              <packaging>pom</packaging>
            
              <name>Test Project</name>
              <url>https://example.org</url>
              <description>Test Java project</description>
            
              <properties>
                <slf4j.version>1.7.36</slf4j.version>
              </properties>
            
              <modules>
                <module>android</module>
                <module>maven-plugin</module>
              </modules>
            </project>
        """.trimIndent()
        File(projectPath, "pom.xml").writeText(pomXml)
        val resolvedModules = BuildSystem.MAVEN.getProjectModules(projectPath)
        val expectedModules = mapOf(
            "android" to File(projectPath, "android"),
            "maven-plugin" to File(projectPath, "maven-plugin"),
        )
        assertThat(resolvedModules).containsExactlyEntriesOf(expectedModules)
    }
}