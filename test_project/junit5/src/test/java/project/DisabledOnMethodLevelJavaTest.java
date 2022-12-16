package project;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class DisabledOnMethodLevelJavaTest {
    @Test
    @Disabled
    public void testDisabled() {
        throw new UnsupportedOperationException("implement later");
    }
}