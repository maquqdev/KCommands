package club.skidware.kcommands.example.commands

import club.skidware.kcommands.annotation.*
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import java.time.Duration

@Command("eban")
@Permission("example.ban")
@Description("Ban a player")
class BanCommand {
    @DefaultHandler
    fun ban(
        @Sender sender: CommandSender,
        @Arg("player") target: OfflinePlayer,
        @Arg("reason") @Greedy reason: String = "No reason provided",
        @Flag("silent", short = 's') silent: Boolean = false,
        @Flag("duration", short = 'd') duration: Duration = Duration.ofDays(30)
    ) {
        val durationStr = formatDuration(duration)
        if (!silent) {
            @Suppress("DEPRECATION")
            Bukkit.broadcastMessage("${target.name} was banned for $durationStr: $reason")
        }
        sender.sendMessage("Banned ${target.name} for $durationStr (silent: $silent)")
    }

    private fun formatDuration(d: Duration): String {
        val days = d.toDays()
        return when {
            days > 0 -> "${days}d"
            d.toHours() > 0 -> "${d.toHours()}h"
            else -> "${d.toMinutes()}m"
        }
    }
}
