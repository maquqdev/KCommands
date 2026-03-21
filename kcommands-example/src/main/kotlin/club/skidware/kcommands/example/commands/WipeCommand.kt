package club.skidware.kcommands.example.commands

import club.skidware.kcommands.annotation.*
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@Command("ewipe")
@Permission("example.wipe")
@Description("Dangerous server operations")
class WipeCommand {
    @SubCommand("inventory")
    @Cooldown(seconds = 30, message = "You must wait before wiping again!", bypassPermission = "example.wipe.bypass")
    @Description("Clear a player's inventory")
    fun wipeInventory(
        @Sender sender: CommandSender,
        @Arg("player") target: Player
    ) {
        target.inventory.clear()
        sender.sendMessage("Cleared ${target.name}'s inventory!")
        target.sendMessage("Your inventory was cleared by ${sender.name}!")
    }

    @SubCommand("all")
    @Confirm(message = "This will clear ALL player inventories! Type /confirm to proceed.", timeoutSeconds = 15)
    @Description("Clear all player inventories")
    @PlayerOnly
    fun wipeAll(@Sender sender: Player) {
        Bukkit.getOnlinePlayers().forEach { it.inventory.clear() }
        sender.sendMessage("Cleared all player inventories!")
    }
}
