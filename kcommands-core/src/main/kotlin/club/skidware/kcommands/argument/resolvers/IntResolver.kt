package club.skidware.kcommands.argument.resolvers

import club.skidware.kcommands.argument.ArgumentException
import club.skidware.kcommands.argument.ArgumentResolver
import club.skidware.kcommands.execution.CommandContext

/** Resolver that parses string input into an [Int]. */
object IntResolver : ArgumentResolver<Int> {
    override fun resolve(input: String, context: CommandContext): Int =
        input.toIntOrNull() ?: throw ArgumentException("'$input' is not a valid integer")
}
