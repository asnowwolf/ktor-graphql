import com.expediagroup.graphql.server.operations.Query
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

data class Group(val id: String, val name: String) {
    fun users(): List<User> = users.filter { it.groupId == id }
}

data class User(val id: String, val name: String, val groupId: String) {
    fun group(): Group? = groups.find { it.id == groupId }
}

val groups = listOf(
    Group("1", "group1"),
    Group("2", "group2"),
)
val users = listOf(
    User("11", "中文1", "1"),
    User("12", "user2", "1"),
    User("21", "中文2", "2"),
    User("22", "user2", "2"),
)

class UserQuery : Query {
    fun users(): List<User> {
        return users
    }
}

class GraphQLFeatureTest {
    @Test
    fun fetchPlaygroundHtml() {
        withTestApplication({
            install(GraphQLFeature) {
                queries = listOf(UserQuery())
            }
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
            install(GraphQLFeature) {
                queries = listOf(UserQuery())
            }
        }) {
            handleRequest(HttpMethod.Post, "/graphql", setup = {
                setBody("""{"operationName":null,"variables":{},"query":"{\n  users {\n    id\n    name\n  }\n}\n"}""")
            }).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json, response.contentType().withoutParameters())
                assertEquals("""{"data":{"users":[{"id":"11","name":"中文1"},{"id":"12","name":"user2"},{"id":"21","name":"中文2"},{"id":"22","name":"user2"}]}}""",
                    response.content)
            }
        }
    }

    @Test
    fun nestedQuery() {
        withTestApplication({
            install(GraphQLFeature) {
                queries = listOf(UserQuery())
            }
        }) {
            handleRequest(HttpMethod.Post, "/graphql", setup = {
                setBody("""{"operationName":null,"variables":{},"query":"{\n  users {\n    id\n    name\n  group\n {\n  id\n  name\n}}\n}\n"}""")
            }).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json, response.contentType().withoutParameters())
                assertEquals("""{"data":{"users":[{"id":"11","name":"中文1","group":{"id":"1","name":"group1"}},{"id":"12","name":"user2","group":{"id":"1","name":"group1"}},{"id":"21","name":"中文2","group":{"id":"2","name":"group2"}},{"id":"22","name":"user2","group":{"id":"2","name":"group2"}}]}}""",
                    response.content)
            }
        }
    }
}
