package project;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class DataProviderJavaTest {

    @Test(dataProvider = "provideData")
    public void testParameterized(boolean arg) {
        assertEquals(arg, arg);
    }

    @DataProvider
    private static Object[][] provideData() {
        return new Object[][] {{true}, {false}};
    }
}
