package club.skidware.kcommands.example.commands

import club.skidware.kcommands.annotation.*
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@Command("einfo")
@Description("Information command with multiple overloads")
class InfoCommand {
    @DefaultHandler
    @PlayerOnly
    fun noArgs(@Sender sender: Player) {
        sender.sendMessage("Usage: /einfo [player] [detail]")
        sender.sendMessage("Shows information about a player.")
    }

    @DefaultHandler
    fun withPlayer(
        @Sender sender: CommandSender,
        @Arg("player") target: Player
    ) {
        sender.sendMessage("${target.name}: HP ${target.health}, Food ${target.foodLevel}")
    }

    @DefaultHandler
    fun withPlayerAndDetail(
        @Sender sender: CommandSender,
        @Arg("player") target: Player,
        @Arg("detail") detail: String
    ) {
        when (detail.lowercase()) {
            "health" -> sender.sendMessage("${target.name} health: ${target.health}")
            "food" -> sender.sendMessage("${target.name} food: ${target.foodLevel}")
            "location" -> sender.sendMessage("${target.name} at: ${target.location.blockX}, ${target.location.blockY}, ${target.location.blockZ}")
            "gamemode" -> sender.sendMessage("${target.name} mode: ${target.gameMode}")
            else -> sender.sendMessage("Unknown detail: $detail. Try: health, food, location, gamemode")
        }
    }
}
