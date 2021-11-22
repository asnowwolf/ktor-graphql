package wang.ralph.graphql

data class GraphQLResponseError(
    val message: String,
    val locations: List<GraphQLResponseErrorLocation>? = null,
    val path: List<String>? = null,
    val extensions: Map<String, Any>? = null,
)

data class GraphQLResponseErrorLocation(val line: Int, val column: Int, val sourceName: String? = null)

data class GraphQLResponse<T>(
    val data: T? = null,
    val errors: List<GraphQLResponseError>? = null,
    val extensions: List<Map<*, *>>? = null,
)
