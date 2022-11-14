package writers

import ResultWriter
import TestMethodInfo
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.csv.Csv
import kotlinx.serialization.csv.encode.EncoderProvider
import kotlinx.serialization.serializer
import java.nio.file.Files
import java.nio.file.Path

@OptIn(ExperimentalSerializationApi::class)
class CsvResultWriter(path: Path) : ResultWriter {
    private val csv = Csv { hasHeaderRecord = true }
    private val writer = Files.newBufferedWriter(path)
    private val encoder = EncoderProvider.getEncoder(csv, writer)
    private val serializer = csv.serializersModule.serializer<TestMethodInfo>()

    override fun writeTestMethod(method: TestMethodInfo) {
        encoder.encodeSerializableValue(serializer, method)
    }

    override fun close() {
        writer.flush()
        writer.close()
    }
}