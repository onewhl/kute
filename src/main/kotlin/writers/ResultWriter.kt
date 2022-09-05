import java.io.Closeable

interface ResultWriter : Closeable {
    fun writeTestMethod(method: TestMethodInfo)
}
