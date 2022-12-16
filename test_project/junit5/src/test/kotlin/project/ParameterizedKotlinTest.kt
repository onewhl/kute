package project

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ParameterizedKotlinTest {
    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun testParameterized(arg: Boolean) = assertEquals(arg, arg)
}