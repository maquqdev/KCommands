package club.skidware.kcommands.example.commands

import club.skidware.kcommands.annotation.*
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import java.time.Instant

@Command("elookup")
@Permission("example.lookup")
@Description("Look up player information (async)")
class LookupCommand {
    @DefaultHandler
    @Async
    fun lookup(
        @Sender sender: CommandSender,
        @Arg("player") target: OfflinePlayer
    ) {
        sender.sendMessage("Looking up ${target.name}...")
        Thread.sleep(100)
        sender.sendMessage("UUID: ${target.uniqueId}")
        sender.sendMessage("First played: ${Instant.ofEpochMilli(target.firstPlayed)}")
        @Suppress("DEPRECATION")
        sender.sendMessage("Last seen: ${Instant.ofEpochMilli(target.lastPlayed)}")
        sender.sendMessage("Banned: ${target.isBanned}")
    }
}
