package project;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CommentJavaTest {
    // This is a first line of comments
    // This is a second line of comments
    @Test
    public void testSlashComment() {
        assertEquals(1, 1);
    }

    /*
     This is a multiline comment
     */
    @Test
    public void testMultiLineComment() {
        assertEquals(1, 1);
    }

    /**
     This is Javadoc comment
     */
    @Test
    public void testJavadocComment() {
        assertEquals(1, 1);
    }
}
