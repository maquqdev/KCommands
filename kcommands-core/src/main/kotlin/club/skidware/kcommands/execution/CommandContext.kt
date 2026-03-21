package club.skidware.kcommands.execution

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * Holds all context for a single command execution, including the sender,
 * raw arguments, parsed arguments, and flags.
 *
 * @property sender the entity or console that executed the command
 * @property label the command label as typed (may be an alias)
 * @property rawArgs the raw string arguments passed to the command
 * @property parsedArgs map of resolved argument values keyed by argument name
 * @property flags map of resolved flag values keyed by flag name
 */
data class CommandContext(
    val sender: CommandSender,
    val label: String,
    val rawArgs: Array<String>,
    val parsedArgs: MutableMap<String, Any?> = mutableMapOf(),
    val flags: MutableMap<String, Any?> = mutableMapOf()
) {
    /** The sender cast to [Player], or null if the sender is not a player. */
    val player: Player? get() = sender as? Player

    /** Returns true if the sender is a [Player]. */
    val isPlayer: Boolean get() = sender is Player

    /**
     * Retrieves a parsed argument by [name] with an unchecked cast to type [T].
     *
     * @param T the expected argument type
     * @param name the argument name as defined by `@Arg`
     * @return the parsed argument value
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(name: String): T = parsedArgs[name] as T

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CommandContext) return false
        return sender == other.sender && label == other.label && rawArgs.contentEquals(other.rawArgs)
    }

    override fun hashCode(): Int {
        var result = sender.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + rawArgs.contentHashCode()
        return result
    }
}
