package club.skidware.kcommands.middleware

import club.skidware.kcommands.execution.CommandContext
import club.skidware.kcommands.model.ExecutorInfo

/**
 * Intercepts command execution before and after the handler is invoked.
 *
 * Middleware runs in order during the `before` phase and in the same order during `after`.
 * Returning `false` from [before] blocks execution and prevents subsequent middleware from running.
 */
interface CommandMiddleware {
    /**
     * Called before the command handler executes.
     *
     * @param context the command execution context
     * @param handler metadata about the handler being invoked
     * @return true to continue execution, false to block it
     */
    fun before(context: CommandContext, handler: ExecutorInfo): Boolean = true

    /**
     * Called after the command handler completes (or fails).
     *
     * @param context the command execution context
     * @param handler metadata about the handler that was invoked
     * @param exception the exception thrown during execution, or null if successful
     */
    fun after(context: CommandContext, handler: ExecutorInfo, exception: Exception? = null) {}
}
