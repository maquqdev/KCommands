package club.skidware.kcommands.example.commands

import club.skidware.kcommands.annotation.*
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@Command("feed", "satiate")
@Permission("example.feed")
@Description("Feed yourself or another player")
class FeedCommand {
    @DefaultHandler
    @PlayerOnly
    fun feedSelf(@Sender sender: Player) {
        sender.foodLevel = 20
        sender.saturation = 20f
        sender.sendMessage("You have been fed!")
    }

    @DefaultHandler
    fun feedOther(
        @Sender sender: CommandSender,
        @Arg("player") target: Player
    ) {
        target.foodLevel = 20
        target.saturation = 20f
        sender.sendMessage("Fed ${target.name}!")
        target.sendMessage("You have been fed by ${sender.name}!")
    }
}
