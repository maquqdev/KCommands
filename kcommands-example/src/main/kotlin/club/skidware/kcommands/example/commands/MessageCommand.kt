package club.skidware.kcommands.example.commands

import club.skidware.kcommands.annotation.*
import org.bukkit.entity.Player

@Command("emsg", "ewhisper")
@Permission("example.message")
@Description("Send a private message")
class MessageCommand {
    @DefaultHandler
    @PlayerOnly
    fun message(
        @Sender sender: Player,
        @Arg("player") target: Player,
        @Arg("message") @Greedy message: String
    ) {
        target.sendMessage("[${sender.name} -> you] $message")
        sender.sendMessage("[you -> ${target.name}] $message")
    }
}
