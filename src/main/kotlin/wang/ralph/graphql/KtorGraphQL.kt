package wang.ralph.graphql

import com.expediagroup.graphql.generator.SchemaGeneratorConfig
import com.expediagroup.graphql.generator.TopLevelObject
import com.expediagroup.graphql.generator.toSchema
import graphql.ExecutionInput
import graphql.GraphQL
import graphql.schema.DataFetchingEnvironment
import graphql.schema.GraphQLScalarType
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlin.reflect.KClass

lateinit var gql: GraphQL

fun Application.configureGraphQL(
    packageNames: List<String>,
    queries: List<Any> = emptyList(),
    mutations: List<Any> = emptyList(),
    scalars: Map<KClass<*>, GraphQLScalarType> = Scalars.all,
    graphQLUri: String = "/graphql",
) {
    gql = GraphQL.newGraphQL(toSchema(
        SchemaGeneratorConfig(supportedPackages = packageNames, hooks = KtorSchemaGeneratorHooks(scalars)),
        queries.map { TopLevelObject(it) },
        mutations.map { TopLevelObject(it) },
    )).build()

    routing {
        graphql(graphQLUri)
    }
}

fun Route.graphql(graphQLUri: String = "/graphql"): Route {
    return post(graphQLUri) {
        val request = call.receive<GraphQLRequest>()
        // use localContext to pass through the ApplicationCall
        val result = gql.execute(request.toExecutionBuilder().localContext(this.call))
        call.respond(result.toSpecification())
    }
}

val DataFetchingEnvironment.call: ApplicationCall get() = getLocalContext()
val DataFetchingEnvironment.application: Application get() = call.application

fun GraphQLRequest.toExecutionBuilder(): ExecutionInput.Builder {
    return ExecutionInput.newExecutionInput()
        .operationName(operationName)
        .query(query)
        .variables(variables)
}
