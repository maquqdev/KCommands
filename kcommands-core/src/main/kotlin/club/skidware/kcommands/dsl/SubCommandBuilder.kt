package club.skidware.kcommands.dsl

import club.skidware.kcommands.argument.ArgumentResolverRegistry
import club.skidware.kcommands.model.CommandNode

/**
 * DSL builder for constructing a subcommand within a [CommandBuilder].
 *
 * Supports nested subcommands, aliases, permissions, and default handlers.
 */
class SubCommandBuilder(
    private val name: String,
    private val resolverRegistry: ArgumentResolverRegistry
) {
    private val aliases = mutableListOf<String>()
    var permission: String? = null
    var description: String? = null
    private val defaultBuilders = mutableListOf<DefaultBuilder>()
    private val subBuilders = mutableListOf<SubCommandBuilder>()

    fun aliases(vararg names: String) {
        aliases.addAll(names)
    }

    fun default(block: DefaultBuilder.() -> Unit) {
        val builder = DefaultBuilder(resolverRegistry)
        builder.block()
        defaultBuilders.add(builder)
    }

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
