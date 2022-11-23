import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import writers.OutputType
import java.io.File

class CliRunner: CliktCommand() {
    private val projects by option(help = "Path to file with projects").file(mustExist = true, canBeFile = true)
        .required()
    private val outputFormat by option(help = "Format to store results in. Supported formats: csv, json, sqlite")
        .choice(OutputType.values().associateBy { it.value }).required()
    private val outputPath by option(help = "Path to output directory").file(canBeFile = true).required()
    private val repoStorage by option(help = "Path to the directory to clone repositories to").file(canBeFile = false)
        .default(File("repos"))
    private val ioThreads by option(help = "Number of threads used for downloading Git repos. Use 0 for common pool").int()
        .default(1)
    private val cpuThreads by option(help = "Number of threads used for processing projects. Use 0 for common pool").int()
        .default(0)

    override fun run() = Runner(projects, outputFormat, outputPath, repoStorage, ioThreads, cpuThreads).run()
}

fun main(args: Array<String>) = CliRunner().main(args)