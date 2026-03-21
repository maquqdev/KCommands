package club.skidware.kcommands.model

/**
 * Represents a node in the command tree.
 *
 * Each node can have its own handlers (for execution at this level), child nodes (subcommands),
 * aliases, permissions, and a description.
 *
 * @property name the primary name of this command or subcommand
 * @property aliases alternative names that also match this node
 * @property permission the permission required to use this node, or null for unrestricted
 * @property permissionMessage custom message shown when the sender lacks permission
 * @property description a human-readable description for help output
 * @property handlers the group of executor handlers registered at this node
 * @property children subcommand nodes keyed by their name
 */
data class CommandNode(
    val name: String,
    val aliases: List<String> = emptyList(),
    val permission: String? = null,
    val permissionMessage: String? = null,
    val description: String? = null,
    val handlers: HandlerGroup = HandlerGroup(),
    val children: MutableMap<String, CommandNode> = mutableMapOf()
)
