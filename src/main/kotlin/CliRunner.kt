import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.options.unique
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import mu.KotlinLogging
import writers.OutputType
import java.io.File
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue

private val logger = KotlinLogging.logger {}

class CliRunner: CliktCommand() {
    private val projects by option(help = "Path to file with projects").file(mustExist = true, canBeFile = true)
        .required()
    private val outputFormats by option(help = "Comma-separated list of formats to store results in. Supported formats: csv, json, sqlite")
        .choice(OutputType.values().associateBy { it.value }).split(",").required().unique()
    private val outputPath by option(help = "Path to output directory").file(canBeFile = true).required()
    private val repoStorage by option(help = "Path to the directory to clone repositories to").file(canBeFile = false)
        .default(File("repos"))
    private val ioThreads by option(help = "Number of threads used for downloading Git repos. Use 0 for common pool").int()
        .default(1)
    private val cpuThreads by option(help = "Number of threads used for processing projects. Use 0 for common pool").int()
        .default(0)
    private val cleanup by option(help = "Delete cloned Git repos after processing").flag(default = false)

    override fun run() = ResultWriter.create(outputFormats, outputPath).use { resultWriter ->
        logger.info { "Start processing projects in ${projects.path}..." }

        val scanner = ProjectScanner(repoStorage, ioThreads, cpuThreads, cleanup)
        val tasks = LinkedBlockingQueue<Future<List<TestMethodInfo>>>()
        val projectCount = projects.bufferedReader().useLines { it.count { path -> scanner.scanProject(path, tasks) } }

        repeat(projectCount) {
            tasks.take().valueOrNull()?.let { resultWriter.writeSynchronized(it) }
        }

        logger.info { "Finished processing projects." }
    }
}

fun main(args: Array<String>) = CliRunner().main(args)