package club.skidware.kcommands.dsl

import club.skidware.kcommands.argument.ArgumentResolverRegistry
import club.skidware.kcommands.model.CommandNode

/**
 * DSL builder for constructing a top-level command with handlers, subcommands,
 * permissions, aliases, and descriptions.
 */
class CommandBuilder(
    private val name: String,
    private val resolverRegistry: ArgumentResolverRegistry
) {
    private val aliases = mutableListOf<String>()
    var permission: String? = null
    var permissionMessage: String? = null
    var description: String? = null
    private val defaultBuilders = mutableListOf<DefaultBuilder>()
    private val subBuilders = mutableListOf<SubCommandBuilder>()

    /** Adds alternative command names. */
    fun aliases(vararg names: String) {
        aliases.addAll(names)
    }

    /** Sets the required permission node and optional denial message. */
    fun permission(node: String, message: String? = null) {
        this.permission = node
        this.permissionMessage = message
    }

    /** Sets the command description for help output. */
    fun description(desc: String) {
        this.description = desc
    }

    /** Defines a player-only default handler for this command. */
    fun playerOnly(block: DefaultBuilder.() -> Unit) {
        val builder = DefaultBuilder(resolverRegistry)
        builder.playerOnly = true
        builder.block()
        defaultBuilders.add(builder)
    }

    /** Defines the default handler (no subcommand) for this command. */
    fun default(block: DefaultBuilder.() -> Unit) {
        val builder = DefaultBuilder(resolverRegistry)
        builder.block()
        defaultBuilders.add(builder)
    }

    /** Defines a subcommand with the given [name]. */
    fun sub(name: String, block: SubCommandBuilder.() -> Unit) {
        val builder = SubCommandBuilder(name, resolverRegistry)
        builder.block()
        subBuilders.add(builder)
    }

    internal fun build(): CommandNode {
        val node = CommandNode(
            name = name.lowercase(),
            aliases = aliases.map { it.lowercase() },
            permission = permission,
            permissionMessage = permissionMessage,
            description = description
        )

        for (builder in defaultBuilders) {
            val executor = builder.build()
            if (executor != null) {
                node.handlers.add(executor)
            }
        }

        for (subBuilder in subBuilders) {
            val childNode = subBuilder.build()
            node.children[childNode.name] = childNode
        }

        return node
    }
}
