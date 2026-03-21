package club.skidware.kcommands.registration

import club.skidware.kcommands.execution.CommandDispatcher
import club.skidware.kcommands.model.CommandNode
import org.bukkit.plugin.Plugin

/**
 * Strategy interface for registering and unregistering commands with the server platform.
 *
 * Implementations handle the platform-specific mechanics (e.g., Bukkit CommandMap, Brigadier).
 */
interface CommandRegistrar {

    /** Registers a command [node] with the server, routing execution to the [dispatcher]. */
    fun register(plugin: Plugin, node: CommandNode, dispatcher: CommandDispatcher)

    /** Unregisters a single command [node] from the server. */
    fun unregister(plugin: Plugin, node: CommandNode)

    /** Unregisters all commands registered by this registrar. */
    fun unregisterAll(plugin: Plugin)
}
