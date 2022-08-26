import java.io.Closeable
import java.io.OutputStream

interface ResultWriter : Closeable {
    fun writeTestMethod(method: TestMethodInfo)
    fun writeSourceMethod(method: SourceMethodInfo)
    fun writeTestClass(classInfo: TestClassInfo)
    fun writeSourceClass(sourceClass: SourceClassInfo)
}

class CsvResultWriter : ResultWriter {
    fun OutputStream.writeCsv(methods: List<TestMethodInfo>) {
        //TODO: Use specialized CSV writer with correct escaping special characters, e.g. univocity
        val writer = bufferedWriter()
        writer.write(""""Project", "Name", "Body", "Comment", "Display name"""")
        writer.newLine()
        methods.forEach {
            writer.write("${it.name}, ${it.body}, ${it.comment}, ${it.displayName}")
            writer.newLine()
        }
        writer.flush()
    }

    override fun writeTestMethod(method: TestMethodInfo) {
        TODO("Not yet implemented")
    }

    override fun writeSourceMethod(method: SourceMethodInfo) {
        TODO("Not yet implemented")
    }

    override fun writeTestClass(classInfo: TestClassInfo) {
        TODO("Not yet implemented")
    }

    override fun writeSourceClass(sourceClass: SourceClassInfo) {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}