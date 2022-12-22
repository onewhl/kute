inline fun <T> namedThread(name: String, action: () -> T): T {
    val thread = Thread.currentThread()
    val oldName = thread.name
    thread.name = name
    try {
        return action()
    } finally {
        thread.name = oldName
    }
}

class ParallelTask<R>(private val task: () -> R): java.util.concurrent.RecursiveTask<R>() {
    override fun compute(): R = task()
}