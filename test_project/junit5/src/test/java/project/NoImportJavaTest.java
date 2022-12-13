package project;

public class NoImportJavaTest {
    @org.junit.jupiter.api.Test
    public void testWithoutImports() {
        org.junit.jupiter.api.Assertions.assertEquals(1, 1);
    }
}