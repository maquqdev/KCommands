package club.skidware.kcommands.registration

import club.skidware.kcommands.execution.CommandDispatcher
import club.skidware.kcommands.model.CommandNode
import org.bukkit.plugin.Plugin

/** [CommandRegistrar] implementation that registers commands via Bukkit's CommandMap injection. */
class BukkitRegistrar : CommandRegistrar {

    private val injector = CommandMapInjector()

    override fun register(plugin: Plugin, node: CommandNode, dispatcher: CommandDispatcher) {
        injector.inject(plugin, node, dispatcher)
        plugin.logger.info("[KCommands] Registered command '/${node.name}' via CommandMap")
    }

    override fun unregister(plugin: Plugin, node: CommandNode) {
        injector.remove(plugin, node)
        plugin.logger.info("[KCommands] Unregistered command '/${node.name}'")
    }

    override fun unregisterAll(plugin: Plugin) {
        injector.removeAll(plugin)
        plugin.logger.info("[KCommands] Unregistered all commands")
    }
}
