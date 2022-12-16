package project;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(JUnitParamsRunner.class)
public class JUnitParamsJavaTest {
    @Test
    @Parameters(value = {"true", "false"})
    public void testParameterized(boolean arg) {
        assertEquals(arg, arg);
    }
}