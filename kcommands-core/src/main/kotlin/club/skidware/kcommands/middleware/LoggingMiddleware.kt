package club.skidware.kcommands.middleware

import club.skidware.kcommands.execution.CommandContext
import club.skidware.kcommands.model.ExecutorInfo
import org.bukkit.entity.Player
import java.util.logging.Logger

/** Middleware that logs command executions and errors to the plugin logger. */
class LoggingMiddleware(
    private val logger: Logger
) : CommandMiddleware {

    override fun before(context: CommandContext, handler: ExecutorInfo): Boolean {
        val senderName = if (context.sender is Player) {
            (context.sender as Player).name
        } else {
            "CONSOLE"
        }

        logger.info("[KCommands] $senderName executed /${context.label} ${context.rawArgs.joinToString(" ")}".trim())
        return true
    }

    override fun after(context: CommandContext, handler: ExecutorInfo, exception: Exception?) {
        if (exception != null) {
            val senderName = if (context.sender is Player) {
                (context.sender as Player).name
            } else {
                "CONSOLE"
            }
            logger.warning("[KCommands] Command /${context.label} by $senderName threw: ${exception.message}")
        }
    }
}
