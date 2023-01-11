import mu.KotlinLogging
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.TextProgressMonitor
import parsers.Lang
import parsers.ParserRunner
import java.io.File
import java.io.Writer
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

private val logger = KotlinLogging.logger {}

class ProjectScanner private constructor(
    private val repoStorage: File,
    private val taskExecutor: TaskExecutor,
    private val cleanupClonedRepos: Boolean
) {
    constructor(repoStorage: File, ioThreads: Int, cpuThreads: Int, cleanupClonedRepos: Boolean)
            : this(repoStorage, TaskExecutor(ioThreads, cpuThreads), cleanupClonedRepos)

    fun scanProject(path: String, tasks: BlockingQueue<Future<List<TestMethodInfo>>>): Boolean {
        if (path.startsWith("https://")) {
            GitRepoDownloadingTask(path, repoStorage).takeIf { it.project.name.isNotEmpty() }?.let { downloadingTask ->
                taskExecutor.runDownloadingTask(downloadingTask)
                    .handle { repoPath, ex ->
                        tasks += if (ex != null) {
                            CompletableFuture.failedFuture(ex)
                        } else {
                            taskExecutor.runComputationTask(
                                ProjectProcessingTask(repoPath, downloadingTask.project, path, cleanupClonedRepos)
                            )
                        }
                    }
                return true
            } ?: logger.warn { "Could not extract project by URL: $path" }
        } else if (path.isNotBlank()) {
            val directory = File(path)
            when {
                directory.isDirectory -> {
                    tasks += taskExecutor.runComputationTask(
                        ProjectProcessingTask(directory, Project(directory.name), path, deleteAfterProcessing = false)
                    )
                    return true
                }
                directory.isFile -> logger.warn { "Path to project must be a directory, but file provided: $path" }
                else -> logger.warn { "Directory doesn't exist: $path" }
            }
        }
        return false
    }

    private class GitRepoDownloadingTask(private val url: String, private val targetDir: File) : Callable<File> {
        val project = Project.fromPath(url)

        override fun call(): File {
            return namedThread("${project.fullName}.downloader") {
                val destination = File(File(targetDir, project.author), project.name)
                if (destination.isDirectory) {
                    logger.warn("$destination directory already exists and will be cleaned")
                    destination.deleteRecursively()
                }

                logger.info("Cloning Git repository $url to $destination")

                Git.cloneRepository()
                    .setURI(url)
                    .setDirectory(destination)
                    .setDepth(1)
                    .setProgressMonitor(TextProgressMonitor(LogWriter()))
                    .call()
                    .close()

                destination
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
        private val path: File,
        private val project: Project,
        private val originalPath: String,
        private val deleteAfterProcessing: Boolean
    ) : Callable<List<TestMethodInfo>> {
        override fun call(): List<TestMethodInfo> {
            return namedThread("${project.fullName}.processor") {
                try {
                    val buildSystem = detectBuildSystem(path)
                    val projectInfo = ProjectInfo(project.name, buildSystem, originalPath)
                    buildSystem.getProjectModules(path).flatMap { (moduleName, modulePath) ->
                        ParserRunner(Lang.values(), modulePath, ModuleInfo(moduleName, projectInfo)).call()
                    }
                } catch (e: Throwable) {
                    logger.error(e) { e.message }
                    throw e
                } finally {
                    if (deleteAfterProcessing) {
                        logger.info { "Deleting $path" }
                        if (!path.deleteRecursively()) {
                            logger.warn("$path not fully deleted")
                        }
                    }
                }
            }
        }
    }
}

