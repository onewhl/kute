package mappers

import BuildSystem
import ModuleInfo
import ProjectInfo
import SourceClassAndLocation
import SourceClassInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import parsers.Lang
import java.io.File

class ClassMapperTest {
    private val module = ModuleInfo("test", ProjectInfo("test", BuildSystem.OTHER, "/tmp/test"))

    @Test
    fun `test that single source with corresponding name and package is located`() {
        val entityJava = File("src/io/test/Entity.java")
        val classNameToSources = mapOf("Entity" to listOf(Pair(entityJava, module)))
        val mapper = ClassMapper(module, classNameToSources) { "io.test" }
        val testClass = TestClassMeta("EntityTest", "io.test", Lang.JAVA) { true }
        val expectedSourceClassAndLocation = SourceClassAndLocation(
            SourceClassInfo("Entity", "io.test", module, Lang.JAVA),
            entityJava
        )
        assertThat(mapper.findSourceClass(testClass))
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(expectedSourceClassAndLocation)
    }

    @Test
    fun `test that same-package discriminator works if multiple same-name classes exist in different packages and all used in test`() {
        val classNameToSources = mapOf("Entity" to listOf("", "dto", "model")
            .map { Pair(File("src/io/test/$it/Entity.java"), module) }
        )
        val mapper = ClassMapper(module, classNameToSources, PathBasedPackageResolver("src/"))
        val testClass = TestClassMeta("EntityTest", "io.test", Lang.JAVA) { true }
        val expectedSourceClassAndLocation = SourceClassAndLocation(
            SourceClassInfo("Entity", "io.test", module, Lang.JAVA),
            File("src/io/test/Entity.java")
        )
        assertThat(mapper.findSourceClass(testClass))
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(expectedSourceClassAndLocation)
    }

    @Test
    fun `test that only class used inside test can be chosen as source class even if class with same name in same package exists`() {
        val expectedFile = File("src/io/test/model/Entity.java")
        val classNameToSources = mapOf("Entity" to listOf("", "dto", "model")
            .map { Pair(File("src/io/test/$it/Entity.java"), module) }
        )
        val mapper = ClassMapper(module, classNameToSources, PathBasedPackageResolver("src/"))
        val testClass = TestClassMeta("EntityTest", "io.test", Lang.JAVA) { it.file == expectedFile }
        val expectedSourceClassAndLocation = SourceClassAndLocation(
            SourceClassInfo("Entity", "io.test.model", module, Lang.JAVA),
            expectedFile
        )
        assertThat(mapper.findSourceClass(testClass))
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(expectedSourceClassAndLocation)
    }

    @Test
    fun `test that a class containing a token from test class name can be chosen as source class`() {
        val expectedFile = File("src/io/test/model/Entity.java")
        val classNameToSources = mapOf("Entity" to listOf(Pair(expectedFile, module)))
        val mapper = ClassMapper(module, classNameToSources, PathBasedPackageResolver("src/"))
        val testClass = TestClassMeta("SerializingEntityAsJsonTest", "io.test", Lang.JAVA) { it.file == expectedFile }
        val expectedSourceClassAndLocation = SourceClassAndLocation(
            SourceClassInfo("Entity", "io.test.model", module, Lang.JAVA),
            expectedFile
        )
        assertThat(mapper.findSourceClass(testClass))
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(expectedSourceClassAndLocation)
    }
}

class TestClassMeta(
    override val name: String,
    override val packageName: String,
    override val language: Lang,
    private val classUsageResolver: (SourceClassAndLocation) -> Boolean
) : ClassMeta {
    override val methods: Iterable<MethodMeta> = emptyList()
    override fun hasClassUsage(sourceClassAndLocation: SourceClassAndLocation): Boolean = classUsageResolver(sourceClassAndLocation)
    override fun hasAnnotation(name: String): Boolean = false
    override fun getAnnotationValue(name: String, key: String?) = null
}

class PathBasedPackageResolver(private val prefix: String) : PackageNameResolver {
    override fun extractPackageName(file: File): String = file.toString().let {
        it.substring(0, it.lastIndexOf('/'))
            .removePrefix(prefix)
            .replace('/', '.')
    }
}