package project;

public class NoImportJavaTest {
    @org.testng.annotations.Test
    public void testWithoutImports() {
        org.testng.Assert.assertEquals(1, 1);
    }
}