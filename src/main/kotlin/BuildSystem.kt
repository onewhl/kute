import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

enum class BuildSystem {
    GRADLE {
        private val moduleImportsRegex = "include\\s*[(]?((?:\\s*['\"][^'\"]+['\"]\\s*,?)+)".toRegex()
        private val extractModuleNamesRegex = "(?<=')[^',\\s]+|(?<=\")[^\",\\s]+".toRegex()

        private fun findConfigFile(projectPath: File, name:String) =
            File(projectPath, "$name.gradle").takeIf { it.isFile }
                ?: File(projectPath, "$name.gradle.kts").takeIf { it.isFile }

        override fun getProjectModules(projectPath: File): Map<String, File> =
            findConfigFile(projectPath, "settings")?.let { settings ->
                extractModuleNames(projectPath, settings, moduleImportsRegex) { matchResult ->
                    extractModuleNamesRegex.findAll(matchResult.groupValues[1]).map { it.value }
                }
            } ?: super.getProjectModules(projectPath)

        override fun supportsTestDirFiltering(path: File): Boolean =
            findConfigFile(path, "build")?.let { !it.readText().contains("sourceSets") }
                ?: true
    },
    MAVEN {
        private val moduleNameRegex = "(?<=<module>)[^<]*(?=</module>)".toRegex()

        override fun getProjectModules(projectPath: File): Map<String, File> {
            val pomFile = File(projectPath, "pom.xml")
            return extractModuleNames(projectPath, pomFile, moduleNameRegex) { sequenceOf(it.value) }
                ?: super.getProjectModules(projectPath)
        }
    },
    ANT, OTHER;

    open fun supportsTestDirFiltering(path: File) = this == MAVEN

    open fun getProjectModules(projectPath: File): Map<String, File> =
        mapOf(projectPath.name to projectPath)
            .also { logger.debug { "Found ${it.size} modules in project $projectPath." } }
}

private fun extractModuleNames(
    projectPath: File,
    settingsFile: File,
    regex: Regex,
    extractor: (MatchResult) -> Sequence<String>
): Map<String, File>? =
    regex.findAll(settingsFile.readText())
        .flatMap(extractor)
        .map { it to File(projectPath, it.replace(':', '/')) }
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