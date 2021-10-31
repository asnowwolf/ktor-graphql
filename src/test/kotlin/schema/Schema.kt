package schema

import com.expediagroup.graphql.server.execution.KotlinDataLoader
import com.expediagroup.graphql.server.extensions.getValueFromDataLoader
import com.expediagroup.graphql.server.extensions.getValuesFromDataLoader
import com.expediagroup.graphql.server.operations.Query
import graphql.schema.DataFetchingEnvironment
import org.dataloader.DataLoaderFactory
import java.util.concurrent.CompletableFuture

data class User(val id: String, val name: String, val groupId: String) {
    fun group(dataFetchingEnvironment: DataFetchingEnvironment): CompletableFuture<Group> {
        return dataFetchingEnvironment.getValueFromDataLoader(GroupDataLoader.dataLoaderName, groupId)
    }

    companion object {
        fun query(ids: List<String>): List<User> {
            return ids.mapNotNull { id -> users.find { it.id == id } }
        }
    }
}

val UserDataLoader = object : KotlinDataLoader<String, User> {
    override val dataLoaderName = "BATCH_USER_LOADER"
    override fun getDataLoader() = DataLoaderFactory.newDataLoader<String, User> { ids ->
        CompletableFuture.supplyAsync { User.query(ids) }
    }
}

data class Group(val id: String, val name: String) {
    fun users(dataFetchingEnvironment: DataFetchingEnvironment): CompletableFuture<List<User>> {
        val userIds = users.filter { it.groupId == id }.map { it.id }
        return dataFetchingEnvironment.getValuesFromDataLoader(UserDataLoader.dataLoaderName, userIds)
    }

    companion object {
        fun query(ids: List<String>): List<Group> {
            return ids.mapNotNull { id -> groups.find { it.id == id } }
        }
    }
}

val GroupDataLoader = object : KotlinDataLoader<String, Group> {
    override val dataLoaderName = "BATCH_GROUP_LOADER"
    override fun getDataLoader() = DataLoaderFactory.newDataLoader<String, Group> { ids ->
        CompletableFuture.supplyAsync { Group.query(ids) }
    }
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
    fun users(): List<User> = users
}

class GroupQuery : Query {
    fun groups(): List<Group> = groups
}
