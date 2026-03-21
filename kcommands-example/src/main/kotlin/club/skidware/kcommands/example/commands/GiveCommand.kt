package club.skidware.kcommands.example.commands

import club.skidware.kcommands.annotation.*
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

@Command("egive")
@Permission("example.give")
@Description("Give items to a player")
class GiveCommand {
    @DefaultHandler
    fun give(
        @Sender sender: CommandSender,
        @Arg("player") target: Player,
        @Arg("item") material: Material,
        @Arg("amount") @Range(min = 1.0, max = 64.0) amount: Int = 1
    ) {
        val stack = ItemStack(material, amount)
        target.inventory.addItem(stack)
        sender.sendMessage("Gave ${amount}x ${material.name.lowercase()} to ${target.name}!")
    }
}
