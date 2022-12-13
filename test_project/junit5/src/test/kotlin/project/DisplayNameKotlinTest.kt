package project

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class DisplayNameKotlinTest {
    @Test
    @DisplayName("Test with DisplayName")
    fun testWithDisplayName() = assertEquals(1, 1)
}