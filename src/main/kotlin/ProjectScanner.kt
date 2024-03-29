import mu.KotlinLogging
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.TextProgressMonitor
import org.jetbrains.kotlin.utils.identity
import parsers.Lang
import parsers.createParser
import java.io.File
import java.io.Writer
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

private val logger = KotlinLogging.logger {}

class ProjectScanner constructor(
    private val repoStorage: File,
    private val taskExecutor: TaskExecutor,
    private val cleanupClonedRepos: Boolean,
    private val processingTaskUpdater: (Callable<List<TestMethodInfo>>) -> Callable<List<TestMethodInfo>>,
    private val langs: Array<Lang> = Lang.values()
) {
    constructor(repoStorage: File, ioThreads: Int, cpuThreads: Int, cleanupClonedRepos: Boolean)
            : this(repoStorage, TaskExecutor(ioThreads, cpuThreads), cleanupClonedRepos, identity())

    fun scanProject(
        path: String,
        processingTaskCallback: (String, Future<List<TestMethodInfo>>) -> Unit
    ): Future<out Any>? {
        if (path.startsWith("https://")) {
            GitRepoDownloadingTask(path, repoStorage).takeIf { it.project.name.isNotEmpty() }?.let { downloadingTask ->
                return taskExecutor.runDownloadingTask(downloadingTask)
                    .handle { repoPath, ex ->
                        val processingTask = if (ex != null) {
                            CompletableFuture.failedFuture(ex)
                        } else {
                            taskExecutor.runComputationTask(
                                createProcessingTask(repoPath, downloadingTask.project, path, cleanupClonedRepos)
                            )
                        }
                        processingTaskCallback(path, processingTask)
                    }
            } ?: logger.warn { "Could not extract project by URL: $path" }
        } else if (path.isNotBlank()) {
            val directory = File(path)
            when {
                directory.isDirectory -> {
                    return taskExecutor.runComputationTask(
                        createProcessingTask(directory, Project(directory.name), path, deleteAfterProcessing = false)
                    ).also { processingTaskCallback(path, it) }
                }
                directory.isFile -> logger.warn { "Path to project must be a directory, but file provided: $path" }
                else -> logger.warn { "Directory doesn't exist: $path" }
            }
        }
        return null
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

    private fun createProcessingTask(
        path: File,
        project: Project,
        originalPath: String,
        deleteAfterProcessing: Boolean
    ) = processingTaskUpdater(
        ProjectProcessingTask(path, project, originalPath, deleteAfterProcessing, langs)
    )

    private class ProjectProcessingTask(
        private val path: File,
        private val project: Project,
        private val originalPath: String,
        private val deleteAfterProcessing: Boolean,
        private val langs: Array<Lang> = Lang.values()
    ) : Callable<List<TestMethodInfo>> {
        override fun call(): List<TestMethodInfo> {
            return namedThread("${project.fullName}.processor") {
                try {
                    val buildSystem = detectBuildSystem(path)
                    val projectInfo = ProjectInfo(project.name, buildSystem, originalPath)
                    val supportedExtensions = langs.map { it.extension }.toSet()

                    val classNameToFileAcrossAllModules = mutableMapOf<String, MutableList<Pair<File, ModuleInfo>>>()
                    val parsingTasks = buildSystem.getProjectModules(path).flatMap { (moduleName, modulePath) ->
                        val moduleInfo = ModuleInfo(moduleName, projectInfo)
                        val classFiles: List<File> = extractFiles(modulePath, supportedExtensions)
                        val classNameToFile = classFiles.groupByTo(
                            classNameToFileAcrossAllModules,
                            { it.name.substringBeforeLast('.') },
                            { Pair(it, moduleInfo) }
                        )
                        langs.map { Pair(createParser(it, modulePath, moduleInfo, classNameToFile), classFiles) }
                    }
                    parsingTasks.flatMap { (parser, classes) -> parser.process(classes) }
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

        private fun extractFiles(module: File, extensions: Set<String>): List<File> = module
            .walkTopDown()
            .filter { extensions.contains(it.extension) && it.isFile }
            .toList()
    }
}

