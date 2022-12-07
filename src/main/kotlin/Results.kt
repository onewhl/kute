import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import parsers.Lang
import java.io.File
import java.lang.IllegalStateException
import java.util.concurrent.atomic.AtomicInteger

@Serializable
data class TestMethodInfo(
    var name: String,
    var body: String,
    var comment: String,
    var displayName: String,
    var isParametrised: Boolean,
    val classInfo: TestClassInfo,
    val sourceMethod: SourceMethodInfo?,
    val id: Int = IdGenerator.testMethodCounter.getAndIncrement()
)

@Serializable
data class TestClassInfo(
    val name: String,
    @SerialName("package") val pkg: String,
    val projectInfo: ProjectInfo,
    val moduleInfo: ModuleInfo,
    val sourceClass: SourceClassInfo?,
    val language: Lang,
    val id: Int = IdGenerator.testClassCounter.getAndIncrement()
)

@Serializable
data class SourceMethodInfo(
    val name: String,
    val body: String,
    val sourceClass: SourceClassInfo,
    val id: Int = IdGenerator.sourceMethodCounter.getAndIncrement()
)

@Serializable
data class SourceClassInfo(
    val name: String,
    @SerialName("package") val pkg: String,
    val moduleInfo: ModuleInfo,
    val language: Lang,
    @Transient val file: File = throw IllegalStateException("file must be provided"),
    val id: Int = IdGenerator.sourceClassCounter.getAndIncrement(),
)

@Serializable
data class ProjectInfo(
    val name: String,
    val buildSystem: BuildSystem,
    val id: Int = IdGenerator.projectCounter.getAndIncrement(),
)

@Serializable
data class ModuleInfo(
    val name: String,
    val projectInfo: ProjectInfo,
    val id: Int = IdGenerator.moduleCounter.getAndIncrement()
)

object IdGenerator {
    val projectCounter: AtomicInteger = AtomicInteger(1)
    val moduleCounter: AtomicInteger = AtomicInteger(1)
    val testMethodCounter: AtomicInteger = AtomicInteger(1)
    val sourceMethodCounter: AtomicInteger = AtomicInteger(1)
    val testClassCounter: AtomicInteger = AtomicInteger(1)
    val sourceClassCounter: AtomicInteger = AtomicInteger(1)
}