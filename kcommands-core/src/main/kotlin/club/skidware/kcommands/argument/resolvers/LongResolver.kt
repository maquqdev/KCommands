package club.skidware.kcommands.argument.resolvers

import club.skidware.kcommands.argument.ArgumentException
import club.skidware.kcommands.argument.ArgumentResolver
import club.skidware.kcommands.execution.CommandContext

/** Resolver that parses string input into a [Long]. */
object LongResolver : ArgumentResolver<Long> {
    override fun resolve(input: String, context: CommandContext): Long =
        input.toLongOrNull() ?: throw ArgumentException("'$input' is not a valid number")
}
