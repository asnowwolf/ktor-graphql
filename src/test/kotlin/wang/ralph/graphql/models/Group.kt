package wang.ralph.graphql.models

import com.expediagroup.graphql.generator.scalars.ID
import wang.ralph.graphql.schema.groups
import wang.ralph.graphql.schema.users

data class Group(val id: ID? = null, val name: String) {
    fun users(): List<User?> {
        val userIds = users.filter { it.groupId == id }.mapNotNull { it.id }
        return User.query(userIds)
    }

    companion object {
        fun query(ids: List<ID>): List<Group?> {
            return ids.map(::get)
        }

        fun get(id: ID): Group? {
            return groups.find { it.id == id }
        }
    }
}
