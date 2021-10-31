import com.expediagroup.graphql.generator.SchemaGeneratorConfig
import com.expediagroup.graphql.generator.TopLevelObject
import com.expediagroup.graphql.generator.execution.GraphQLContext
import com.expediagroup.graphql.generator.toSchema
import com.expediagroup.graphql.server.execution.*
import com.expediagroup.graphql.server.operations.Mutation
import com.expediagroup.graphql.server.operations.Query
import com.expediagroup.graphql.server.types.GraphQLServerRequest
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import graphql.GraphQL
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.util.*
import org.dataloader.DataLoaderFactory
import org.dataloader.DataLoaderRegistry

class GraphQLFeature(configuration: Configuration) {
    val queries = configuration.queries
    val mutations = configuration.mutations
    val batchLoaders = configuration.batchLoaders
    val contextGenerator = configuration.contextGenerator
    val graphQLEndpoint = configuration.graphQLEndpoint
    val subscriptionsEndPoint = configuration.subscriptionsEndpoint
    val mapper = configuration.mapper

    class Configuration {
        var queries: List<Query> = emptyList()
        var mutations: List<Mutation> = emptyList()
        var batchLoaders: List<KtorBatchLoader<*, *>> = emptyList()
        var graphQLEndpoint = "graphql"
        var subscriptionsEndpoint = "subscriptions"
        var contextGenerator: (request: ApplicationRequest) -> GraphQLContext? = { null }
        var mapper = jacksonObjectMapper()
    }

    companion object Feature : ApplicationFeature<ApplicationCallPipeline, Configuration, GraphQLFeature> {

        override val key = AttributeKey<GraphQLFeature>("GraphQLFeature")

        override fun install(pipeline: ApplicationCallPipeline, configure: Configuration.() -> Unit): GraphQLFeature {
            val configuration = Configuration().apply(configure)
            val feature = GraphQLFeature(configuration)

            val requestParser = object : GraphQLRequestParser<ApplicationRequest> {
                override suspend fun parseRequest(request: ApplicationRequest): GraphQLServerRequest = try {
                    val rawRequest = request.call.receiveText()
                    @Suppress("BlockingMethodInNonBlockingContext")
                    feature.mapper.readValue(rawRequest, GraphQLServerRequest::class.java)
                } catch (e: JsonMappingException) {
                    e.printStackTrace()
                    throw BadRequestException("Invalid json format")
                } catch (e: JsonProcessingException) {
                    e.printStackTrace()
                    throw BadRequestException("Invalid json format")
                }
            }
            val dataLoaderRegistryFactory = object : DataLoaderRegistryFactory {
                override fun generate(): DataLoaderRegistry {
                    val registry = DataLoaderRegistry()
                    feature.batchLoaders.forEach {
                        registry.register(it::class.qualifiedName,
                            DataLoaderFactory.newDataLoader(KtorBatchLoaderAdaptor(it))
                        )
                    }
                    return registry
                }
            }
            val supportedPackages = (feature.queries + feature.mutations).map { it::class.java.packageName }.distinct()
            val schema = toSchema(
                SchemaGeneratorConfig(supportedPackages),
                feature.queries.map { TopLevelObject(it) },
                feature.mutations.map { TopLevelObject(it) },
            )
            val contextFactory = object : GraphQLContextFactory<GraphQLContext, ApplicationRequest> {
                override suspend fun generateContextMap(request: ApplicationRequest): Map<*, Any>? {
                    val result = feature.contextGenerator(request)
                    return result?.let {
                        val props = result::class.java.fields.associateBy { it.name }
                        return props.keys.associateWith {
                            props[it]?.get(it)
                                ?: throw KotlinNullPointerException("$it in GraphQLContext cannot be null")
                        }
                    }
                }

                override suspend fun generateContext(request: ApplicationRequest): GraphQLContext? {
                    return feature.contextGenerator(request)
                }
            }

            val server = GraphQLServer(
                requestParser,
                contextFactory,
                GraphQLRequestHandler(GraphQL.newGraphQL(schema).build(), dataLoaderRegistryFactory)
            )

            fun buildPlaygroundHtml(): String {
                val resource = this::class.java.getResource("playground.html")
                    ?: throw IllegalStateException("playground.html cannot be found in the classpath")
                return resource.readText()
                    .replace("\${graphQLEndpoint}", feature.graphQLEndpoint)
                    .replace("\${subscriptionsEndpoint}", feature.subscriptionsEndPoint)
            }

            pipeline.intercept(ApplicationCallPipeline.Features) {
                if (call.request.httpMethod == HttpMethod.Post && call.request.uri == "/graphql") {
                    val result = server.execute(call.request) ?: throw BadRequestException("Invalid GraphQL request")

                    @Suppress("BlockingMethodInNonBlockingContext")
                    val json = feature.mapper.writeValueAsString(result)
                    call.respondText(json, ContentType.Application.Json)
                } else if (call.request.httpMethod == HttpMethod.Get && call.request.uri == "/playground") {
                    call.respondText(buildPlaygroundHtml(), ContentType.Text.Html)
                } else {
                    proceed()
                }
            }
            return feature
        }
    }
}
