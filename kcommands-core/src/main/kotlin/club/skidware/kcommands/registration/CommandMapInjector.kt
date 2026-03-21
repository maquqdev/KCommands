package club.skidware.kcommands.registration

import club.skidware.kcommands.execution.CommandContext
import club.skidware.kcommands.execution.CommandDispatcher
import club.skidware.kcommands.model.CommandNode
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandMap
import org.bukkit.command.CommandSender
import org.bukkit.command.SimpleCommandMap
import org.bukkit.plugin.Plugin

/**
 * Injects commands into the Bukkit [CommandMap] via reflection.
 *
 * Handles runtime registration and unregistration of commands, including
 * cleanup of the internal `knownCommands` map.
 */
class CommandMapInjector {

    private val registeredCommands = mutableMapOf<String, Command>()

    /**
     * Injects a command into the Bukkit command map.
     *
     * @param plugin the owning plugin
     * @param node the command node to register
     * @param dispatcher the dispatcher that handles execution and tab completion
     */
    fun inject(plugin: Plugin, node: CommandNode, dispatcher: CommandDispatcher) {
        val commandMap = getCommandMap()

        val command = object : Command(
            node.name,
            node.description ?: "",
            "/${node.name}",
            node.aliases
        ) {
            init {
                permission = node.permission
                permissionMessage = node.permissionMessage
            }

            override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
                val context = CommandContext(
                    sender = sender,
                    label = commandLabel,
                    rawArgs = args as Array<String>
                )
                return dispatcher.dispatch(context)
            }

            override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): MutableList<String> {
                val context = CommandContext(
                    sender = sender,
                    label = alias,
                    rawArgs = args as Array<String>
                )
                return dispatcher.complete(context).toMutableList()
            }
        }

        commandMap.register(plugin.name.lowercase(), command)
        registeredCommands[node.name.lowercase()] = command

        // Register aliases
        for (alias in node.aliases) {
            registeredCommands[alias.lowercase()] = command
        }
    }

    /** Removes a single command and its aliases from the Bukkit command map. */
    fun remove(plugin: Plugin, node: CommandNode) {
        val commandMap = getCommandMap()

        val command = registeredCommands.remove(node.name.lowercase())
        if (command != null) {
            command.unregister(commandMap)

            // Remove from known commands map via reflection
            try {
                val knownCommandsField = SimpleCommandMap::class.java.getDeclaredField("knownCommands")
                knownCommandsField.isAccessible = true
                @Suppress("UNCHECKED_CAST")
                val knownCommands = knownCommandsField.get(commandMap) as MutableMap<String, Command>
                knownCommands.remove(node.name.lowercase())
                knownCommands.remove("${plugin.name.lowercase()}:${node.name.lowercase()}")
                for (alias in node.aliases) {
                    knownCommands.remove(alias.lowercase())
                    knownCommands.remove("${plugin.name.lowercase()}:${alias.lowercase()}")
                    registeredCommands.remove(alias.lowercase())
                }
            } catch (e: Exception) {
                plugin.logger.warning("[KCommands] Failed to fully unregister command '${node.name}': ${e.message}")
            }
        }
    }

    /** Removes all commands registered by this injector from the Bukkit command map. */
    fun removeAll(plugin: Plugin) {
        val commandMap = getCommandMap()
        try {
            val knownCommandsField = SimpleCommandMap::class.java.getDeclaredField("knownCommands")
            knownCommandsField.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            val knownCommands = knownCommandsField.get(commandMap) as MutableMap<String, Command>

            for ((name, command) in registeredCommands) {
                command.unregister(commandMap)
                knownCommands.remove(name)
                knownCommands.remove("${plugin.name.lowercase()}:$name")
            }
        } catch (e: Exception) {
            plugin.logger.warning("[KCommands] Failed to unregister all commands: ${e.message}")
        }
        registeredCommands.clear()
    }

    private fun getCommandMap(): CommandMap {
        return try {
            val method = Bukkit.getServer().javaClass.getMethod("getCommandMap")
            method.invoke(Bukkit.getServer()) as CommandMap
        } catch (e: Exception) {
            // Fallback via reflection
            val field = Bukkit.getServer().javaClass.getDeclaredField("commandMap")
            field.isAccessible = true
            field.get(Bukkit.getServer()) as CommandMap
        }
    }
}
