package club.skidware.kcommands.model

import kotlin.reflect.KFunction

/** Cooldown configuration for a command handler. */
data class CooldownInfo(val seconds: Int, val message: String, val bypassPermission: String)

/** Confirmation prompt configuration for a command handler. */
data class ConfirmInfo(val message: String, val timeoutSeconds: Int)

/**
 * Metadata describing a single command handler (method) and its parameters,
 * arguments, permissions, and behavioral annotations.
 */
data class ExecutorInfo(
    val function: KFunction<*>,
    val instance: Any?,
    val parameters: List<ParameterInfo>,
    val args: List<ArgInfo>,
    val permission: String? = null,
    val permissionMessage: String? = null,
    val description: String? = null,
    val isAsync: Boolean = false,
    val cooldown: CooldownInfo? = null,
    val confirm: ConfirmInfo? = null,
    val playerOnly: Boolean = false,
    val consoleOnly: Boolean = false
)
