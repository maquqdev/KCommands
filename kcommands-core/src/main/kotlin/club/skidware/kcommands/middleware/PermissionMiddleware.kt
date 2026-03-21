package club.skidware.kcommands.middleware

import club.skidware.kcommands.execution.CommandContext
import club.skidware.kcommands.execution.ErrorHandler
import club.skidware.kcommands.model.ExecutorInfo

/** Middleware that blocks execution if the sender lacks the handler's required permission. */
class PermissionMiddleware(
    private val errorHandler: ErrorHandler
) : CommandMiddleware {

    override fun before(context: CommandContext, handler: ExecutorInfo): Boolean {
        val permission = handler.permission ?: return true

        if (!context.sender.hasPermission(permission)) {
            errorHandler.onNoPermission(context, permission)
            return false
        }

        return true
    }
}
