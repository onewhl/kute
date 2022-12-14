package mappers

interface MethodMeta {
    val name: String
    val parameters: List<Any>
    val comment: String
    val body: String

    fun hasMethodCall(sourceMethod: MethodMeta): Boolean
    fun hasAnnotation(name: String): Boolean
    fun getAnnotationValue(name: String, key: String? = null): String?
}
