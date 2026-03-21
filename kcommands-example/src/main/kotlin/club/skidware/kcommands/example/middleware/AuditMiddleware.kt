package club.skidware.kcommands.example.middleware

import club.skidware.kcommands.execution.CommandContext
import club.skidware.kcommands.middleware.CommandMiddleware
import club.skidware.kcommands.model.ExecutorInfo
import java.util.logging.Logger

class AuditMiddleware(private val logger: Logger) : CommandMiddleware {
    override fun before(context: CommandContext, handler: ExecutorInfo): Boolean {
        logger.info("[Audit] ${context.sender.name} executing /${context.label} ${context.rawArgs.joinToString(" ")}")
        return true
    }

    override fun after(context: CommandContext, handler: ExecutorInfo, exception: Exception?) {
        if (exception != null) {
            logger.warning("[Audit] Command failed: ${exception.message}")
        }
    }
}
