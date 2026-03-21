package club.skidware.kcommands.argument.resolvers

import club.skidware.kcommands.argument.ArgumentException
import club.skidware.kcommands.argument.ArgumentResolver
import club.skidware.kcommands.execution.CommandContext

/** Resolver that parses string input into a [Float]. */
object FloatResolver : ArgumentResolver<Float> {
    override fun resolve(input: String, context: CommandContext): Float =
        input.toFloatOrNull() ?: throw ArgumentException("'$input' is not a valid decimal number")
}
