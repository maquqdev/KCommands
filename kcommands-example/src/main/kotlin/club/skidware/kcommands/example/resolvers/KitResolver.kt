package club.skidware.kcommands.example.resolvers

import club.skidware.kcommands.argument.ArgumentException
import club.skidware.kcommands.argument.ArgumentResolver
import club.skidware.kcommands.execution.CommandContext

class KitResolver : ArgumentResolver<Kit> {
    private val kits = mapOf(
        "starter" to Kit("starter", listOf("STONE_SWORD", "LEATHER_CHESTPLATE", "BREAD:16")),
        "pvp" to Kit("pvp", listOf("DIAMOND_SWORD", "DIAMOND_CHESTPLATE", "GOLDEN_APPLE:5")),
        "builder" to Kit("builder", listOf("DIAMOND_PICKAXE", "OAK_PLANKS:64", "COBBLESTONE:64")),
        "admin" to Kit("admin", listOf("NETHERITE_SWORD", "NETHERITE_CHESTPLATE", "ENCHANTED_GOLDEN_APPLE:64"))
    )

    override fun resolve(input: String, context: CommandContext): Kit {
        return kits[input.lowercase()]
            ?: throw ArgumentException("Kit '$input' not found! Available: ${kits.keys.joinToString()}")
    }

    override fun complete(input: String, context: CommandContext): List<String> {
        return kits.keys.filter { it.startsWith(input.lowercase()) }
    }
}
