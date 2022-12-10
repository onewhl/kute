package mappers

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ClassMapperImplTest {
    @ParameterizedTest
    @CsvSource(
        "ImplTest,Impl",
        "ImplTests,Impl",
        "ImplTestCase,Impl",
        "ImplIT,Impl",
        "ImplITCase,Impl",
        "ImplITTest,ImplIT",
        "TestImpl,Impl",
        "ITImpl,Impl",
        "TestImplIT,TestImpl",
    )
    fun removeSingleTestSuffixOrPrefix(input: String, expectedOutput: String) {
        assertEquals(expectedOutput, ClassMapper.removeSingleTestSuffixOrPrefix(input))
    }

    @Test
    fun splitByToken() {
        assertEquals(
            listOf("Persistence", "Annotation", "Bean", "Post", "Processor").map { it.length },
            ClassMapper.splitByTokensCamelCase("PersistenceAnnotationBeanPostProcessor")
        )
    }

    @Test
    fun generateAllTokenCombinations() {
        assertEquals(
            listOf(
                "PersistenceAnnotationBeanPostProcessor",
                "PersistenceAnnotationBeanPost",
                "AnnotationBeanPostProcessor",
                "PersistenceAnnotationBean",
                "PersistenceAnnotation",
                "AnnotationBeanPost",
                "BeanPostProcessor",
                "AnnotationBean",
                "PostProcessor",
                "Persistence",
                "Annotation",
                "Processor",
                "BeanPost",
                "Bean",
                "Post"
            ),
            ClassMapper.generateTokenCombinations("PersistenceAnnotationBeanPostProcessor")
        )
    }

    @Test
    fun generateTokenCombinationsWith3OrMoreTokens() {
        assertEquals(
            listOf(
                "PersistenceAnnotationBeanPostProcessor",
                "PersistenceAnnotationBeanPost",
                "AnnotationBeanPostProcessor",
                "PersistenceAnnotationBean",
                "AnnotationBeanPost",
                "BeanPostProcessor",
            ),
            ClassMapper.generateTokenCombinations("PersistenceAnnotationBeanPostProcessor") { 3 }
        )
    }
}