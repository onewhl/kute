package project

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class ParameterizedKotlinTest(val arg: Boolean) {
    @Test
    fun testParameterized() = assertEquals(arg, arg)

    companion object {
        @Parameters
        @JvmStatic
        fun provideData() = arrayOf(true, false)
    }
}