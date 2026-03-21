package club.skidware.kcommands.message

/**
 * Data class holding all configurable message templates used by KCommands.
 *
 * All messages use MiniMessage format and support placeholders (e.g., `{PERMISSION}`, `{ARG}`).
 * Override individual messages via `copy()` or provide a complete instance to [KCommands.setup].
 */
data class CommandMessages(
    val noPermission: String = "<red>No permission! <dark_gray>({PERMISSION})",
    val playerOnly: String = "<red>This command is player-only!",
    val consoleOnly: String = "<red>This command is console-only!",
    val invalidArg: String = "<red>Invalid argument <gold>{ARG}</gold>: <white>{INPUT}<red>. {EXPECTED}",
    val missingArg: String = "<red>Missing required argument: <gold>{ARG}",
    val usage: String = "<gray>{USAGE}",
    val outOfRange: String = "<red>Value <gold>{ARG}</gold> must be between <white>{MIN}</white> and <white>{MAX}</white>!",
    val cooldown: String = "<red>Wait <gold>{TIME}</gold> before using this again!",
    val unknownSubcmd: String = "<red>Unknown subcommand: <gold>{INPUT}<red>. Available: <yellow>{AVAILABLE}",
    val confirm: String = "<yellow>Type <gold>/confirm</gold> within <white>{TIME}s</white> to confirm.",
    val confirmExpired: String = "<red>Confirmation expired.",
    val commandError: String = "<red>An error occurred while executing this command!",
    val helpHeader: String = "<gold><bold>★ {COMMAND}</bold></gold>",
    val helpEntry: String = "<gray>  {USAGE} <dark_gray>- {DESCRIPTION}",
    val helpFooter: String = "<gray>Page <white>{PAGE}<gray>/<white>{PAGES}"
)
