package wang.ralph.graphql

import com.expediagroup.graphql.generator.scalars.ID
import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.server.testing.*
import wang.ralph.graphql.models.Group
import wang.ralph.graphql.models.User
import wang.ralph.graphql.schema.UserMutation
import wang.ralph.graphql.schema.UserQuery
import wang.ralph.graphql.schema.groups
import wang.ralph.graphql.schema.users
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GraphQLFeatureTest {
    @Test
    fun fetchPlaygroundHtml() {
        withTestApplication({
            configureGraphQLPlayground()
        }) {
            handleRequest(HttpMethod.Get, "/playground").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Text.Html, response.contentType().withoutParameters())
                assertTrue(response.content!!.startsWith("<!DOCTYPE html>\n<html>"))
            }
        }
    }

    @Test
    fun simpleQuery() {
        withTestApplication({
            configureSerialization()
            configureGraphQL(
                packageNames = listOf(GraphQLFeatureTest::class.java.packageName),
                queries = listOf(UserQuery()),
            )
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
                data class Payload(var users: List<User> = emptyList())

                val payload = Payload(users)
                assertEquals(mapper.writePayload(payload), response.content)
            }
        }
    }

    @Test
    fun nestedQuery() {
        withTestApplication({
            configureSerialization()
            configureGraphQL(
                packageNames = listOf(GraphQLFeatureTest::class.java.packageName),
                queries = listOf(UserQuery()),
            )
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

                data class UserDto(val id: ID?, val name: String, val group: Group?)
                data class Payload(val users: List<UserDto> = emptyList())

                val payload = Payload(users.map {
                    UserDto(it.id, it.name, groups.find { group -> group.id == it.groupId })
                })
                assertEquals(mapper.writePayload(payload), response.content)
            }
        }
    }

    @Test
    fun queryWithRequestHeader() {
        withTestApplication({
            configureSerialization()
            configureGraphQL(
                packageNames = listOf(GraphQLFeatureTest::class.java.packageName),
                queries = listOf(UserQuery()),
            )
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
                data class UserDto(val id: ID?, val name: String, val token: String)
                data class Payload(val users: List<UserDto>)

                val payload = Payload(users.map { UserDto(it.id, it.name, "SECRET") })
                assertEquals(mapper.writePayload(payload), response.content)
            }
        }
    }

    @Test
    fun mutation() {
        withTestApplication({
            configureSerialization()
            configureGraphQL(
                packageNames = listOf(GraphQLFeatureTest::class.java.packageName),
                queries = listOf(UserQuery()),
                mutations = listOf(UserMutation()),
            )
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
                data class UserDto(val id: String)
                data class Payload(val createUser: UserDto)

                val payload = Payload(UserDto("9"))
                assertEquals(mapper.writePayload(payload), response.content)
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

private fun <T> ObjectMapper.writePayload(payload: T): String {
    return writeValueAsString(GraphQLResponse(payload))
}

private fun TestApplicationRequest.setGraphQLQuery(query: String, variables: Map<String, Any>) {
    addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    setBody(mapper.writeValueAsString(GraphQLRequest(query = query.trimMargin(), variables = variables)))
}

data class GraphQLResponse<Payload>(var data: Payload? = null)

fun TestApplicationEngine.sendGraphQLQuery(
    query: String,
    variables: Map<String, Any> = emptyMap(),
    headers: Headers = Headers.Empty,
) =
    handleRequest(HttpMethod.Post, "/graphql", setup = {
        setGraphQLQuery(query, variables)
        headers.forEach { name, values -> values.forEach { value -> addHeader(name, value) } }
    })
