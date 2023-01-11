package writers

import ModuleInfo
import ProjectInfo
import ResultWriter
import SourceClassInfo
import SourceMethodInfo
import TestMethodInfo
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.csv.Csv
import kotlinx.serialization.csv.encode.EncoderProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.serializer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass

@OptIn(ExperimentalSerializationApi::class)
class CsvResultWriter(path: Path) : ResultWriter {
    private val csv = Csv(from = Csv.Rfc4180) { hasHeaderRecord = true }
    private val writer = Files.newBufferedWriter(path)
    private val encoder = EncoderProvider.getEncoder(csv, writer)
    private val serializer = csv.serializersModule.serializer<TestMethodInfo>()

    override fun writeTestMethod(method: TestMethodInfo) {
        encoder.encodeSerializableValue(serializer, method)
        // below is workaround for defect when nullable field results in single empty column
        // instead of an empty column for each property
        if (method.sourceMethod == null) {
            writer.append(emptySourceMethodPlaceholder)
        }
        if (method.classInfo.sourceClass == null) {
            writer.append(emptySourceClassPlaceholder)
        }
    }

    override fun close() {
        writer.flush()
        writer.close()
    }

    companion object {
        private val numberOfColumnsInModuleInfo = ModuleInfo("", ProjectInfo("", BuildSystem.OTHER, ""))
            .let { Csv.Default.encodeToString(it).split(",").size }
        private val numberOfPropsInSourceClassInfo: Int =
            numberOfProps(SourceClassInfo::class) - 1 + numberOfColumnsInModuleInfo
        private val numberOfPropsInSourceMethodInfo: Int =
            numberOfProps(SourceMethodInfo::class) - 1 + numberOfPropsInSourceClassInfo
        private val emptySourceClassPlaceholder = ",".repeat(numberOfPropsInSourceClassInfo - 1)
        private val emptySourceMethodPlaceholder = ",".repeat(numberOfPropsInSourceMethodInfo - 1)

        private fun numberOfProps(clazz: KClass<*>): Int = clazz.constructors
            .filter { it.annotations.isEmpty() }
            .maxOf { it.parameters.size }
    }
}