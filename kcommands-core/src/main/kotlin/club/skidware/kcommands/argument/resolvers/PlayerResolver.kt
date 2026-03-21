package club.skidware.kcommands.argument.resolvers

import club.skidware.kcommands.argument.ArgumentException
import club.skidware.kcommands.argument.ArgumentResolver
import club.skidware.kcommands.execution.CommandContext
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/** Resolver that looks up an online [Player] by exact name. */
object PlayerResolver : ArgumentResolver<Player> {
    override fun resolve(input: String, context: CommandContext): Player =
        Bukkit.getPlayerExact(input)
            ?: throw ArgumentException("Player '$input' is not online")

    override fun complete(input: String, context: CommandContext): List<String> {
        val alreadyUsedNames = context.parsedArgs.values
            .filterIsInstance<Player>()
            .map { it.name }
            .toSet()

        return Bukkit.getOnlinePlayers()
            .map { it.name }
            .filter { it !in alreadyUsedNames }
            .filter { it.lowercase().startsWith(input.lowercase()) }
    }
}
