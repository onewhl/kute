package writers

import ResultWriter
import TestMethodInfo

class CompositeResultWriter(private val delegates: Array<ResultWriter>): ResultWriter {
    override fun writeTestMethod(method: TestMethodInfo) {
        delegates.forEach { it.writeTestMethod(method) }
    }

    override fun writeTestMethods(methods: List<TestMethodInfo>) {
        delegates.forEach { it.writeTestMethods(methods) }
    }

    override fun writeSynchronized(testMethodInfos: List<TestMethodInfo>) {
        delegates.forEach { it.writeSynchronized(testMethodInfos) }
    }

    override fun close() {
        delegates.forEach { it.close() }
    }
}