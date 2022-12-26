import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinTask
import kotlin.math.max

class TaskExecutor(ioThreads: Int, computeThreads: Int) {
    private val ioExecutor = if (ioThreads == 0) ForkJoinPool.commonPool() else ForkJoinPool(ioThreads)
    private val cpuExecutor = if (computeThreads == 0) ForkJoinPool.commonPool() else ForkJoinPool(
        max(computeThreads - 1, 1)
    )

    fun <T : Any?> runDownloadingTask(task: Callable<T>): CompletableFuture<T> =
        CompletableFuture.supplyAsync(task::call, ioExecutor)

    fun <T : Any?> runComputationTask(task: Callable<T>): ForkJoinTask<T> =
        cpuExecutor.submit(task)
}