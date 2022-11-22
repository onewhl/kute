import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import mu.KotlinLogging
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.TextProgressMonitor
import parsers.Lang
import parsers.ParserRunner
import writers.CsvResultWriter
import writers.DBResultWriter
import writers.JsonResultWriter
import writers.OutputType
import java.io.File
import java.io.Writer
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

class Runner : CliktCommand() {
    private val projects by option(help = "Path to file with projects").file(mustExist = true, canBeFile = true)
        .required()
    private val outputFormat by option(help = "Format to store results in. Supported formats: csv, json, sqlite").required()
    private val outputPath by option(help = "Path to output directory").file(canBeFile = true).required()
    private val repoStorage by option(help = "Path to the directory to clone repositories to").file(canBeFile = false)
        .default(File("repos"))
    private val ioThreads by option(help = "Number of threads used for downloading Git repos. Use 0 for common pool").int()
        .default(1)
    private val cpuThreads by option(help = "Number of threads used for processing projects. Use 0 for common pool").int()
        .default(0)

    override fun run() {
        getResultWriter()?.use { resultWriter ->
            val taskExecutor = TaskExecutor(ioThreads, cpuThreads)
            logger.info { "Start processing projects in ${projects.path}..." }

            projects.forEachLine { path ->
                if (path.startsWith("http")) {
                    taskExecutor.runDownloadingTask(GitRepoDownloadingTask(path, repoStorage))
                        .thenCompose { repoPath ->
                            taskExecutor.runComputationTask(ProjectProcessingTask(repoPath))
                        }.thenAccept { methodInfos -> taskExecutor.runResultSavingTask {
                            writeTestMethodInfos(methodInfos, resultWriter)
                        }}
                } else if (path.isNotBlank()) {
                    taskExecutor.runComputationTask(ProjectProcessingTask(File(path)))
                        .thenAccept { methodInfos -> taskExecutor.runResultSavingTask {
                                writeTestMethodInfos(methodInfos, resultWriter)
                        }}
                }
            }
            taskExecutor.join()
            logger.info { "Finished processing projects." }
        }
    }

    private fun writeTestMethodInfos(testMethodInfos: List<TestMethodInfo>, resultWriter: ResultWriter) {
        if (testMethodInfos.isNotEmpty()) {
            synchronized(resultWriter) {
                val projectName = testMethodInfos[0].classInfo.projectInfo.name
                namedThread("${projectName}.resultWriter") {
                    resultWriter.writeTestMethods(testMethodInfos)
                }
            }
        }
    }

    private class GitRepoDownloadingTask(private val url: String, private val targetDir: File) : Callable<File> {
        override fun call(): File {
            val projectName = url.substringAfterLast('/').removeSuffix(".git")
            return namedThread("${projectName}.downloader") {
                val destination = File(targetDir, projectName)
                if (destination.isDirectory) {
                    logger.warn("$destination directory already exists and will be cleaned")
                    destination.deleteRecursively()
                }

                logger.info("Cloning Git repository $url to $destination")

                val git = Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(destination)
                    .setDepth(1)
                    .setProgressMonitor(TextProgressMonitor(LogWriter()))
                    .call()
                val directory = git.repository.workTree
                git.close()

                directory
            }
        }
    }

    private class LogWriter : Writer() {
        override fun write(str: String) {
            logger.info(str.trim())
        }

        override fun write(buffer: CharArray, offset: Int, len: Int) {
            write(String(buffer, offset, len))
        }

        override fun close() {}

        override fun flush() {}

    }

    private class ProjectProcessingTask(
        private val path: File
    ) : Callable<List<TestMethodInfo>> {
        override fun call(): List<TestMethodInfo> {
            val projectName = path.name
            return namedThread("${projectName}.processor") {
                val buildSystem = detectBuildSystem(path)
                val projectInfo = ProjectInfo(projectName, buildSystem)
                val res = buildSystem.getProjectModules(path).flatMap { (moduleName, modulePath) ->
                    ParserRunner(Lang.values(), modulePath, ModuleInfo(moduleName, projectInfo)).call()
                }
                res
            }
        }
    }

    private fun getResultWriter() = when (outputFormat) {
        OutputType.JSON.value -> JsonResultWriter(getOutputFile().toPath())
        OutputType.CSV.value -> CsvResultWriter(getOutputFile().toPath())
        OutputType.SQLITE.value -> DBResultWriter("jdbc:sqlite:${getOutputFile().toPath()}")
        else -> null
    }

    private fun getOutputFile(): File = if (outputPath.isDirectory) {
        File(outputPath, "results.${outputFormat}")
    } else {
        outputPath
    }
}

private inline fun <T> namedThread(name: String, action: () -> T): T {
    val thread = Thread.currentThread()
    val oldName = thread.name
    thread.name = name
    try {
        return action()
    } finally {
        thread.name = oldName
    }
}

fun main(args: Array<String>) = Runner().main(args)