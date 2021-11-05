# GraphQL for Ktor

## 这是什么？

这是供 Ktor 应用使用的一个简化封装库。它基于 `graphql-java` 和 `graphql-kotlin-schema-generator`，并简化了使用方式。

## 用法

### 添加依赖

```
implementation("wang.ralph.common:ktor-graphql:2.1.0")
implementation("com.expediagroup:graphql-kotlin-schema-generator:5.2.0")
```

### 路由配置

```kotlin
embeddedServer(Netty) {
    configureRouting()
    // 配置 `GET /playground` 路由
    configureGraphQLPlayground()
    // 配置 `POST /graphql` 路由
    configureGraphQL(
        // TODO: 改为你要在其中扫描所涉及类的包名
        packageNames = listOf("wang.ralph"),
        // TODO: 改为你要支持的查询
        queries = listOf(UserQuery()),
        // TODO: 改为你要支持的修改
        mutations = listOf(UserMutation()),
        // TODO: 可以改为你要支持的标量类型映射表
        scalars = Scalars.all, // (default)
        // TODO: 可以改为你要使用的 `/graphql` uri
        graphQLUri = "/graphql", // (default)
    )
    configureMonitoring()
    // 目前，只支持用 `jackson` 作为 serializer
    configureSerialization()
}.start(wait = true)

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        jackson()
    }
}
```

### 编写 Query 和 Mutation

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

### 编写自定义标量

```kotlin
object NewTypeCoercing : Coercing<NewType, String> {
    // ...
}

// 必须是单例
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

然后把它们添加到 `conigureGraphQL` 的 `queries` 或 `mutations` 列表中。这两个类都是 POJO，其顶级方法会变成 GraphQL 的 Query 或 Mutation 的名称。

参数表中所引用的类必须位于 `packageNames` 指定的包或其子包下。

## 注意事项

1. 目前只支持使用 `jackson` 作为 `serializer`
2. 不要使用 `packageNames = listOf("")"` 这样的写法，虽然它能工作，但是会导致启动变慢（因为要扫描太多类）
3. 自定义标量的 GraphQLScalarType.name 属性值不能重复，否则无法同时注册
4. 自定义标量时，用于注册的 GraphQLScalarType 实例必须是单例对象，而不能每次使用时都新建它（比如通过函数获取实例）

## 提出反馈

本项目位于 <https://github.com/asnowwolf/ktor-graphql>，欢迎提出 issue 或 PR。
