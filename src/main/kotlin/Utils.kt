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