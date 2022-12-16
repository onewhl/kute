package project;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
class ParameterizedJavaTest {
    public final boolean arg;
    public ParameterizedJavaTest(boolean arg) {
        this.arg = arg;
    }

    @Test
    public void testParameterized() {
        assertEquals(arg, arg);
    }

    public static Object[][] provideData() {
        return new Object[][]{{true}, {false}};
    }
}