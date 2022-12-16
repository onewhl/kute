package project

import org.testng.Assert.assertEquals
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class DataProviderKotlinTest {
    @Test(dataProvider = "provideData")
    fun testParameterized(arg: Boolean) = assertEquals(arg, arg)

    @DataProvider
    private fun provideData() = arrayOf(true, false)
}