import java.io.File
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

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

data class Project(val author: String, val name: String) {
    constructor(name: String): this("", name)
    val fullName = if (author.isNotEmpty()) "$author/$name" else name

    companion object {
        fun fromPath(path: String): Project =
            if (path.startsWith("https://")) {
                val indexOfName = path.lastIndexOf('/')
                val indexOfAuthor = path.lastIndexOf('/', indexOfName - 1)
                val author = path.substring(indexOfAuthor + 1, indexOfName)
                val name = path.substring(indexOfName + 1).removeSuffix(".git")
                Project(author, name)
            } else {
                Project(File(path).name)
            }
    }
}

fun <T> Future<T>.valueOrNull(): T? =
    try {
        this.get()
    } catch (e: ExecutionException) {
        null
    }