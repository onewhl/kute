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
    val sourceMethod: SourceMethodInfo?
)

@Serializable
data class TestClassInfo(
    val name: String,
    @SerialName("package") val pkg: String,
    val projectInfo: ProjectInfo,
    val moduleInfo: ModuleInfo,
    val sourceClass: SourceClassInfo?,
    val language: Lang
)

@Serializable
data class SourceMethodInfo(
    val name: String,
    val body: String,
    val sourceClass: SourceClassInfo
)

@Serializable
data class SourceClassInfo(
    val name: String,
    @SerialName("package") val pkg: String,
    val moduleInfo: ModuleInfo,
    val language: Lang,
    @Transient val file: File = throw IllegalStateException("file must be provided")
)

@Serializable
data class ProjectInfo(
    val name: String,
    val buildSystem: BuildSystem
)

@Serializable
data class ModuleInfo(
    val name: String,
    val projectInfo: ProjectInfo
)