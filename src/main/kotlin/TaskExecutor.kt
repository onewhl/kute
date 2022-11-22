import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit
import kotlin.math.max

class TaskExecutor(ioThreads: Int, computeThreads: Int) {
    private val ioExecutor = if (ioThreads == 0) ForkJoinPool.commonPool() else ForkJoinPool(ioThreads)
    private val cpuExecutor = if (computeThreads == 0) ForkJoinPool.commonPool() else ForkJoinPool(
        max(computeThreads - 1, 1)
    )
    private val resultSaveExecutor = ForkJoinPool(1)

    fun <T : Any?> runDownloadingTask(task: Callable<T>): CompletableFuture<T> =
        CompletableFuture.supplyAsync(task::call, ioExecutor)

    fun <T : Any?> runComputationTask(task: Callable<T>): CompletableFuture<T> =
        CompletableFuture.supplyAsync(task::call, cpuExecutor)

    fun runResultSavingTask(task: Runnable) {
        resultSaveExecutor.execute(task)
    }

    fun join() {
        for (i in (0..2)) {
            do {
                var quiescent = cpuExecutor.awaitQuiescence(Long.MAX_VALUE, TimeUnit.MILLISECONDS)
                if (ioExecutor == ForkJoinPool.commonPool()) {
                    quiescent = ioExecutor.awaitQuiescence(Long.MAX_VALUE, TimeUnit.MILLISECONDS) && quiescent
                } else if (!ioExecutor.isQuiescent) {
                    quiescent = false
                }
                if (!resultSaveExecutor.isQuiescent) {
                    quiescent = false
                }
                if (!quiescent) {
                    Thread.sleep(100)
                }
            } while (!quiescent)
            Thread.sleep(100)
        }
    }

}