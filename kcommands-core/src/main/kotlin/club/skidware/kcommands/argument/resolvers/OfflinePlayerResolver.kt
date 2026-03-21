package club.skidware.kcommands.argument.resolvers

import club.skidware.kcommands.argument.ArgumentResolver
import club.skidware.kcommands.execution.CommandContext
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer

@Suppress("DEPRECATION")
/** Resolver that looks up an [OfflinePlayer] by name (may not be online). */
object OfflinePlayerResolver : ArgumentResolver<OfflinePlayer> {
    override fun resolve(input: String, context: CommandContext): OfflinePlayer =
        Bukkit.getOfflinePlayer(input)

    override fun complete(input: String, context: CommandContext): List<String> =
        Bukkit.getOnlinePlayers()
            .map { it.name }
            .filter { it.lowercase().startsWith(input.lowercase()) }
}
