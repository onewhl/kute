import writers.CsvResultWriter
import writers.DBResultWriter
import writers.JsonResultWriter
import writers.OutputType
import java.io.Closeable
import java.io.File

/**
 * Common interface for result writers.
 *
 * The format type should be defined in [writers.OutputType].
 */
interface ResultWriter : Closeable {
    fun writeTestMethod(method: TestMethodInfo)
    fun writeTestMethods(methods: List<TestMethodInfo>) = methods.forEach { writeTestMethod(it) }

    fun writeSynchronized(testMethodInfos: List<TestMethodInfo>) {
        if (testMethodInfos.isNotEmpty()) {
            synchronized(this) {
                val projectName = testMethodInfos[0].classInfo.projectInfo.name
                namedThread("${projectName}.resultWriter") {
                    writeTestMethods(testMethodInfos)
                }
            }
        }
    }

    companion object {
        fun create(outputFormat: OutputType, outputPath: File): ResultWriter {
            val outputFile = if (outputPath.isDirectory) {
                File(outputPath, "results.${outputFormat.value}")
            } else {
                outputPath
            }
            return when (outputFormat) {
                OutputType.JSON -> JsonResultWriter(outputFile.toPath())
                OutputType.CSV -> CsvResultWriter(outputFile.toPath())
                OutputType.SQLITE -> DBResultWriter("jdbc:sqlite:$outputFile")
            }
        }
    }
}