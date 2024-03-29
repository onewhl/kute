package writers

import ModuleInfo
import ProjectInfo
import ResultWriter
import SourceClassInfo
import TestClassInfo
import TestMethodInfo
import dao.ModulesTable
import dao.ProjectsTable
import dao.SourceClassesTable
import dao.SourceMethodsTable
import dao.TestClassesTable
import dao.TestMethodsTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.reflect.KProperty0

private const val BATCH_SIZE = 100

/**
 * Writes results in database if [OutputType.SQLITE] is chosen.
 */
class DBResultWriter(connectionString: String) : ResultWriter {
    private val batch = ArrayList<TestMethodInfo>(BATCH_SIZE)
    private var lastRecordedValues: LastRecordedValues = LastRecordedValues()

    init {
        Database.connect(connectionString)
        transaction {
            SchemaUtils.create(
                TestMethodsTable,
                TestClassesTable,
                SourceMethodsTable,
                SourceClassesTable,
                ProjectsTable,
                ModulesTable
            )
        }
    }

    override fun writeTestMethod(method: TestMethodInfo) {
        batch.add(method)
        if (batch.size == BATCH_SIZE) {
            writeBatch(batch)
            batch.clear()
        }
    }

    override fun writeTestMethods(methods: List<TestMethodInfo>) {
        if (methods.isNotEmpty()) {
            writeBatch(methods)
        }
    }

    private fun writeBatch(batch: List<TestMethodInfo>) {
        transaction {
            if (batch[0].classInfo.projectInfo != lastRecordedValues.projectInfo.value) {
                lastRecordedValues.moduleIds.clear()
            }
            batch.forEach { testMethodInfo ->
                val classInfo = testMethodInfo.classInfo
                val sourceClass = classInfo.sourceClass
                val moduleInfo = classInfo.moduleInfo
                val projectInfo = moduleInfo.projectInfo
                val sourceMethod = testMethodInfo.sourceMethod

                val projectId = insertIfNew(projectInfo, lastRecordedValues::projectInfo) {
                    ProjectsTable.insert {
                        it[name] = projectInfo.name
                        it[buildSystem] = projectInfo.buildSystem
                        it[path] = projectInfo.path
                    } get ProjectsTable.id
                }

                val moduleId = insertModuleIfNew(moduleInfo, projectId)

                val sourceClassId = sourceClass?.let { source ->
                    val sourceModuleId = insertModuleIfNew(source.moduleInfo, projectId)

                    insertIfNew(source, lastRecordedValues::sourceClassInfo) {
                        SourceClassesTable.insert {
                            it[name] = source.name
                            it[pkg] = source.pkg
                            it[language] = source.language
                            it[module] = sourceModuleId
                        } get SourceClassesTable.id
                    }
                }

                val testClassId = insertIfNew(classInfo, lastRecordedValues::classInfo) { testClassInfo ->
                    TestClassesTable.insert {
                        it[name] = testClassInfo.name
                        it[pkg] = testClassInfo.packageName
                        it[language] = testClassInfo.language
                        it[testFramework] = testClassInfo.testFramework
                        it[project] = projectId
                        it[module] = moduleId
                        sourceClassId?.let { id -> it[TestClassesTable.sourceClass] = id }
                    } get TestClassesTable.id
                }

                val sourceMethodId = sourceMethod?.let { sourceMethodInfo ->
                    SourceMethodsTable.insert {
                        it[name] = sourceMethodInfo.name
                        it[body] = sourceMethodInfo.body
                        sourceClassId?.let { id -> it[SourceMethodsTable.sourceClass] = id }
                    } get SourceMethodsTable.id
                }

                TestMethodsTable.insert {
                    it[name] = testMethodInfo.name
                    it[body] = testMethodInfo.body
                    it[comment] = testMethodInfo.comment
                    it[displayName] = testMethodInfo.displayName
                    it[isParametrised] = testMethodInfo.isParametrised
                    it[isDisabled] = testMethodInfo.isDisabled
                    it[testClass] = testClassId
                    sourceMethodId?.let { id -> it[TestMethodsTable.sourceMethod] = id }
                }
            }
        }
    }

    private fun insertModuleIfNew(moduleInfo: ModuleInfo, projectId: Int): Int =
        lastRecordedValues.moduleIds.getOrPut(moduleInfo.name) {
            ModulesTable.insert {
                it[name] = moduleInfo.name
                it[project] = projectId
            } get ModulesTable.id
        }

    /**
     * Checks if the values is already in DB to avoid entity duplication.
     */
    private fun <T> insertIfNew(
        value: T,
        lastValueAccessor: KProperty0<ValueAndId<T>>,
        insertCommand: (T) -> Int
    ): Int {
        return lastValueAccessor.get().takeIf { it.value == value }?.id
            ?: insertCommand(value).also { id ->
                val lastValue = lastValueAccessor.get()
                lastValue.value = value
                lastValue.id = id
            }
    }

    override fun close() {
        if (batch.isNotEmpty()) {
            writeBatch(batch)
            batch.clear()
        }
    }

    /**
     * Stores last values' ids to avoid duplication.
     *
     * The tool handles methods in the sorted by project/module/class way.
     */
    private data class LastRecordedValues(
        val sourceClassInfo: ValueAndId<SourceClassInfo> = ValueAndId(),
        val classInfo: ValueAndId<TestClassInfo> = ValueAndId(),
        val projectInfo: ValueAndId<ProjectInfo> = ValueAndId(),
        val moduleIds: MutableMap<String, Int> = mutableMapOf()
    )

    private data class ValueAndId<T>(var value: T? = null, var id: Int = 0)
}