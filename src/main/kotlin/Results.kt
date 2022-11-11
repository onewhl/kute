import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicLong

@Serializable
data class TestMethodInfo(
    var name: String,
    var body: String,
    var comment: String,
    var displayName: String,
    var isParametrised: Boolean,
    val classInfo: TestClassInfo,
    val sourceMethod: SourceMethodInfo?,
    val id: Long = IdGenerator.testMethodCounter.getAndIncrement()
)

@Serializable
data class TestClassInfo(
    val name: String,
    val projectInfo: ProjectInfo,
    val moduleInfo: ModuleInfo,
    val sourceClass: SourceClassInfo?,
    val id: Long = IdGenerator.testClassCounter.getAndIncrement()
)

@Serializable
data class SourceMethodInfo(
    val name: String,
    val body: String,
    val sourceClass: SourceClassInfo,
    val id: Long = IdGenerator.sourceMethodCounter.getAndIncrement()
)

@Serializable
data class SourceClassInfo(
    val name: String,
    val moduleInfo: ModuleInfo,
    val id: Long = IdGenerator.sourceClassCounter.getAndIncrement()
)

@Serializable
data class ProjectInfo(
    val name: String,
    val buildSystem: BuildSystem,
    val id: Long = IdGenerator.projectCounter.getAndIncrement(),
)

@Serializable
data class ModuleInfo(
    val name: String,
    val projectInfo: ProjectInfo,
    val id: Long = IdGenerator.moduleCounter.getAndIncrement()
)

object IdGenerator {
    val projectCounter: AtomicLong = AtomicLong(1)
    val moduleCounter: AtomicLong = AtomicLong(1)
    val testMethodCounter: AtomicLong = AtomicLong(1)
    val sourceMethodCounter: AtomicLong = AtomicLong(1)
    val testClassCounter: AtomicLong = AtomicLong(1)
    val sourceClassCounter: AtomicLong = AtomicLong(1)
}