package wang.ralph.graphql.models

import com.expediagroup.graphql.generator.scalars.ID
import graphql.schema.DataFetchingEnvironment
import wang.ralph.graphql.call
import wang.ralph.graphql.schema.users

data class User(val id: ID? = null, val name: String, val groupId: ID?) {
    fun group(): Group? {
        return groupId?.let { Group.get(groupId) }
    }

    fun token(env: DataFetchingEnvironment) = env.call.request.headers["X-TOKEN"]

    companion object {
        fun query(ids: List<ID>): List<User?> {
            return ids.map(::get)
        }

        fun get(id: ID) = users.find { it.id == id }
    }
}
