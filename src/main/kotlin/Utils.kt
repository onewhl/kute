import java.io.OutputStream

fun OutputStream.writeCsv(methods: List<MethodInfo>) {
    val writer = bufferedWriter()
    writer.write(""""Project", "Name", "Body", "Comment", "Display name"""")
    writer.newLine()
    methods.forEach {
        writer.write("${it.project}, ${it.name}, ${it.body}, ${it.comment}, ${it.displayName}")
        writer.newLine()
    }
    writer.flush()
}