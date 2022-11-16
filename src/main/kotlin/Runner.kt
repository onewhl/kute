import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import mu.KotlinLogging
import parsers.Lang
import parsers.ParserRunner
import writers.CsvResultWriter
import writers.DBResultWriter
import writers.JsonResultWriter
import writers.OutputType
import java.io.File

class Runner : CliktCommand() {
    private val logger = KotlinLogging.logger {}

    //TODO: add an argument with list of test frameworks to work with
    private val projects by option(help = "Path to file with projects").file(mustExist = true, canBeFile = true)
        .required()
    private val outputFormat by option(help = "Format to store results in. Supported formats: json, database").required()
    private val outputPath by option(help = "Path to output directory").file(canBeFile = true).required()
    private val connection by option(help = "")

    override fun run() {
        getResultWriter()?.use { resultWriter ->
            logger.info { "Start processing projects in ${projects.path}..." }
            projects.forEachLine { path ->
                val buildSystem = detectBuildSystem(path)
                val modules = buildSystem.getProjectModules(path)
                val pathAsFile = File(path)
                val projectInfo = ProjectInfo(pathAsFile.name, buildSystem)

                modules.map {
                    ParserRunner(Lang.values(), pathAsFile, ModuleInfo(it.key, projectInfo), resultWriter)
                }.forEach { it.run() }
            }
            logger.info { "Finished processing projects." }
        }
    }

    private fun getResultWriter() = when (outputFormat) {
        OutputType.JSON.value -> JsonResultWriter(getOutputFile().toPath())
        OutputType.CSV.value -> CsvResultWriter(getOutputFile().toPath())
        OutputType.DATABASE.value -> {
            if (connection == null) {
                logger.error { "Connection is not defined. Please, provide --connection option with a value." }
                null
            } else {
                DBResultWriter(connection!!)
            }
        }

        else -> null
    }

    private fun getOutputFile(): File {
        val outputFile = if (outputPath.isDirectory) {
            File(outputPath, "results.${outputFormat}")
        } else {
            outputPath
        }
        return outputFile
    }
}

fun main(args: Array<String>) = Runner().main(args)