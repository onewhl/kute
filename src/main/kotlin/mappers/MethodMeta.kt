package mappers

interface MethodMeta {
    val name: String
    val parameters: List<Any>
    val comment: String
    val body: String
    val isPublic: Boolean

    fun findLastMethodCall(sourceMethods: Map<String, List<MethodMeta>>): MethodMeta?
    fun hasAnnotation(name: String): Boolean
    fun getAnnotationValue(name: String, key: String? = null): String?
}
