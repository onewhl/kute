package parsers

import ModuleInfo
import mappers.KotlinMetaFactory
import mu.KotlinLogging
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import java.io.File

private val logger = KotlinLogging.logger {}

class KotlinTestParser(
    path: File,
    module: ModuleInfo,
    classNameToFile: Map<String, List<File>>
) : AbstractParser<KtFile, KtClass, KtNamedFunction>(path, module, classNameToFile, logger) {
    override val language = Lang.KOTLIN
    override val metaFactory = KotlinMetaFactory
    override fun findMethods(testClass: KtClass): List<KtNamedFunction> =
        testClass.declarations.filterIsInstance<KtNamedFunction>()

    override fun mapFileToClass(file: KtFile): KtClass? =
        PsiTreeUtil.findChildrenOfType(file, KtClass::class.java).firstOrNull()

    override fun doParseSource(fileSource: File, content: String): KtFile =
        StaticKotlinFileParser.parse(fileSource.name, content)

}