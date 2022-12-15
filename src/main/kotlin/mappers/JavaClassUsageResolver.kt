package mappers

import SourceClassInfo
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.ObjectCreationExpr

object JavaClassUsageResolver : ClassUsageResolver<CompilationUnit, ClassOrInterfaceDeclaration> {
    override fun isSourceClassUsed(
        testFile: CompilationUnit,
        testClass: ClassOrInterfaceDeclaration,
        sourceClass: SourceClassInfo
    ): Boolean {
        val testPackage = testFile.packageDeclaration.orElse(null) ?: ""
        val differentPackages = testPackage != sourceClass.pkg
        var expectPackageNameOnUsage = differentPackages
        if (differentPackages) {
            val fqcn = if (sourceClass.pkg != "") {
                "${sourceClass.pkg}.${sourceClass.name}"
            } else {
                sourceClass.name
            }

            testFile.imports.forEach { import ->
                val foundMatch: Boolean = if (import.isStatic) {
                    if (import.isAsterisk) {
                        // import static org.test.SourceClass.*
                        fqcn == import.name.asString()
                    } else {
                        // import static org.test.SourceClass.doSmth
                        fqcn == (import.name.qualifier.orElse(null)?.asString() ?: "")
                    }
                } else if (!import.isAsterisk) {
                    // import org.test.SourceClass
                    fqcn == import.name.asString()
                } else {
                    // import org.test.*
                    if (sourceClass.pkg == (import.name.qualifier.orElse(null)?.asString() ?: "")) {
                        expectPackageNameOnUsage = false
                    }
                    false
                }

                if (foundMatch && testFile.types.size == 1) return true
            }
        }
        return testClass.stream()
            .filter { it.hasClassUsage(sourceClass, expectPackageNameOnUsage) }
            .findAny()
            .isPresent
    }

    private fun Node.hasClassUsage(sourceClass: SourceClassInfo, requirePackage: Boolean): Boolean {
        return when (this) {
            is ObjectCreationExpr -> {
                this.type.nameAsString == sourceClass.name &&
                        (!requirePackage || sourceClass.pkg == this.type.scope.orElse(null)?.nameAsString)
            }

            is MethodCallExpr -> {
                val fieldAccessExpr = this.scope.orElse(null) as? FieldAccessExpr
                fieldAccessExpr?.let {
                    it.nameAsString == sourceClass.name &&
                            (!requirePackage || sourceClass.pkg == it.scope?.toString())
                } ?: false
            }

            else -> false
        }
    }
}