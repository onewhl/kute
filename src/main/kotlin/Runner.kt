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

private val logger = KotlinLogging.logger {}

class Runner(
    private val projectsPath: File,
    outputFormat: OutputType,
    outputPath: File,
    private val repoStorage: File,
    ioThreads: Int,
    cpuThreads: Int
) {
    private val outputFile = if (outputPath.isDirectory) {
        File(outputPath, "results.${outputFormat.value}")
    } else {
        outputPath
    }

    private val resultWriter = when (outputFormat) {
        OutputType.JSON -> JsonResultWriter(outputFile.toPath())
        OutputType.CSV -> CsvResultWriter(outputFile.toPath())
        OutputType.SQLITE -> DBResultWriter("jdbc:sqlite:$outputFile")
    }

    private val taskExecutor = TaskExecutor(ioThreads, cpuThreads)

    fun run() {
        logger.info { "Start processing projects in ${projectsPath.path}..." }

        projectsPath.forEachLine { path ->
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
                buildSystem.getProjectModules(path).flatMap { (moduleName, modulePath) ->
                    ParserRunner(Lang.values(), modulePath, ModuleInfo(moduleName, projectInfo)).call()
                }
            }
        }
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
