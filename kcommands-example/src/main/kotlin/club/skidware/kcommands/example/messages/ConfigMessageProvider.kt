package club.skidware.kcommands.example.messages

import club.skidware.kcommands.message.IMessageProvider
import org.bukkit.configuration.file.FileConfiguration

class ConfigMessageProvider(private val config: FileConfiguration) : IMessageProvider {
    override val noPermission get() = msg("no-permission", "<red>You don't have permission! <dark_gray>({PERMISSION})")
    override val playerOnly get() = msg("player-only", "<red>This command can only be used by players!")
    override val consoleOnly get() = msg("console-only", "<red>This command can only be used from console!")
    override val invalidArg get() = msg("invalid-arg", "<red>Invalid <gold>{ARG}</gold>: <white>{INPUT}<red> - {EXPECTED}")
    override val missingArg get() = msg("missing-arg", "<red>Missing argument: <gold>{ARG}")
    override val usage get() = msg("usage", "<gray>{USAGE}")
    override val outOfRange get() = msg("out-of-range", "<red>{ARG} must be between <white>{MIN}</white> and <white>{MAX}</white>!")
    override val cooldown get() = msg("cooldown", "<red>Please wait <gold>{TIME}</gold> before using this again!")
    override val unknownSubcmd get() = msg("unknown-subcommand", "<red>Unknown subcommand: <gold>{INPUT}<red>. Available: <yellow>{AVAILABLE}")
    override val confirm get() = msg("confirm", "<yellow>Type <gold>/confirm</gold> within <white>{TIME}s</white> to confirm.")
    override val confirmExpired get() = msg("confirm-expired", "<red>Confirmation expired.")
    override val commandError get() = msg("command-error", "<red>An error occurred! Please contact an administrator.")
    override val helpHeader get() = msg("help-header", "<gold><bold>{COMMAND} Help</bold></gold>")
    override val helpEntry get() = msg("help-entry", "<gray>  {USAGE} <dark_gray>- {DESCRIPTION}")
    override val helpFooter get() = msg("help-footer", "<gray>Page <white>{PAGE}<gray>/<white>{PAGES}")

    private fun msg(key: String, default: String): String {
        return config.getString("messages.$key", default) ?: default
    }
}
