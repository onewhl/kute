package project;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ParameterizedJavaTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testParameterized(boolean arg) {
        assertEquals(arg, arg);
    }
}
