package club.skidware.kcommands.middleware

import club.skidware.kcommands.execution.CommandContext
import club.skidware.kcommands.model.ExecutorInfo
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

/** Middleware that provides async execution support for handlers annotated with `@Async`. */
class AsyncMiddleware(
    private val plugin: Plugin
) : CommandMiddleware {

    override fun before(context: CommandContext, handler: ExecutorInfo): Boolean {
        // The dispatcher handles the async check; this middleware
        // simply flags it. Return true to allow the pipeline to continue.
        return true
    }

    /** Executes the given [action] asynchronously on the Bukkit scheduler. */
    fun executeAsync(context: CommandContext, handler: ExecutorInfo, action: () -> Unit) {
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
            val sender = context.sender
            if (sender is Player && !sender.isOnline) {
                return@Runnable
            }
            try {
                action()
            } catch (e: Exception) {
                plugin.logger.severe("[KCommands] Async command execution failed: ${e.message}")
                e.printStackTrace()
            }
        })
    }

    /** Runs the given [action] on the main server thread. */
    fun runOnMainThread(action: () -> Unit) {
        plugin.server.scheduler.runTask(plugin, Runnable {
            action()
        })
    }
}
