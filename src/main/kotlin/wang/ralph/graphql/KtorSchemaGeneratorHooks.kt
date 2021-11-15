package wang.ralph.graphql

import com.expediagroup.graphql.generator.hooks.SchemaGeneratorHooks
import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.CoercingSerializeException
import graphql.schema.GraphQLScalarType
import graphql.schema.GraphQLType
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KType

private fun Instant.toISOString(): String {
    return DateTimeFormatter.ISO_INSTANT.format(this)
}

object InstantCoercing : Coercing<Instant, String> {
    override fun parseValue(input: Any): Instant {
        return Instant.parse(serialize(input))
    }

    override fun parseLiteral(input: Any): Instant {
        val stringValue = (input as StringValue).value
        return stringValue?.let { parseValue(it) } ?: throw CoercingSerializeException("Invalid DateTime: $stringValue")
    }

    override fun serialize(dataFetcherResult: Any): String {
        return (dataFetcherResult as Instant).toISOString()
    }
}

object DateCoercing : Coercing<Date, String> {
    override fun parseValue(input: Any): Date {
        return Date.from(Instant.parse(serialize(input)))
    }

    override fun parseLiteral(input: Any): Date {
        val stringValue = (input as StringValue).value
        return stringValue?.let { parseValue(it) } ?: throw CoercingSerializeException("Invalid DateTime: $stringValue")
    }

    override fun serialize(dataFetcherResult: Any): String {
        return (dataFetcherResult as Date).toInstant().toISOString()
    }
}

object CalendarCoercing : Coercing<Calendar, String> {
    override fun parseValue(input: Any): Calendar {
        val calendar = Calendar.getInstance()
        calendar.time = Date.from(Instant.parse(serialize(input)))
        return calendar
    }

    override fun parseLiteral(input: Any): Calendar {
        val stringValue = (input as StringValue).value
        return stringValue?.let { parseValue(it) }
            ?: throw CoercingSerializeException("Invalid CalendarTime: $stringValue")
    }

    override fun serialize(dataFetcherResult: Any): String {
        return (dataFetcherResult as Calendar).toInstant().toISOString()
    }
}

object BigDecimalCoercing : Coercing<BigDecimal, String> {
    override fun parseValue(input: Any): BigDecimal {
        return BigDecimal(serialize(input))
    }

    override fun parseLiteral(input: Any): BigDecimal {
        val stringValue = (input as StringValue).value
        return stringValue?.let { parseValue(it) }
            ?: throw CoercingSerializeException("Invalid BigDecimal: $stringValue")
    }

    override fun serialize(dataFetcherResult: Any): String {
        return dataFetcherResult.toString()
    }
}

object BigIntegerCoercing : Coercing<BigInteger, String> {
    override fun parseValue(input: Any): BigInteger {
        return BigInteger(serialize(input))
    }

    override fun parseLiteral(input: Any): BigInteger {
        val stringValue = (input as StringValue).value
        return stringValue?.let { parseValue(it) }
            ?: throw CoercingSerializeException("Invalid BigInteger: $stringValue")
    }

    override fun serialize(dataFetcherResult: Any): String {
        return (dataFetcherResult as BigInteger).toString()
    }
}

object UUIDCoercing : Coercing<UUID, String> {
    override fun parseValue(input: Any): UUID {
        return UUID.fromString(serialize(input))
    }

    override fun parseLiteral(input: Any): UUID {
        val uuidString = (input as? StringValue)?.value
        return UUID.fromString(uuidString)
    }

    override fun serialize(dataFetcherResult: Any): String = dataFetcherResult.toString()
}

object Scalars {
    val instant: GraphQLScalarType = GraphQLScalarType.newScalar()
        .name("Instant")
        .description("A type representing a formatted ISO-8601 DateTime")
        .coercing(InstantCoercing)
        .build()

    val date: GraphQLScalarType = GraphQLScalarType.newScalar()
        .name("Date")
        .description("A type representing a formatted ISO-8601 DateTime")
        .coercing(DateCoercing)
        .build()

    val calendar: GraphQLScalarType = GraphQLScalarType.newScalar()
        .name("Calendar")
        .description("A type representing a formatted ISO-8601 DateTime")
        .coercing(CalendarCoercing)
        .build()

    val bigDecimal: GraphQLScalarType = GraphQLScalarType.newScalar()
        .name("BigDecimal")
        .description("A type representing a BigDecimal")
        .coercing(BigDecimalCoercing)
        .build()

    val bigInteger: GraphQLScalarType = GraphQLScalarType.newScalar()
        .name("BigInteger")
        .description("A type representing a BigInteger")
        .coercing(BigIntegerCoercing)
        .build()

    val uuid: GraphQLScalarType = GraphQLScalarType.newScalar()
        .name("UUID")
        .description("A type representing a formatted java.util.UUID")
        .coercing(UUIDCoercing)
        .build()

    val all: Map<KClass<*>, GraphQLScalarType> =
        mapOf(
            Instant::class to instant,
            Date::class to date,
            BigDecimal::class to bigDecimal,
            BigInteger::class to bigInteger,
            UUID::class to uuid,
            Calendar::class to calendar,
        )
}

class KtorSchemaGeneratorHooks(private val scalars: Map<KClass<*>, GraphQLScalarType>) : SchemaGeneratorHooks {
    override fun willGenerateGraphQLType(type: KType): GraphQLType? {
        if (type.classifier !is KClass<*>) {
            return null
        }
        return scalars[type.classifier]
    }
}
