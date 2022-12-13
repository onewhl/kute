package project;

import org.testng.annotations.Test;

public class DisabledJavaTest {
    @Test(enabled = false)
    public void testDisabled() {
        throw new UnsupportedOperationException("implement later");
    }
}