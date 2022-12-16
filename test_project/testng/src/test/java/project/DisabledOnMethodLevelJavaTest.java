package project;

import org.testng.annotations.Test;

public class DisabledOnMethodLevelJavaTest {
    @Test(enabled = false)
    public void testDisabled() {
        throw new UnsupportedOperationException("implement later");
    }
}