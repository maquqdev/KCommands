package club.skidware.kcommands.completion

import club.skidware.kcommands.execution.CommandContext
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material

/** Pre-built [TabCompleter] instances for common Bukkit types (players, worlds, materials, etc.). */
object BuiltinCompleters {

    val PLAYERS = object : TabCompleter {
        override fun complete(input: String, context: CommandContext): List<String> {
            return Bukkit.getOnlinePlayers()
                .map { it.name }
                .filter { it.lowercase().startsWith(input.lowercase()) }
        }
    }

    val WORLDS = object : TabCompleter {
        override fun complete(input: String, context: CommandContext): List<String> {
            return Bukkit.getWorlds()
                .map { it.name }
                .filter { it.lowercase().startsWith(input.lowercase()) }
        }
    }

    val MATERIALS = object : TabCompleter {
        override fun complete(input: String, context: CommandContext): List<String> {
            return Material.entries
                .map { it.name.lowercase() }
                .filter { it.startsWith(input.lowercase()) }
        }
    }

    val GAME_MODES = object : TabCompleter {
        override fun complete(input: String, context: CommandContext): List<String> {
            return GameMode.entries
                .map { it.name.lowercase() }
                .filter { it.startsWith(input.lowercase()) }
        }
    }

    val BOOLEAN = object : TabCompleter {
        override fun complete(input: String, context: CommandContext): List<String> {
            return listOf("true", "false")
                .filter { it.startsWith(input.lowercase()) }
        }
    }

    val EMPTY = object : TabCompleter {
        override fun complete(input: String, context: CommandContext): List<String> = emptyList()
    }
}
