package parsers

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import java.io.File

/**
 * Helper class to use JavaParser statically. Should be preferred over [com.github.javaparser.StaticJavaParser]
 * to save time on JavaCC parser initialization due to a bug in eager LookaheadSuccess exception initialization.
 */
object StaticJavaFileParser {
    private val cachedParsers = ThreadLocal.withInitial { JavaParser() }

    fun parse(file: File): CompilationUnit? = cachedParsers.get().parse(file).result.orElse(null)

    fun parse(code: String, originalFile: File): CompilationUnit? =
        cachedParsers.get().parse(code).result.orElse(null)?.also {
            it.setStorage(originalFile.toPath())
        }
}