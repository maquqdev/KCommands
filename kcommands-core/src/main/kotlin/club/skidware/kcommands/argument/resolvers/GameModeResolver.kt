package club.skidware.kcommands.argument.resolvers

import club.skidware.kcommands.argument.ArgumentException
import club.skidware.kcommands.argument.ArgumentResolver
import club.skidware.kcommands.execution.CommandContext
import org.bukkit.GameMode

/** Resolver that parses string input into a Bukkit [GameMode]. */
object GameModeResolver : ArgumentResolver<GameMode> {
    override fun resolve(input: String, context: CommandContext): GameMode =
        GameMode.entries.firstOrNull { it.name.equals(input, ignoreCase = true) }
            ?: throw ArgumentException("'$input' is not a valid game mode")

    override fun complete(input: String, context: CommandContext): List<String> =
        GameMode.entries
            .map { it.name.lowercase() }
            .filter { it.startsWith(input.lowercase()) }
}
