package club.skidware.kcommands.argument.resolvers

import club.skidware.kcommands.argument.ArgumentException
import club.skidware.kcommands.argument.ArgumentResolver
import club.skidware.kcommands.execution.CommandContext
import org.bukkit.Bukkit
import org.bukkit.World

/** Resolver that looks up a loaded [World] by name. */
object WorldResolver : ArgumentResolver<World> {
    override fun resolve(input: String, context: CommandContext): World =
        Bukkit.getWorld(input)
            ?: throw ArgumentException("World '$input' does not exist")

    override fun complete(input: String, context: CommandContext): List<String> =
        Bukkit.getWorlds()
            .map { it.name }
            .filter { it.lowercase().startsWith(input.lowercase()) }
}
