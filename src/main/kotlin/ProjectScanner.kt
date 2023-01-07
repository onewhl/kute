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
        if (path.startsWith("http")) {
            taskExecutor.runDownloadingTask(GitRepoDownloadingTask(path, repoStorage))
                .handle { repoPath, ex ->
                    tasks += if (ex != null) {
                        CompletableFuture.failedFuture(ex)
                    } else {
                        taskExecutor.runComputationTask(ProjectProcessingTask(repoPath, cleanupClonedRepos))
                    }
                }
            return true
        }
        if (path.isNotBlank()) {
            File(path).takeIf { it.isDirectory }?.let { directory ->
                tasks += taskExecutor.runComputationTask(ProjectProcessingTask(directory, deleteAfterProcessing = false))
                return true
            }
        }
        return false
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
        private val path: File,
        private val deleteAfterProcessing: Boolean
    ) : Callable<List<TestMethodInfo>> {
        override fun call(): List<TestMethodInfo> {
            val projectName = path.name
            return namedThread("${projectName}.processor") {
                try {
                    val buildSystem = detectBuildSystem(path)
                    val projectInfo = ProjectInfo(projectName, buildSystem)
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

