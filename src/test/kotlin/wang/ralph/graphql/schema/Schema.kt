package wang.ralph.graphql.schema

import com.expediagroup.graphql.generator.scalars.ID
import wang.ralph.graphql.models.Group
import wang.ralph.graphql.models.User
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

val groups = listOf(
    Group(ID("1"), "group1"),
    Group(ID("2"), "group2"),
)
val users = listOf(
    User(ID("11"),
        "中文1",
        ID("1")),
    User(ID("12"),
        "user2",
        ID("1")),
    User(ID("21"),
        "中文2",
        ID("2")),
    User(ID("21"),
        "user2",
        ID("2")),
    User(ID("31"),
        "user3",
        ID("3")),
)

class UserQuery {
    fun users(): List<User> = users
}

class GroupQuery {
    fun groups(): List<Group> = groups
}

class UserMutation {
    fun createUser(user: User): User {
        return user.copy(id = ID("9"))
    }
}

private val defaultInstant = Instant.from(DateTimeFormatter.ISO_INSTANT.parse("2000-01-01T00:00:00Z"))
private val defaultUuid = UUID.fromString("11111111-1111-1111-1111-111111111111")
private val calendar: Calendar
    get() {
        val instance = Calendar.getInstance()
        instance.setTime(Date.from(defaultInstant))
        return instance
    }

data class Log(
    val uuid: UUID = defaultUuid,
    val instant: Instant = defaultInstant,
    val date: Date = Date.from(defaultInstant),
    val calendar: Calendar = wang.ralph.graphql.schema.calendar,
    val bigDecimal: BigDecimal = BigDecimal("1.0"),
    val bigInteger: BigInteger = BigInteger("110181837737166161633331111111111"),
)

class LogQuery {
    fun latestLog(): Log = Log()
}
