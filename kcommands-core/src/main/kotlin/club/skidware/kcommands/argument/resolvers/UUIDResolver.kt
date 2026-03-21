package club.skidware.kcommands.argument.resolvers

import club.skidware.kcommands.argument.ArgumentException
import club.skidware.kcommands.argument.ArgumentResolver
import club.skidware.kcommands.execution.CommandContext
import java.util.UUID

/** Resolver that parses string input into a [UUID]. */
object UUIDResolver : ArgumentResolver<UUID> {
    override fun resolve(input: String, context: CommandContext): UUID =
        try {
            UUID.fromString(input)
        } catch (e: IllegalArgumentException) {
            throw ArgumentException("'$input' is not a valid UUID")
        }
}
