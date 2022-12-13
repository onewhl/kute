package project

import org.testng.Assert.assertEquals
import org.testng.annotations.Test

class DisplayNameKotlinTest {
    @Test(description = "Test with DisplayName")
    fun testWithDisplayName() = assertEquals(1, 1)
}