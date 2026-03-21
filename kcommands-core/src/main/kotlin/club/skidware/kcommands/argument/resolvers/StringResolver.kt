package club.skidware.kcommands.argument.resolvers

import club.skidware.kcommands.argument.ArgumentResolver
import club.skidware.kcommands.execution.CommandContext

/** Resolver that returns the raw string input unchanged. */
object StringResolver : ArgumentResolver<String> {
    override fun resolve(input: String, context: CommandContext): String = input
}
