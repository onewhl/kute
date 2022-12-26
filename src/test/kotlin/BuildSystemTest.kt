import org.junit.jupiter.api.Assertions.assertEquals
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
        """.trimIndent()
        File(projectPath, "settings.gradle").writeText(settings)
        val resolvedModules = BuildSystem.GRADLE.getProjectModules(projectPath)
        val expectedModules = mapOf(
            "project:projectj" to File(projectPath, "project/projectj"),
            "project:core" to File(projectPath, "project/core"),
            ":project:server" to File(projectPath, "project/server"),
            )
        assertEquals(expectedModules, resolvedModules)
    }
}