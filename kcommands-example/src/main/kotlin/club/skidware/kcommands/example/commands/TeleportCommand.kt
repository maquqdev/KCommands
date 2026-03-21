package club.skidware.kcommands.example.commands

import club.skidware.kcommands.annotation.*
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@Command("etp")
@Permission("example.teleport")
@Description("Teleport to player or coordinates")
class TeleportCommand {
    @DefaultHandler
    @PlayerOnly
    fun toPlayer(
        @Sender sender: Player,
        @Arg("target") target: Player
    ) {
        sender.teleport(target)
        sender.sendMessage("Teleported to ${target.name}!")
    }

    @DefaultHandler
    fun toPlayerToPlayer(
        @Sender sender: CommandSender,
        @Arg("player") player: Player,
        @Arg("target") target: Player
    ) {
        player.teleport(target)
        sender.sendMessage("Teleported ${player.name} to ${target.name}!")
    }
}
