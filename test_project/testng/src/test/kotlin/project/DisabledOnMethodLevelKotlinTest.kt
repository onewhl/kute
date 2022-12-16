package project

import org.testng.annotations.Test

class DisabledOnMethodLevelKotlinTest {
    @Test(enabled = false)
    fun testDisabled(): Unit = TODO("implement later")
}