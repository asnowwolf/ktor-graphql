package wang.ralph.graphql

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*

class GraphQLPlayground {
    fun html(graphQLEndpoint: String = "graphql", subscriptionsEndpoint: String = "subscriptions") =
        this::class.java.getResourceAsStream("playground.html")!!
            .reader(Charsets.UTF_8).readText()
            .replace("\${graphQLEndpoint}", graphQLEndpoint)
            .replace("\${subscriptionsEndpoint}", subscriptionsEndpoint)
}

fun Application.configureGraphQLPlayground(
    playgroundUri: String = "/playground",
    graphQLEndpoint: String = "graphql",
    subscriptionsEndpoint: String = "subscriptions",
) {
    routing {
        get(playgroundUri) {
            call.respondText(GraphQLPlayground().html(graphQLEndpoint, subscriptionsEndpoint),
                ContentType.Text.Html.withCharset(Charsets.UTF_8))
        }
    }
}
