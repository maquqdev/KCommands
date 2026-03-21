package club.skidware.kcommands.argument.resolvers

import club.skidware.kcommands.argument.ArgumentException
import club.skidware.kcommands.argument.ArgumentResolver
import club.skidware.kcommands.execution.CommandContext

/** Resolver that parses string input into a [Double]. */
object DoubleResolver : ArgumentResolver<Double> {
    override fun resolve(input: String, context: CommandContext): Double =
        input.toDoubleOrNull() ?: throw ArgumentException("'$input' is not a valid decimal number")
}
