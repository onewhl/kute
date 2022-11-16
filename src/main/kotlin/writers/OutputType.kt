package writers

enum class OutputType(val value: String) {
    JSON("json"),
    SQLITE("sqlite"),
    CSV("csv")
}