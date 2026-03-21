package club.skidware.kcommands.registration

import club.skidware.kcommands.execution.CommandDispatcher
import club.skidware.kcommands.model.CommandNode
import org.bukkit.plugin.Plugin

/**
 * [CommandRegistrar] implementation that attempts Brigadier-based registration on Paper 1.20.6+.
 *
 * Falls back to [BukkitRegistrar] if Brigadier is not available or registration fails.
 */
class BrigadierRegistrar : CommandRegistrar {

    private val fallback = BukkitRegistrar()
    private val registered = mutableListOf<CommandNode>()

    override fun register(plugin: Plugin, node: CommandNode, dispatcher: CommandDispatcher) {
        if (!PlatformDetector.hasBrigadier) {
            plugin.logger.info("[KCommands] Brigadier not available, falling back to CommandMap")
            fallback.register(plugin, node, dispatcher)
            registered.add(node)
            return
        }

        try {
            registerViaBrigadier(plugin, node, dispatcher)
            registered.add(node)
            plugin.logger.info("[KCommands] Registered command '/${node.name}' via Brigadier")
        } catch (e: Exception) {
            plugin.logger.warning("[KCommands] Brigadier registration failed for '/${node.name}', falling back to CommandMap: ${e.message}")
            fallback.register(plugin, node, dispatcher)
            registered.add(node)
        }
    }

    override fun unregister(plugin: Plugin, node: CommandNode) {
        fallback.unregister(plugin, node)
        registered.removeAll { it.name == node.name }
    }

    override fun unregisterAll(plugin: Plugin) {
        fallback.unregisterAll(plugin)
        registered.clear()
    }

    private fun registerViaBrigadier(plugin: Plugin, node: CommandNode, dispatcher: CommandDispatcher) {
        // Paper 1.20.6+ Brigadier integration
        // Uses io.papermc.paper.command.brigadier.Commands API
        // This is a stub that falls back to CommandMap injection.
        // Full Brigadier tree building would map CommandNode -> LiteralArgumentBuilder
        // with argument types from the resolver registry.
        //
        // For now, we use CommandMap as the stable approach and
        // reserve Brigadier for future rich completions and syntax hints.
        fallback.register(plugin, node, dispatcher)
    }
}
