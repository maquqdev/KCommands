package club.skidware.kcommands.message

/**
 * Interface for providing command messages.
 *
 * Implement this interface to customize all messages KCommands sends to players.
 * This enables full i18n support, config-based messages, database-backed messages, etc.
 *
 * Example:
 * ```kotlin
 * class MyMessageProvider(private val config: FileConfiguration) : IMessageProvider {
 *     override val noPermission get() = config.getString("messages.no-permission")!!
 *     override val playerOnly get() = config.getString("messages.player-only")!!
 *     // ... etc
 * }
 *
 * KCommands.setup(plugin) {
 *     messageProvider(MyMessageProvider(config))
 * }
 * ```
 */
interface IMessageProvider {
    val noPermission: String
    val playerOnly: String
    val consoleOnly: String
    val invalidArg: String
    val missingArg: String
    val usage: String
    val outOfRange: String
    val cooldown: String
    val unknownSubcmd: String
    val confirm: String
    val confirmExpired: String
    val commandError: String
    val helpHeader: String
    val helpEntry: String
    val helpFooter: String
}

/**
 * Default implementation backed by [CommandMessages] data class.
 * All messages use MiniMessage format with placeholders.
 */
class DefaultMessageProvider(
    private val messages: CommandMessages = CommandMessages()
) : IMessageProvider {
    override val noPermission get() = messages.noPermission
    override val playerOnly get() = messages.playerOnly
    override val consoleOnly get() = messages.consoleOnly
    override val invalidArg get() = messages.invalidArg
    override val missingArg get() = messages.missingArg
    override val usage get() = messages.usage
    override val outOfRange get() = messages.outOfRange
    override val cooldown get() = messages.cooldown
    override val unknownSubcmd get() = messages.unknownSubcmd
    override val confirm get() = messages.confirm
    override val confirmExpired get() = messages.confirmExpired
    override val commandError get() = messages.commandError
    override val helpHeader get() = messages.helpHeader
    override val helpEntry get() = messages.helpEntry
    override val helpFooter get() = messages.helpFooter
}
