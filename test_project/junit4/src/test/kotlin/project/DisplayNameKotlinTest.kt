package project

import io.qameta.allure.junit4.DisplayName
import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayNameKotlinTest {
    @Test
    @DisplayName("Test with DisplayName")
    fun testWithDisplayName() = assertEquals(1, 1)
}