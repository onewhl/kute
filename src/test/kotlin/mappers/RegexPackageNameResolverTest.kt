package mappers

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class RegexPackageNameResolverTest {
    @ParameterizedTest
    @CsvSource(
        "\"package com.test\n\nclass Test\",com.test",
        "\"package com.test;\n\nclass Test\",com.test",
        "\"/* LICENSE */\npackage com.test\n\nclass Test\",com.test",
        quoteCharacter = '"'
    )
    fun extractPackageName(input: String, expectedOutput: String) {
        assertEquals(expectedOutput, RegexPackageNameResolver.extractPackageName(input))
    }
}