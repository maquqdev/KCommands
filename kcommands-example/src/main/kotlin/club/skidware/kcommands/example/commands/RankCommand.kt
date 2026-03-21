package club.skidware.kcommands.example.commands

import club.skidware.kcommands.annotation.*
import club.skidware.kcommands.example.resolvers.Rank
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@Command("erank")
@Permission("example.rank")
@Description("Rank management")
class RankCommand {
    @SubCommand("set")
    @Permission("example.rank.set")
    @Description("Set a player's rank")
    fun setRank(
        @Sender sender: CommandSender,
        @Arg("player") target: Player,
        @Arg("rank") rank: Rank
    ) {
        sender.sendMessage("Set ${target.name}'s rank to ${rank.name} (weight: ${rank.weight})")
    }

    @SubCommand("info")
    @Description("View a player's rank")
    fun info(
        @Sender sender: CommandSender,
        @Arg("player") target: Player
    ) {
        sender.sendMessage("${target.name}'s current rank: Member")
    }
}
