package club.skidware.kcommands.execution

import club.skidware.kcommands.help.UsageFormatter
import club.skidware.kcommands.message.DefaultMessageProvider
import club.skidware.kcommands.message.IMessageProvider
import club.skidware.kcommands.model.CommandNode
import net.kyori.adventure.text.minimessage.MiniMessage
import java.time.Duration

/**
 * Default [ErrorHandler] implementation that sends MiniMessage-formatted error messages to the sender.
 *
 * Uses the configured [IMessageProvider] for all message templates with placeholder substitution.
 *
 * @param messageProvider the message provider for error message templates
 */
class DefaultErrorHandler(
    private val messageProvider: IMessageProvider = DefaultMessageProvider()
) : ErrorHandler {

    private val mm = MiniMessage.miniMessage()

    override fun onNoPermission(context: CommandContext, permission: String) {
        val msg = messageProvider.noPermission.replace("{PERMISSION}", permission)
        context.sender.sendMessage(mm.deserialize(msg))
    }

    override fun onPlayerOnly(context: CommandContext) {
        context.sender.sendMessage(mm.deserialize(messageProvider.playerOnly))
    }

    override fun onConsoleOnly(context: CommandContext) {
        context.sender.sendMessage(mm.deserialize(messageProvider.consoleOnly))
    }

    override fun onInvalidArgument(context: CommandContext, argName: String, input: String, expected: String) {
        val msg = messageProvider.invalidArg
            .replace("{ARG}", argName)
            .replace("{INPUT}", input)
            .replace("{EXPECTED}", expected)
        context.sender.sendMessage(mm.deserialize(msg))
    }

    override fun onMissingArgument(context: CommandContext, argName: String, usage: String) {
        val msg = messageProvider.missingArg.replace("{ARG}", argName)
        context.sender.sendMessage(mm.deserialize(msg))
        context.sender.sendMessage(mm.deserialize(messageProvider.usage.replace("{USAGE}", usage)))
    }

    override fun onOutOfRange(context: CommandContext, argName: String, value: Number, range: ClosedFloatingPointRange<Double>) {
        val msg = messageProvider.outOfRange
            .replace("{ARG}", argName)
            .replace("{MIN}", range.start.toString())
            .replace("{MAX}", range.endInclusive.toString())
        context.sender.sendMessage(mm.deserialize(msg))
    }

    override fun onCooldown(context: CommandContext, remaining: Duration) {
        val timeStr = formatDuration(remaining)
        val msg = messageProvider.cooldown.replace("{TIME}", timeStr)
        context.sender.sendMessage(mm.deserialize(msg))
    }

    override fun onCommandException(context: CommandContext, exception: Exception) {
        context.sender.sendMessage(mm.deserialize(messageProvider.commandError))
        exception.printStackTrace()
    }

    override fun onShowHelp(context: CommandContext, node: CommandNode) {
        val usage = UsageFormatter.formatAll(node)
        context.sender.sendMessage(mm.deserialize(messageProvider.usage.replace("{USAGE}", usage)))
    }

    override fun onUnknownSubcommand(context: CommandContext, input: String, available: List<String>) {
        val msg = messageProvider.unknownSubcmd
            .replace("{INPUT}", input)
            .replace("{AVAILABLE}", available.joinToString(", "))
        context.sender.sendMessage(mm.deserialize(msg))
    }

    private fun formatDuration(duration: Duration): String {
        val seconds = duration.seconds
        return when {
            seconds >= 3600 -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
            seconds >= 60 -> "${seconds / 60}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }
}
