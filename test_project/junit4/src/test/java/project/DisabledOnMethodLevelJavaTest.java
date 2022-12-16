package project;

import org.junit.Ignore;
import org.junit.Test;

public class DisabledOnMethodLevelJavaTest {
    @Test
    @Ignore
    public void testDisabled() {
        throw new UnsupportedOperationException("implement later");
    }
}