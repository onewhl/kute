package mappers

import BuildSystem
import ModuleInfo
import ProjectInfo
import SourceClassInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import parsers.Lang
import java.io.File

class ClassMapperTest {
    private val module = ModuleInfo("test", ProjectInfo("test", BuildSystem.OTHER, 1), 1)

    @Test
    fun `test that single source with corresponding name and package is located`() {
        val entityJava = File("src/io/test/Entity.java")
        val classNameToSources = mapOf("Entity" to listOf(entityJava))
        val mapper = ClassMapper(module, classNameToSources) { "io.test" }
        val testClass = TestClassMeta("EntityTest", "io.test", Lang.JAVA) { true }
        val expectedSourceClass = SourceClassInfo("Entity", "io.test", module, Lang.JAVA, entityJava)
        assertThat(mapper.findSourceClass(testClass))
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(expectedSourceClass)
    }

    @Test
    fun `test that same-package discriminator works if multiple same-name classes exist in different packages and all used in test`() {
        val classNameToSources = mapOf("Entity" to listOf("", "dto", "model")
            .map { File("src/io/test/$it/Entity.java") }
        )
        val mapper = ClassMapper(module, classNameToSources, PathBasedPackageResolver("src/"))
        val testClass = TestClassMeta("EntityTest", "io.test", Lang.JAVA) { true }
        val expectedFile = File("src/io/test/Entity.java")
        val expectedSourceClass = SourceClassInfo("Entity", "io.test", module, Lang.JAVA,  expectedFile)
        assertThat(mapper.findSourceClass(testClass))
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(expectedSourceClass)
    }

    @Test
    fun `test that only class used inside test can be chosen as source class even if class with same name in same package exists`() {
        val expectedFile = File("src/io/test/model/Entity.java")
        val classNameToSources = mapOf("Entity" to listOf("", "dto", "model")
            .map { File("src/io/test/$it/Entity.java") }
        )
        val mapper = ClassMapper(module, classNameToSources, PathBasedPackageResolver("src/"))
        val testClass = TestClassMeta("EntityTest", "io.test", Lang.JAVA) { it.file == expectedFile }
        val expectedSourceClass = SourceClassInfo("Entity", "io.test.model", module, Lang.JAVA,  expectedFile)
        assertThat(mapper.findSourceClass(testClass))
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(expectedSourceClass)
    }

    @Test
    fun `test that a class containing a token from test class name can be chosen as source class`() {
        val expectedFile = File("src/io/test/model/Entity.java")
        val classNameToSources = mapOf("Entity" to listOf(expectedFile))
        val mapper = ClassMapper(module, classNameToSources, PathBasedPackageResolver("src/"))
        val testClass = TestClassMeta("SerializingEntityAsJsonTest", "io.test", Lang.JAVA) { it.file == expectedFile }
        val expectedSourceClass = SourceClassInfo("Entity", "io.test.model", module, Lang.JAVA,  expectedFile)
        assertThat(mapper.findSourceClass(testClass))
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(expectedSourceClass)
    }
}

class TestClassMeta(
    override val name: String,
    override val packageName: String,
    override val language: Lang,
    private val classUsageResolver: (SourceClassInfo) -> Boolean
) : ClassMeta {
    override fun hasClassUsed(sourceClass: SourceClassInfo): Boolean = classUsageResolver(sourceClass)
}

class PathBasedPackageResolver(private val prefix: String): PackageNameResolver {
    override fun extractPackageName(file: File): String = file.toString().let {
        it.substring(0, it.lastIndexOf('/'))
            .removePrefix(prefix)
            .replace('/', '.')
    }
}