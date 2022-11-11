package writers

import ResultWriter
import TestMethodInfo
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import java.io.BufferedOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path

private const val separator = ','.code

/**
 * Writes results in JSON format if [OutputType.JSON] is chosen.
 */
class JsonResultWriter(path: Path) : ResultWriter {
    private val stream: OutputStream = BufferedOutputStream(Files.newOutputStream(path)).also { it.write('['.code) }
    private var hasWrittenElement = false

    @OptIn(ExperimentalSerializationApi::class)
    override fun writeTestMethod(method: TestMethodInfo) {
        //add element separator before each element
        if (hasWrittenElement) {
            stream.write(separator)
        } else {
            hasWrittenElement = true
        }
        Json.encodeToStream(method, stream)
    }

    override fun close() {
        stream.write(']'.code)
        stream.close()
    }
}