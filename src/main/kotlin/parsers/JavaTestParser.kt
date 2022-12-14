package parsers

import ModuleInfo
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.MethodDeclaration
import mappers.JavaMetaFactory
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Extracts tests files, finds test method in them, and collects metadata.
 */
class JavaTestParser(
    path: File,
    module: ModuleInfo,
    classNameToFile: Map<String, List<File>>
) : AbstractParser<CompilationUnit, CompilationUnit, MethodDeclaration>(path, module, classNameToFile, logger) {
    override val language = Lang.JAVA
    override val metaFactory = JavaMetaFactory

    override fun findMethods(testClass: CompilationUnit): List<MethodDeclaration> =
        testClass.findAll(MethodDeclaration::class.java)

    override fun doParseSource(fileSource: File, content: String): CompilationUnit? =
        StaticJavaFileParser.parse(content, fileSource)

    override fun mapFileToClass(file: CompilationUnit): CompilationUnit = file
}