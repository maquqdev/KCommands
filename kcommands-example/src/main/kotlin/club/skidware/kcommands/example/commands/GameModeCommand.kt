package club.skidware.kcommands.example.commands

import club.skidware.kcommands.annotation.*
import org.bukkit.GameMode
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@Command("gm")
@Permission("example.gamemode")
@Description("Change game mode")
class GameModeCommand {
    @DefaultHandler
    @PlayerOnly
    fun changeMode(
        @Sender sender: Player,
        @Arg("mode") mode: GameMode
    ) {
        sender.gameMode = mode
        sender.sendMessage("Game mode set to ${mode.name.lowercase()}!")
    }

    @DefaultHandler
    fun changeModeOther(
        @Sender sender: CommandSender,
        @Arg("mode") mode: GameMode,
        @Arg("player") target: Player
    ) {
        target.gameMode = mode
        sender.sendMessage("Set ${target.name}'s game mode to ${mode.name.lowercase()}!")
    }
}
