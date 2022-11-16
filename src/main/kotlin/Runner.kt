import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import mu.KotlinLogging
import org.eclipse.jgit.api.Git
import parsers.Lang
import parsers.ParserRunner
import writers.CsvResultWriter
import writers.DBResultWriter
import writers.JsonResultWriter
import writers.OutputType
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executor

private val logger = KotlinLogging.logger {}
class Runner : CliktCommand() {
    private val logger = KotlinLogging.logger {}

    //TODO: add an argument with list of test frameworks to work with
    private val projects by option(help = "Path to file with projects").file(mustExist = true, canBeFile = true)
        .required()
    private val outputFormat by option(help = "Format to store results in. Supported formats: json, database").required()
    private val outputPath by option(help = "Path to output directory").file(canBeFile = true).required()
    private val repoStorage by option(help = "Path to cloned git repos directory").file(canBeFile = false)
        .default(File("repos"))
    private val connection by option(help = "")

    override fun run() {
        getResultWriter()?.use { resultWriter ->
            val ioExecutor = Executor { command -> command.run() }
            val computeExecutor = Executor { command -> command.run() }
            logger.info { "Start processing projects in ${projects.path}..." }
            projects.forEachLine { path ->
                if (path.startsWith("http")) {
                    ioExecutor.execute {
                        val repoPath = GitRepoDownloadingTask(path, repoStorage).call()
                        computeExecutor.execute(
                            ProjectProcessingTask(repoPath, resultWriter)
                        )
                    }
                } else {
                    computeExecutor.execute(
                        ProjectProcessingTask(File(path), resultWriter)
                    )
                }
            }
            logger.info { "Finished processing projects." }
        }
    }


    private class GitRepoDownloadingTask(private val url: String, private val targetDir: File) : Callable<File> {
        override fun call(): File {
            val destination = File(targetDir, url.substringAfterLast('/').removeSuffix(".git"))
            if (destination.isDirectory) {
                logger.warn("$destination directory already exists and will be cleaned")
                destination.deleteRecursively()
            }
            logger.info("Cloning Git repository $url to $destination")
            val git = Git.cloneRepository()
                .setURI(url)
                .setDirectory(destination)
                .call()
            val directory = git.repository.workTree
            git.close()
            return directory
        }
    }

    private class ProjectProcessingTask(
        private val path: File,
        private val writer: ResultWriter
    ) : Runnable {
        override fun run() {
            val buildSystem = detectBuildSystem(path)
            val projectInfo = ProjectInfo(path.name, buildSystem)
            buildSystem.getProjectModules(path).forEach { (moduleName, modulePath) ->
                ParserRunner(Lang.values(), modulePath, ModuleInfo(moduleName, projectInfo), writer)
            }
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