package project;


import io.qameta.allure.junit4.DisplayName;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DisplayNameJavaTest {
    @Test
    @DisplayName("Test with DisplayName")
    public void testWithDisplayName(){
        assertEquals(1, 1);
    }
}