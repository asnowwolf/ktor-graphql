# GraphQL for Ktor library

## What's this

This is a simplified GraphQL library for Ktor applications. It is based on `graphql-java` and `graphql-kotlin-schema-generator` and provides a set of straightforward APIs.

## Usage

### Add dependencies

```
implementation("wang.ralph.common:ktor-graphql:2.1.0")
implementation("com.expediagroup:graphql-kotlin-schema-generator:5.2.0")
```

### Routing configuration

```kotlin
embeddedServer(Netty) {
    configureRouting()
    // configure the `GET /playground` route
    configureGraphQLPlayground()
    // configure the `POST /graphql` route
    configureGraphQL(
        // TODO: change to your package names to scan
        packageNames = listOf("wang.ralph"),
        // TODO: change to your queries
        queries = listOf(UserQuery()),
        // TODO: change to your mutations
        mutations = listOf(UserMutation()),
        // TODO: could be changed to your custom scalar map
        scalars = Scalars.all, // (default)
    )
    configureMonitoring()
    // Currently, only the `jackson` can be used as serializer
    configureSerialization()
    configureRouting()
}.start(wait = true)

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        jackson()
    }
}

fun Application.configureRouting() {
    routing {
        graphqlSchema()
        graphql()
    }
}
```

### Write queries and mutations

```kotlin
class UserQuery {
    fun users(): List<User> = users
}
class UserMutation {
    fun createUser(user: User): User {
        return user.copy(id = ID("9"))
    }
}
```

### Write your custom scalars

```kotlin
object NewTypeCoercing : Coercing<NewType, String> {
    // ...
}

// GraphQLScalarType MUST be a singleton
val newType: GraphQLScalarType = GraphQLScalarType.newScalar()
    .name("NewType")
    .description("NewType description")
    .coercing(NewTypeCoercing)
    .build()

configureGraphQL(
    // ...
    scalars = Scalars.all + mapOf(NewType::class to NewType)
)

```

Then add them to the `queries` or `mutations` list of the `conigureGraphQL`. Both of these classes are POJOs, and their top-level methods will become the names of GraphQL's Query or Mutation.

The classes referenced in the parameter list must be located in the package or sub-packages specified by `packageNames`.

## Notes

1. Currently, only the `jackson` can be used as serializer.
2. Don't write `packageNames = listOf("")"`. Although it works, it will slow down the startup (because too many packages need to be scanned).
3. The value of the `GraphQLScalarType`.`name` for custom scalar type must be UNIQUE, otherwise they cannot be registered at the same time.
4. When customizing a scalar type, the instance of `GraphQLScalarType` used for registration must be a singleton. It must not be created every time it is used (such as obtaining a new instance through a function)

## Feedback

This project is located at <https://github.com/asnowwolf/ktor-graphql>, feel free to submit issues or pull requests.
