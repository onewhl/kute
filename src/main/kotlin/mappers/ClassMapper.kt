package mappers

import ModuleInfo
import SourceClassAndLocation
import SourceClassInfo
import parsers.detectLangByExtension
import java.io.File
import java.util.*

/**
 * Implements heuristics to map test class to source class
 * 1. a. Remove common test class suffix (Test, ITCase, IT, etc) and check if the files with such name exist
 *    b. If a class exists and is used in test, return this class, otherwise see 2.
 * 2. a. Split expected source class name into words (tokens) and generate permutations keeping original order.
 *      For example, for SourceClassResolver the following will be generated:
 *      Source, Class, Resolver, SourceClass, ClassResolver, SourceClassResolver
 *    b. Sort the candidates by length in decreasing order
 *    c. Add to the list If class file exists and the name is used in test
 *    d. From the candidates used in test class, find first one from the same package as test class. If no such class
 *      found, return first in the list.
 * 3. If the list of possible candidates is empty, return null
 */
class ClassMapper(
    private val module: ModuleInfo,
    private val classNameToSourcesMap: Map<String, List<File>>,
    private val packageNameResolver: PackageNameResolver = RegexPackageNameResolver
) {
    fun findSourceClass(testClassMeta: ClassMeta): SourceClassAndLocation? {
        return classNameTokenSubsetHeuristics(testClassMeta)
    }

    private fun classNameTokenSubsetHeuristics(testClassMeta: ClassMeta): SourceClassAndLocation? {
        val testClassNameWithoutTestSuffix = removeSingleTestSuffixOrPrefix(testClassMeta.name)
        val sourceClassCandidates = mutableListOf<SourceClassAndLocation>()
        processClassNameCandidate(testClassNameWithoutTestSuffix, testClassMeta, sourceClassCandidates)
        if (sourceClassCandidates.size == 1) return sourceClassCandidates[0]

        val candidateNames = generateTokenCombinations(testClassNameWithoutTestSuffix) { 1 }
        candidateNames.forEach { classNameCandidate ->
            if (classNameCandidate != testClassNameWithoutTestSuffix) {
                processClassNameCandidate(classNameCandidate, testClassMeta, sourceClassCandidates)
            }
        }

        return sourceClassCandidates.firstOrNull { it.sourceClass.pkg == testClassMeta.packageName }
            ?: sourceClassCandidates.firstOrNull()
    }

    private fun processClassNameCandidate(
        classNameCandidate: String,
        testClassMeta: ClassMeta,
        sink: MutableList<SourceClassAndLocation>
    ) {
        classNameToSourcesMap[classNameCandidate]?.also { matchingFiles ->
            if (matchingFiles.size == 1) {
                val sourceClassAndLocation =
                    createSourceClassInfo(classNameCandidate, matchingFiles[0], testClassMeta)
                if (testClassMeta.hasClassUsage(sourceClassAndLocation)) {
                    sink += sourceClassAndLocation
                }
            } else {
                val candidates = matchingFiles.map { createSourceClassInfo(classNameCandidate, it, testClassMeta) }
                candidates.filterTo(sink) { testClassMeta.hasClassUsage(it) }
            }
        }
    }

    private fun createSourceClassInfo(className: String, file: File, testClassMeta: ClassMeta): SourceClassAndLocation {
        val packageName = if (isSourceClassPackageNameSameAsTest(file, testClassMeta)) {
            testClassMeta.packageName
        } else {
            packageNameResolver.extractPackageName(file)
        }
        return SourceClassAndLocation(SourceClassInfo(
            className, packageName, module, detectLangByExtension(file.extension)
        ), file)
    }

    companion object {
        private val TEST_SUFFIXES = arrayOf("Test", "Tests", "TestCase", "IT", "ITCase")
        private val TEST_PREFIXES = arrayOf("Test", "IT")

        internal fun removeSingleTestSuffixOrPrefix(className: String) = removeSingleTestSuffix(className).let {
            if (it === className) removeSingleTestPrefix(it) else it
        }

        private fun removeSingleTestSuffix(className: String): String {
            TEST_SUFFIXES.forEach { suffix ->
                className.removeSuffix(suffix).let {
                    if (it !== className) return it
                }
            }
            return className
        }

        private fun removeSingleTestPrefix(className: String): String {
            TEST_PREFIXES.forEach { prefix ->
                className.removePrefix(prefix).let {
                    if (it !== className) return it
                }
            }
            return className
        }

        private fun isSourceClassPackageNameSameAsTest(sourceClass: File, expectedPackagePath: String): Boolean =
            sourceClass.parent.endsWith(expectedPackagePath)

        private fun isSourceClassPackageNameSameAsTest(sourceClass: File, testClassMeta: ClassMeta): Boolean =
            isSourceClassPackageNameSameAsTest(
                sourceClass,
                testClassMeta.packageName.replace('.', File.pathSeparatorChar)
            )

        internal fun splitByTokensCamelCase(className: String): List<Int> {
            var beginIndex = 0
            val result = ArrayList<Int>()
            for (i in 1 until className.length) {
                val ch = className[i]
                if (ch.isUpperCase()) {
                    result.add(i - beginIndex)
                    beginIndex = i
                }
            }
            result.add(className.length - beginIndex)
            return result
        }

        internal fun generateTokenCombinations(
            className: String,
            minNumberOfTokensResolver: (List<Int>) -> Int = { 1 }
        ): List<String> {
            val tokens = splitByTokensCamelCase(className)
            val minNumberOfTokens = minNumberOfTokensResolver(tokens)
            require(minNumberOfTokens > 0) { "minNumberOfTokens must be higher than zero" }
            val result = ArrayList<String>()
            val maxLength = className.length
            val sb = StringBuilder(maxLength)
            var charsRemovedFromHead = 0
            for (numItemsRemovedInHead in 0..tokens.size - minNumberOfTokens) {
                sb.setLength(0)
                // add without prefix
                sb.append(className, charsRemovedFromHead, maxLength)
                val maxLengthWithoutPrefix = sb.length
                var charsRemovedFromTail = 0
                for (i in tokens.size - 1 downTo numItemsRemovedInHead + minNumberOfTokens - 1) {
                    // remove suffix
                    sb.setLength(maxLengthWithoutPrefix - charsRemovedFromTail)
                    result.add(sb.toString())
                    charsRemovedFromTail += tokens[i]
                }
                charsRemovedFromHead += tokens[numItemsRemovedInHead]
            }
            Collections.sort(result, Comparator.comparingInt { -it.length })
            return result
        }
    }
}