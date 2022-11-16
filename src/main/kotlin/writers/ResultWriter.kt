import java.io.Closeable

/**
 * Common interface for result writers.
 *
 * The format type should be defined in [writers.OutputType].
 */
interface ResultWriter : Closeable {
    fun writeTestMethod(method: TestMethodInfo)
    fun writeTestMethods(methods: List<TestMethodInfo>) = methods.forEach { writeTestMethod(it) }
}
