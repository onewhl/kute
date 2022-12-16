package project

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(JUnitParamsRunner::class)
class JUnitParamsKotlinTest {
    @Test
    @Parameters(value = ["true", "false"])
    fun testParameterized(arg: Boolean) = assertEquals(arg, arg)
}