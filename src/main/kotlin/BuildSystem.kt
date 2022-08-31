import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

enum class BuildSystem {
    GRADLE {
        override fun getProjectModules(projectPath: String): Map<String, File> {
            val file = File(projectPath, "settings.gradle")
            return file.takeIf { file.isFile }.let {
                "include\\s+'([^']+)'".toRegex()
                    .findAll(file.readText())
                    .map { it.groups[1]?.value ?: "" }
                    .map { it to File(projectPath, it) }
                    .toMap()
                    .takeIf { it.isNotEmpty() }
                    .also { logger.debug { "Found ${it?.size} modules in project $projectPath." } }
                    ?: super.getProjectModules(projectPath)
            }
        }
    },
    MAVEN {
        override fun getProjectModules(projectPath: String): Map<String, File> {
            val file = File(projectPath, "pom.xml")
            val regex = "(?<=<module>)[^<]*(?=</module>)".toRegex()
            return regex.findAll(file.readText())
                .map { it.groups[1]?.value ?: "" }
                .map { it to File(projectPath, it) }
                .toMap()
                .takeIf { it.isNotEmpty() }
                .also { logger.debug { "Found ${it?.size} modules in project $projectPath." } }
                ?: super.getProjectModules(projectPath)
        }
    },
    ANT, OTHER;

    open fun getProjectModules(projectPath: String): Map<String, File> =
        File(projectPath).let { mapOf(it.name to it) }
            .also { logger.debug { "Found ${it.size} modules in project $projectPath." } }

    open fun getTestFilesRoot(projectPath: File) = projectPath
}

fun detectBuildSystem(projectDirPath: String): BuildSystem {
    return when {
        File(projectDirPath, "build.gradle").isFile ||
                File(projectDirPath, "build.gradle.kts").isFile -> {
            BuildSystem.GRADLE
        }
        File(projectDirPath, "pom.xml").isFile -> {
            BuildSystem.MAVEN
        }
        else -> BuildSystem.OTHER
    }.also { logger.debug { "Detected build system in $projectDirPath is ${it.name}." } }
}