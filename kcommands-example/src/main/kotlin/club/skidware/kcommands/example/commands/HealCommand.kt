package club.skidware.kcommands.example.commands

import club.skidware.kcommands.annotation.*
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player

@Command("heal")
@Permission("example.heal")
@Description("Heal yourself")
class HealCommand {
    @DefaultHandler
    @PlayerOnly
    fun heal(@Sender sender: Player) {
        sender.health = sender.getAttribute(Attribute.MAX_HEALTH)?.value ?: 20.0
        sender.sendMessage("You have been healed!")
    }
}
