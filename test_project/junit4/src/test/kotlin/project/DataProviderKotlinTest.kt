package project

import com.tngtech.java.junit.dataprovider.DataProvider
import com.tngtech.java.junit.dataprovider.DataProviderRunner
import com.tngtech.java.junit.dataprovider.UseDataProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(DataProviderRunner::class)
class DataProviderKotlinTest {
    @Test
    @UseDataProvider("provideData")
    fun testParameterized(arg: Boolean) = assertEquals(arg, arg)

    companion object {
        @JvmStatic
        @DataProvider
        fun provideData(): Array<Array<Any>> = arrayOf(arrayOf(true), arrayOf(false))
    }
}