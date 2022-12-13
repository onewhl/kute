package project;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class DisabledJavaTest {
    @Test
    @Disabled
    public void testDisabled() {
        throw new UnsupportedOperationException("implement later");
    }
}