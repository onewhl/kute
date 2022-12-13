package project;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DisplayNameJavaTest {
    @Test
    @DisplayName("Test with DisplayName")
    public void testWithDisplayName(){
        assertEquals(1, 1);
    }
}