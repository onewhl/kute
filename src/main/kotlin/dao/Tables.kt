package dao

import BuildSystem
import org.jetbrains.exposed.sql.ReferenceOption.CASCADE
import org.jetbrains.exposed.sql.Table

object TestMethodsTable : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 256)
    var body = text("body")
    var comment = text("comment")
    var displayName = varchar("display_name", 256)
    var isParametrised = bool("is_parametrised")
    val testClass = reference("test_class", TestClassesTable.id, onDelete = CASCADE, onUpdate = CASCADE)
    val sourceMethod = reference("source_method", SourceMethodsTable.id, onDelete = CASCADE, onUpdate = CASCADE)
}

object SourceMethodsTable : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 256)
    var body = text("body")
    val sourceClass = reference("source_class", SourceClassesTable.id, onDelete = CASCADE, onUpdate = CASCADE)
}

object TestClassesTable : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 256)
    val project = reference("project", ProjectsTable.id, onDelete = CASCADE, onUpdate = CASCADE)
    val module = reference("module", ModulesTable.id, onDelete = CASCADE, onUpdate = CASCADE)
    val sourceClass = reference("source_class", SourceClassesTable.id, onDelete = CASCADE, onUpdate = CASCADE)
}

object SourceClassesTable : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 256)
    val module = reference("module", ModulesTable.id, onDelete = CASCADE, onUpdate = CASCADE)
}

object ProjectsTable : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 256)
    val buildSystem = enumerationByName("build_system", 10, BuildSystem::class)
}

object ModulesTable : Table() {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 256)
    val project = reference("project", ProjectsTable.id, onDelete = CASCADE, onUpdate = CASCADE)
}