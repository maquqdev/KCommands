package club.skidware.kcommands.middleware

import club.skidware.kcommands.execution.CommandContext
import club.skidware.kcommands.execution.ErrorHandler
import club.skidware.kcommands.model.ExecutorInfo
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player

/** Middleware that blocks execution for `@PlayerOnly` and `@ConsoleOnly` handlers based on sender type. */
class PlayerOnlyMiddleware(
    private val errorHandler: ErrorHandler
) : CommandMiddleware {

    override fun before(context: CommandContext, handler: ExecutorInfo): Boolean {
        if (handler.playerOnly && context.sender !is Player) {
            errorHandler.onPlayerOnly(context)
            return false
        }

        if (handler.consoleOnly && context.sender !is ConsoleCommandSender) {
            errorHandler.onConsoleOnly(context)
            return false
        }

        return true
    }
}
