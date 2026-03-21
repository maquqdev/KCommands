package club.skidware.kcommands.dsl

import club.skidware.kcommands.argument.ArgumentResolverRegistry
import club.skidware.kcommands.model.CommandNode

@DslMarker
annotation class KCommandsDsl

/**
 * Top-level DSL scope for defining commands programmatically.
 *
 * Used inside [KCommands.register] with the DSL overload.
 */
@KCommandsDsl
class CommandDslScope(
    private val resolverRegistry: ArgumentResolverRegistry
) {
    internal val commands = mutableListOf<CommandNode>()

    /**
     * Defines a new command with the given [name].
     *
     * @param name the command name (e.g., "teleport")
     * @param block builder block for configuring the command
     */
    fun command(name: String, block: CommandBuilder.() -> Unit) {
        val builder = CommandBuilder(name, resolverRegistry)
        builder.block()
        commands.add(builder.build())
    }
}
