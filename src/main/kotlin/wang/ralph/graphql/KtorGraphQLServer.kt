package wang.ralph.graphql

import com.expediagroup.graphql.generator.SchemaGeneratorConfig
import com.expediagroup.graphql.generator.TopLevelObject
import com.expediagroup.graphql.generator.toSchema
import graphql.ExecutionInput
import graphql.GraphQL
import graphql.schema.DataFetchingEnvironment
import graphql.schema.GraphQLScalarType
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import javax.security.auth.login.CredentialNotFoundException
import kotlin.reflect.KClass

lateinit var gql: GraphQL

fun Application.configureGraphQL(
    packageNames: List<String>,
    queries: List<Any> = emptyList(),
    mutations: List<Any> = emptyList(),
    scalars: Map<KClass<*>, GraphQLScalarType> = Scalars.all,
) {
    gql = GraphQL.newGraphQL(toSchema(
        SchemaGeneratorConfig(supportedPackages = packageNames, hooks = KtorSchemaGeneratorHooks(scalars)),
        queries.map { TopLevelObject(it) },
        mutations.map { TopLevelObject(it) },
    )).build()
}

fun Route.graphql(graphQLUri: String = "/graphql"): Route {
    return post(graphQLUri) {
        val request = call.receive<GraphQLRequest>()
        // use localContext to pass through the ApplicationCall
        val result = gql.execute(request.toExecutionBuilder().localContext(this.call))
        call.respond(result.toSpecification())
    }
}

fun Route.graphqlPlayground(
    playgroundUri: String = "/playground",
    graphQLEndpoint: String = "graphql",
    subscriptionsEndpoint: String = "subscriptions",
): Route {
    return get(playgroundUri) {
        call.respondText(GraphQLPlayground().html(graphQLEndpoint, subscriptionsEndpoint),
            ContentType.Text.Html.withCharset(Charsets.UTF_8))
    }
}

fun Route.graphqlSchema(graphQLUri: String = "/graphql"): Route {
    return get(graphQLUri) {
        val request = call.receive<GraphQLRequest>()
        if (isIntrospectionQuery(request)) {
            val result = gql.execute(request.toExecutionBuilder())
            call.respond(result.toSpecification())
        } else {
            throw CredentialNotFoundException()
        }
    }
}

private fun isIntrospectionQuery(request: GraphQLRequest) =
    request.operationName == "IntrospectionQuery" && request.query.startsWith("query IntrospectionQuery {")

fun Route.graphqlAll(
    graphQLUri: String = "/graphql",
    playgroundUri: String = "/playground",
    graphQLEndpoint: String = "graphql",
    subscriptionsEndpoint: String = "subscriptions",
): Route {
    graphqlPlayground(playgroundUri, graphQLEndpoint, subscriptionsEndpoint)
    graphqlSchema(graphQLUri)
    graphql(graphQLUri)
    return this
}


val DataFetchingEnvironment.call: ApplicationCall get() = getLocalContext()
val DataFetchingEnvironment.application: Application get() = call.application

fun GraphQLRequest.toExecutionBuilder(): ExecutionInput.Builder {
    return ExecutionInput.newExecutionInput()
        .operationName(operationName)
        .query(query)
        .variables(variables ?: emptyMap())
}
