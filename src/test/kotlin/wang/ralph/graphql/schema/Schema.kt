package wang.ralph.graphql.schema

import com.expediagroup.graphql.generator.scalars.ID
import wang.ralph.graphql.Mutation
import wang.ralph.graphql.Query
import wang.ralph.graphql.models.Group
import wang.ralph.graphql.models.User

val groups = listOf(
    Group(ID("1"), "group1"),
    Group(ID("2"), "group2"),
)
val users = listOf(
    User(ID("11"), "中文1", ID("1")),
    User(ID("12"), "user2", ID("1")),
    User(ID("21"), "中文2", ID("2")),
    User(ID("22"), "user2", ID("2")),
    User(ID("32"), "user3", ID("3")),
)

class UserQuery : Query {
    fun users(): List<User> = users
}

class GroupQuery : Query {
    fun groups(): List<Group> = groups
}

class UserMutation : Mutation {
    fun createUser(user: User): User {
        val newUser = user.copy(id = ID("9"))
        return newUser
    }
}
