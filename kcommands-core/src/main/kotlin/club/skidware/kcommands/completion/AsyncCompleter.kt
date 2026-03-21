package club.skidware.kcommands.completion

import club.skidware.kcommands.execution.CommandContext
import org.bukkit.plugin.Plugin
import java.util.concurrent.CompletableFuture

/**
 * Tab completer wrapper that executes the delegate's completion logic asynchronously.
 *
 * @param plugin the owning plugin for logging
 * @param delegate the actual completer to invoke off the main thread
 */
class AsyncCompleter(
    private val plugin: Plugin,
    private val delegate: TabCompleter
) : TabCompleter {

    override fun complete(input: String, context: CommandContext): List<String> {
        return try {
            val future = CompletableFuture.supplyAsync {
                delegate.complete(input, context)
            }
            future.get()
        } catch (e: Exception) {
            plugin.logger.warning("[KCommands] Async tab completion failed: ${e.message}")
            emptyList()
        }
    }

    /** Returns a [CompletableFuture] with the completion results for non-blocking usage. */
    fun completeAsync(input: String, context: CommandContext): CompletableFuture<List<String>> {
        return CompletableFuture.supplyAsync {
            delegate.complete(input, context)
        }
    }
}
