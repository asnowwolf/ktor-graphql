package wang.ralph.graphql

class GraphQLPlayground {
    fun html(graphQLEndpoint: String = "graphql", subscriptionsEndpoint: String = "subscriptions") =
        this::class.java.getResourceAsStream("playground.html")!!
            .reader(Charsets.UTF_8).readText()
            .replace("\${graphQLEndpoint}", graphQLEndpoint)
            .replace("\${subscriptionsEndpoint}", subscriptionsEndpoint)
}
