package parsers

import TestMethodInfo
import java.io.File

interface Parser {
    fun process(files: List<File>): List<TestMethodInfo>
    val language: Lang
}