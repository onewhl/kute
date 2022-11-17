import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

enum class BuildSystem {
    GRADLE {
        private val moduleNameRegex = "include\\s+['\"]([^']+)['\"]".toRegex()

        override fun getProjectModules(projectPath: File): Map<String, File> {
            val settings = File(projectPath, "settings.gradle").takeIf { it.isFile }
                ?: File(projectPath, "settings.gradle.kts").takeIf { it.isFile }
            return settings?.let {
                extractModuleNames(projectPath, settings, moduleNameRegex, 1)
            } ?: super.getProjectModules(projectPath)
        }
    },
    MAVEN {
        private val moduleNameRegex = "(?<=<module>)[^<]*(?=</module>)".toRegex()

        override fun getProjectModules(projectPath: File): Map<String, File> {
            val pomFile = File(projectPath, "pom.xml")
            return extractModuleNames(projectPath, pomFile, moduleNameRegex, 0)
                ?: super.getProjectModules(projectPath)
        }
    },
    ANT, OTHER;

    open fun getProjectModules(projectPath: File): Map<String, File> =
        mapOf(projectPath.name to projectPath)
            .also { logger.debug { "Found ${it.size} modules in project $projectPath." } }
}

private fun extractModuleNames(
    projectPath: File,
    settingsFile: File,
    regex: Regex,
    expectedGroup: Int
): Map<String, File>? =
    regex.findAll(settingsFile.readText())
        .map { it.groups[expectedGroup]?.value ?: "" }
        .map { it to File(projectPath, it) }
        .toMap()
        .takeIf { it.isNotEmpty() }
        .also { logger.debug { "Found ${it?.size} modules in project $projectPath." } }

fun detectBuildSystem(projectDirPath: File): BuildSystem {
    return when {
        File(projectDirPath, "build.gradle").isFile ||
                File(projectDirPath, "build.gradle.kts").isFile -> BuildSystem.GRADLE

        File(projectDirPath, "pom.xml").isFile -> BuildSystem.MAVEN
        File(projectDirPath, "build.xml").isFile -> BuildSystem.ANT
        else -> BuildSystem.OTHER
    }.also { logger.debug { "Detected build system in $projectDirPath is ${it.name}." } }
}