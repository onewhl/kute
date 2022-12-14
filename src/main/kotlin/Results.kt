import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import parsers.Lang
import java.io.File
import java.lang.IllegalStateException

@Serializable
data class TestMethodInfo(
    val name: String,
    val body: String,
    val comment: String,
    val displayName: String,
    val isParametrised: Boolean,
    val isDisabled: Boolean,
    val classInfo: TestClassInfo,
    val sourceMethod: SourceMethodInfo?
)

@Serializable
data class TestClassInfo(
    val name: String,
    @SerialName("package") val packageName: String,
    val projectInfo: ProjectInfo,
    val moduleInfo: ModuleInfo,
    val sourceClass: SourceClassInfo?,
    val language: Lang,
    val testFramework: TestFramework
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