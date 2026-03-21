package club.skidware.kcommands.execution

import club.skidware.kcommands.model.CommandNode
import java.time.Duration

/**
 * Handles all error conditions that can occur during command execution.
 *
 * Implement this interface to customize error messages, logging, or behavior
 * for permission failures, invalid arguments, cooldowns, etc.
 */
interface ErrorHandler {
    /** Called when the sender lacks the required [permission]. */
    fun onNoPermission(context: CommandContext, permission: String)

    /** Called when a player-only command is executed by a non-player sender. */
    fun onPlayerOnly(context: CommandContext)

    /** Called when a console-only command is executed by a non-console sender. */
    fun onConsoleOnly(context: CommandContext)

    /** Called when an argument cannot be parsed from the given [input]. */
    fun onInvalidArgument(context: CommandContext, argName: String, input: String, expected: String)

    /** Called when a required argument is not provided. */
    fun onMissingArgument(context: CommandContext, argName: String, usage: String)

    /** Called when a numeric argument [value] falls outside the allowed [range]. */
    fun onOutOfRange(context: CommandContext, argName: String, value: Number, range: ClosedFloatingPointRange<Double>)

    /** Called when the sender is still on cooldown, with the [remaining] duration. */
    fun onCooldown(context: CommandContext, remaining: Duration)

    /** Called when an unhandled exception occurs during command execution. */
    fun onCommandException(context: CommandContext, exception: Exception)

    /** Called to display help/usage information for the given command [node]. */
    fun onShowHelp(context: CommandContext, node: CommandNode)

    /** Called when an unknown subcommand [input] is used, with the list of [available] subcommands. */
    fun onUnknownSubcommand(context: CommandContext, input: String, available: List<String>)
}
