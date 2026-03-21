package club.skidware.kcommands.argument.resolvers

import club.skidware.kcommands.argument.ArgumentException
import club.skidware.kcommands.argument.ArgumentResolver
import club.skidware.kcommands.execution.CommandContext

/** Resolver that parses string input into a [Boolean]. Accepts true/false, yes/no, on/off, 1/0. */
object BooleanResolver : ArgumentResolver<Boolean> {
    override fun resolve(input: String, context: CommandContext): Boolean =
        when (input.lowercase()) {
            "true", "yes", "on", "1" -> true
            "false", "no", "off", "0" -> false
            else -> throw ArgumentException("'$input' is not a valid boolean (true/false)")
        }

    override fun complete(input: String, context: CommandContext): List<String> =
        listOf("true", "false").filter { it.startsWith(input.lowercase()) }
}
