import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import mu.KotlinLogging
import java.io.File

class Runner : CliktCommand() {
    private val logger = KotlinLogging.logger {}

    //TODO: add an argument with list of test frameworks to work with
    private val projects by option(help = "Path to file with projects").file(mustExist = true, canBeFile = true)
        .required()
    private val output by option(help = "Path to output directory").file(canBeFile = true).required()

    override fun run() {
        projects.forEachLine { path ->
            val buildSystem = detectBuildSystem(path)
            val modules = buildSystem.getProjectModules(path)
            val pathAsFile = File(path)
            val projectInfo = ProjectInfo(pathAsFile.name, buildSystem)

            modules.map {
                TestExtractor(pathAsFile, ModuleInfo(it.key, projectInfo))
            }.forEach { it.run() }
        }
    }
}

fun main(args: Array<String>) = Runner().main(args)