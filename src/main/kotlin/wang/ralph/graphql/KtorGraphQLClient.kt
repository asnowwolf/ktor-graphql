package wang.ralph.graphql

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.net.URL

suspend inline fun <reified T> HttpClient.graphQL(
    url: URL,
    request: GraphQLRequest,
    block: HttpRequestBuilder.() -> Unit = {},
): GraphQLResponse<T> {
    val response: HttpResponse = post(url) {
        contentType(ContentType.Application.Json)
        body = request
        block()
    }
    return response.receive()
}

suspend inline fun <reified T> HttpClient.graphQL(
    url: String,
    request: GraphQLRequest,
    block: HttpRequestBuilder.() -> Unit = {},
): GraphQLResponse<T> {
    return graphQL(URL(url), request, block)
}

suspend inline fun <reified T> HttpClient.graphQL(
    url: URL,
    query: String,
    variables: Map<String, Any?>? = null,
    operationName: String? = null,
    block: HttpRequestBuilder.() -> Unit = {},
): GraphQLResponse<T> {
    return graphQL(url, GraphQLRequest(query, variables, operationName), block)
}

suspend inline fun <reified T> HttpClient.graphQL(
    url: String,
    query: String,
    variables: Map<String, Any?>? = null,
    operationName: String? = null,
    block: HttpRequestBuilder.() -> Unit = {},
): GraphQLResponse<T> {
    return graphQL(URL(url), query, variables, operationName, block)
}
