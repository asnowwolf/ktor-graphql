package wang.ralph.graphql

import com.expediagroup.graphql.generator.exceptions.TypeNotSupportedException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.routing.*
import io.ktor.server.testing.*
import wang.ralph.graphql.schema.LogQuery
import wang.ralph.graphql.schema.UserMutation
import wang.ralph.graphql.schema.UserQuery
import java.text.SimpleDateFormat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class KtorGraphQLServerTest {
    @Test
    fun fetchPlaygroundHtml() {
        withTestApplication({
            routing {
                graphqlPlayground()
            }
        }) {
            handleRequest(HttpMethod.Get, "/playground").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Text.Html, response.contentType().withoutParameters())
                assertEquals(KtorGraphQLServerTest::class.java.getResourceAsStream("playground-expected.html")!!
                    .reader(Charsets.UTF_8)
                    .readText(), response.content)
            }
        }
    }

    @Test
    fun simpleQuery() {
        withTestApplication({
            configureSerialization()
            configureGraphQL(
                packageNames = listOf("wang.ralph.graphql"),
                queries = listOf(UserQuery()),
            )
            routing {
                graphqlSchema()
                graphql()
            }
        }) {
            sendGraphQLQuery("""{
                    |  users {
                    |      id
                    |      name
                    |      groupId
                    |  }
                    |}"""
            ).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json, response.contentType().withoutParameters())
                assertEquals("""{"data":{"users":[{"id":"11","name":"中文1","groupId":"1"},{"id":"12","name":"user2","groupId":"1"},{"id":"21","name":"中文2","groupId":"2"},{"id":"21","name":"user2","groupId":"2"},{"id":"31","name":"user3","groupId":"3"}]}}""",
                    response.content)
            }
        }
    }

    @Test
    fun predefinedCustomDataType() {
        withTestApplication({
            configureSerialization()
            configureGraphQL(
                packageNames = listOf("wang.ralph.graphql"),
                queries = listOf(LogQuery()),
            )
            routing {
                graphqlSchema()
                graphql()
            }
        }) {
            sendGraphQLQuery("""{
                    |  latestLog {
                    |       uuid
                    |       instant
                    |       date
                    |       calendar
                    |       bigDecimal
                    |       bigInteger
                    |  }
                    |}"""
            ).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json, response.contentType().withoutParameters())
                assertEquals("""{"data":{"latestLog":{"uuid":"11111111-1111-1111-1111-111111111111","instant":"2000-01-01T00:00:00Z","date":"2000-01-01T00:00:00Z","calendar":"2000-01-01T00:00:00Z","bigDecimal":"1.0","bigInteger":"110181837737166161633331111111111"}}}""",
                    response.content)
            }
        }
    }

    @Test
    fun missingCustomDataTypes() {
        assertFailsWith(TypeNotSupportedException::class) {
            withTestApplication({
                configureSerialization()
                configureGraphQL(
                    packageNames = listOf("wang.ralph.graphql"),
                    queries = listOf(LogQuery()),
                    scalars = emptyMap()
                )
                routing {
                    graphqlSchema()
                    graphql()
                }
            }) {
                sendGraphQLQuery("""{
                    |  latestLog {
                    |       id
                    |       time
                    |  }
                    |}"""
                )
            }
        }
    }

    @Test
    fun nestedQuery() {
        withTestApplication({
            configureSerialization()
            configureGraphQL(
                packageNames = listOf("wang.ralph.graphql"),
                queries = listOf(UserQuery()),
            )
            routing {
                graphqlSchema()
                graphql()
            }
        }) {
            sendGraphQLQuery("""{
                    |  users {
                    |      id
                    |      name
                    |      group {
                    |           id
                    |           name
                    |      }
                    |  }
                    |}"""
            ).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json, response.contentType().withoutParameters())
                assertEquals("""{"data":{"users":[{"id":"11","name":"中文1","group":{"id":"1","name":"group1"}},{"id":"12","name":"user2","group":{"id":"1","name":"group1"}},{"id":"21","name":"中文2","group":{"id":"2","name":"group2"}},{"id":"21","name":"user2","group":{"id":"2","name":"group2"}},{"id":"31","name":"user3","group":null}]}}""",
                    response.content)
            }
        }
    }

    @Test
    fun queryByRequestHeader() {
        withTestApplication({
            configureSerialization()
            configureGraphQL(
                packageNames = listOf("wang.ralph.graphql"),
                queries = listOf(UserQuery()),
            )
            routing {
                graphqlSchema()
                graphql()
            }
        }) {
            sendGraphQLQuery(
                """{
                    |  users {
                    |      id
                    |      name
                    |      token
                    |  }
                    |}""",
                headers = headersOf("X-TOKEN", "SECRET")
            ).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json, response.contentType().withoutParameters())
                assertEquals("""{"data":{"users":[{"id":"11","name":"中文1","token":"SECRET"},{"id":"12","name":"user2","token":"SECRET"},{"id":"21","name":"中文2","token":"SECRET"},{"id":"21","name":"user2","token":"SECRET"},{"id":"31","name":"user3","token":"SECRET"}]}}""",
                    response.content)
            }
        }
    }

    @Test
    fun mutation() {
        withTestApplication({
            configureSerialization()
            configureGraphQL(
                packageNames = listOf("wang.ralph.graphql"),
                queries = listOf(UserQuery()),
                mutations = listOf(UserMutation()),
            )
            routing {
                graphqlSchema()
                graphql()
            }
        }) {
            sendGraphQLQuery(
                query = """mutation(${'$'}user: UserInput!) {
                    |  createUser(user: ${'$'}user) {
                    |      id
                    |  }
                    |}""".trimMargin(),
                variables = mapOf("user" to mapOf("name" to "user4")),
            ).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json, response.contentType().withoutParameters())
                assertEquals("""{"data":{"createUser":{"id":"9"}}}""", response.content)
            }
        }
    }
}

private fun Application.configureSerialization() {
    install(ContentNegotiation) {
        jackson {
        }
    }
}

val mapper = ObjectMapper()
    .registerModule(JavaTimeModule())
    .setDateFormat(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"))

private fun TestApplicationRequest.setGraphQLQuery(query: String, variables: Map<String, Any>) {
    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    setBody(mapper.writeValueAsString(GraphQLRequest(query = query.trimMargin(), variables = variables)))
}

fun TestApplicationEngine.sendGraphQLQuery(
    query: String,
    variables: Map<String, Any> = emptyMap(),
    headers: Headers = Headers.Empty,
) = handleRequest(HttpMethod.Post, "/graphql", setup = {
    setGraphQLQuery(query, variables)
    headers.forEach { name, values -> values.forEach { value -> addHeader(name, value) } }
})
