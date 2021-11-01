package schema

import KtorBatchLoader
import com.expediagroup.graphql.server.operations.Mutation
import com.expediagroup.graphql.server.operations.Query
import getValueFromBatchLoader
import getValuesFromBatchLoader
import graphql.schema.DataFetchingEnvironment

data class User(val id: String? = null, val name: String, val groupId: String?) {
    suspend fun group(dataFetchingEnvironment: DataFetchingEnvironment): Group? {
        return groupId?.let { dataFetchingEnvironment.getValueFromBatchLoader(GroupBatchLoader::class, it) }
    }

    companion object {
        fun query(ids: List<String>): List<User?> {
            return ids.map { id -> users.find { it.id == id } }
        }
    }
}

class UserBatchLoader : KtorBatchLoader<String, User> {
    override suspend fun load(keys: List<String>): List<User?> {
        return User.query(keys)
    }
}

data class Group(val id: String? = null, val name: String) {
    suspend fun users(dataFetchingEnvironment: DataFetchingEnvironment): List<User?> {
        val userIds = users.filter { it.groupId == id }.mapNotNull { it.id }
        return dataFetchingEnvironment.getValuesFromBatchLoader(UserBatchLoader::class, userIds)
    }

    companion object {
        fun query(ids: List<String>): List<Group?> {
            return ids.map { id -> groups.find { it.id == id } }
        }
    }
}

class GroupBatchLoader : KtorBatchLoader<String, Group> {
    override suspend fun load(keys: List<String>): List<Group?> {
        return Group.query(keys)
    }
}

val groups = mutableListOf(
    Group("1", "group1"),
    Group("2", "group2"),
)
val users = mutableListOf(
    User("11", "中文1", "1"),
    User("12", "user2", "1"),
    User("21", "中文2", "2"),
    User("22", "user2", "2"),
    User("32", "user3", "3"),
)

class UserQuery : Query {
    fun users(): List<User> = users
}

class GroupQuery : Query {
    fun groups(): List<Group> = groups
}

class UserMutation : Mutation {
    fun createUser(user: User): User {
        val newUser = user.copy(id = users.size.toString())
        users.add(newUser)
        return newUser
    }
}
