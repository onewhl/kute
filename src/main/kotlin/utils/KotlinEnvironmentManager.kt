package utils

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.io.FileUtilRt
import org.jetbrains.kotlin.com.intellij.openapi.vfs.CharsetToolkit
import org.jetbrains.kotlin.com.intellij.psi.PsiFile
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.com.intellij.psi.impl.PsiFileFactoryImpl
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

class KotlinEnvironmentManager {
    companion object {
        fun createKotlinCoreEnvironment(libraries: Set<File>?): KotlinCoreEnvironment {
            val configuration = CompilerConfiguration()
            val files: MutableList<File> = PathUtil.getJdkClassesRootsFromCurrentJre().toMutableList()
            files.addAll(libraries!!)
            //JvmContentRootsKt.addJvmClasspathRoots(configuration, files)
            configuration.put(CommonConfigurationKeys.MODULE_NAME, JvmProtoBufUtil.DEFAULT_MODULE_NAME)
            configuration.put(
                CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
                org.jetbrains.kotlin.cli.common.messages.MessageCollector.Companion.NONE
            )
            return KotlinCoreEnvironment.createForProduction(
                {},
                configuration,
                EnvironmentConfigFiles.JVM_CONFIG_FILES
            )
        }

        /**
         * Creates KotlinCoreEnvironment with specified classpath.
         */
        fun buildPsiFile(file: String, environment: KotlinCoreEnvironment, content: String): PsiFile? {
            val newFile = File("tmp/$file")
            FileUtilRt.createDirectory(newFile)
            val factory = PsiFileFactory.getInstance(environment.project) as PsiFileFactoryImpl
            val virtualFile = KotlinLightVirtualFile(newFile, content)
            virtualFile.charset = CharsetToolkit.UTF8_CHARSET
            return factory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false)
        }
    }

    /**
     * Wrapper for VirtualFile that retains path on machine.
     */
    class KotlinLightVirtualFile(file: File, text: String) :
        LightVirtualFile(file.name, KotlinLanguage.INSTANCE, text) {
        private val path = file.canonicalPath

        override fun getPath(): String {
            return path
        }
    }
}