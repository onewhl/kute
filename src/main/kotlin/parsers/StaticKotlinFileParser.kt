package parsers

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.vfs.CharsetToolkit
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.psi.impl.PsiFileFactoryImpl
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

object StaticKotlinFileParser {
    private val cachedEnvironments = ThreadLocal.withInitial {
        KotlinEnvironment()
    }

    fun parse(file: File): KtFile = cachedEnvironments.get().psiFileFactory.let { factory ->
        val virtualFile = LightVirtualFile(file.name, KotlinLanguage.INSTANCE, file.readText())
        virtualFile.charset = CharsetToolkit.UTF8_CHARSET
        return factory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false) as KtFile
    }

    fun dropCaches() {
        cachedEnvironments.get().psiManager.dropPsiCaches()
    }

    private class KotlinEnvironment {
        val environment = createKotlinCoreEnvironment()
        val psiFileFactory = environment.project.getService(PsiFileFactory::class.java) as PsiFileFactoryImpl
        val psiManager: PsiManager = environment.project.getService(PsiManager::class.java)

        private fun createKotlinCoreEnvironment(): KotlinCoreEnvironment {
            val configuration = CompilerConfiguration()
            configuration.put(CommonConfigurationKeys.MODULE_NAME, JvmProtoBufUtil.DEFAULT_MODULE_NAME)
            configuration.put(
                CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
                MessageCollector.Companion.NONE
            )

            return KotlinCoreEnvironment.createForProduction(
                {},
                configuration,
                EnvironmentConfigFiles.JVM_CONFIG_FILES
            )
        }
    }
}

