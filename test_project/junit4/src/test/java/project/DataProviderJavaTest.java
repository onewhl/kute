package project;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(DataProviderRunner.class)
public class DataProviderJavaTest {
    @Test
    @UseDataProvider("provideData")
    public void testParameterized(boolean arg) {
        assertEquals(arg, arg);
    }

    @DataProvider
    public static Object[][] provideData() {
        return new Object[][]{{true}, {false}};
    }
}
