import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import schema.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
                assertEquals("""{"data":{"users":[{"id":"11","name":"中文1"},{"id":"12","name":"user2"},{"id":"21","name":"中文2"},{"id":"22","name":"user2"},{"id":"32","name":"user3"}]}}""",
                    response.content)
            }
        }
    }

    @Test
    fun nestedQuery() {
        withTestApplication({
            install(GraphQLFeature) {
                queries = listOf(UserQuery())
                batchLoaders = listOf(GroupBatchLoader())
            }
        }) {
            handleRequest(HttpMethod.Post, "/graphql", setup = {
                setBody("""{"operationName":null,"variables":{},"query":"{\n  users {\n    id\n    name\n  group\n {\n  id\n  name\n}}\n}\n"}""")
            }).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json, response.contentType().withoutParameters())
                assertEquals("""{"data":{"users":[{"id":"11","name":"中文1","group":{"id":"1","name":"group1"}},{"id":"12","name":"user2","group":{"id":"1","name":"group1"}},{"id":"21","name":"中文2","group":{"id":"2","name":"group2"}},{"id":"22","name":"user2","group":{"id":"2","name":"group2"}},{"id":"32","name":"user3","group":null}]}}""",
                    response.content)
            }
        }
    }

    @Test
    fun queryWithSingleDataLoaders() {
        withTestApplication({
            install(GraphQLFeature) {
                queries = listOf(UserQuery())
                batchLoaders = listOf(GroupBatchLoader(), UserBatchLoader())
            }
        }) {
            handleRequest(HttpMethod.Post, "/graphql", setup = {
                setBody("""{"operationName":null,"variables":{},"query":"{\n  users {\n    id\n    name\n  group\n {\n  id\n  name\n}}\n}\n"}""")
            }).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json, response.contentType().withoutParameters())
                assertEquals("""{"data":{"users":[{"id":"11","name":"中文1","group":{"id":"1","name":"group1"}},{"id":"12","name":"user2","group":{"id":"1","name":"group1"}},{"id":"21","name":"中文2","group":{"id":"2","name":"group2"}},{"id":"22","name":"user2","group":{"id":"2","name":"group2"}},{"id":"32","name":"user3","group":null}]}}""",
                    response.content)
            }
        }
    }

    @Test
    fun queryWithManyDataLoaders() {
        withTestApplication({
            install(GraphQLFeature) {
                queries = listOf(GroupQuery())
                batchLoaders = listOf(UserBatchLoader())
            }
        }) {
            handleRequest(HttpMethod.Post, "/graphql", setup = {
                setBody("""{"operationName":null,"variables":{},"query":"{\n  groups {\n    id\n    name\n  users\n {\n  id\n  name\n}}\n}\n"}""")
            }).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json, response.contentType().withoutParameters())
                assertEquals("""{"data":{"groups":[{"id":"1","name":"group1","users":[{"id":"11","name":"中文1"},{"id":"12","name":"user2"}]},{"id":"2","name":"group2","users":[{"id":"21","name":"中文2"},{"id":"22","name":"user2"}]}]}}""",
                    response.content)
            }
        }
    }

    @Test
    fun mutation() {
        withTestApplication({
            install(GraphQLFeature) {
                queries = listOf(UserQuery())
                mutations = listOf(UserMutation())
            }
        }) {
            handleRequest(HttpMethod.Post, "/graphql", setup = {
                setBody("""{"operationName":null,"variables":{"user":{"name": "user4"}},"query":"mutation(${'$'}user: UserInput!) {\n  createUser(user: ${'$'}user) {\n    id\n  }\n}\n"}""")
            }).apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(ContentType.Application.Json, response.contentType().withoutParameters())
                assertEquals("""{"data":{"createUser":{"id":"5"}}}""",
                    response.content)
            }
        }

    }
}
