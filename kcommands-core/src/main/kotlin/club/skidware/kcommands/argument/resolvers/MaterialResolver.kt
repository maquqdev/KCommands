package club.skidware.kcommands.argument.resolvers

import club.skidware.kcommands.argument.ArgumentException
import club.skidware.kcommands.argument.ArgumentResolver
import club.skidware.kcommands.execution.CommandContext
import org.bukkit.Material

/** Resolver that matches string input to a Bukkit [Material]. */
object MaterialResolver : ArgumentResolver<Material> {
    override fun resolve(input: String, context: CommandContext): Material =
        Material.matchMaterial(input)
            ?: throw ArgumentException("'$input' is not a valid material")

    override fun complete(input: String, context: CommandContext): List<String> =
        Material.entries
            .filter { !it.isLegacy }
            .map { it.name.lowercase() }
            .filter { it.startsWith(input.lowercase()) }
            .take(30)
}
