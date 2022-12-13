package project

import org.junit.Assert.assertEquals
import org.junit.Test

class CommentKotlinTest {
    // This is a first line of comments
    // This is a second line of comments
    @Test
    fun testSlashComment() = assertEquals(1, 1)

   /*
    This is a multiline comment
    */
   @Test
   fun testMultiLineComment() = assertEquals(1, 1)

    /**
    This is Javadoc comment
    */
    @Test
    fun testJavadocComment() = assertEquals(1, 1)
}