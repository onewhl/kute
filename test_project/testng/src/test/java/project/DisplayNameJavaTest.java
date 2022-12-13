package project;


import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class DisplayNameJavaTest {
    @Test(description = "Test with DisplayName")
    public void testWithDisplayName(){
        assertEquals(1, 1);
    }
}