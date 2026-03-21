package club.skidware.kcommands.example.commands

import club.skidware.kcommands.annotation.*
import org.bukkit.entity.Player

@Command("espawn")
@Permission("example.spawn")
@Description("Teleport to spawn")
class SpawnCommand {
    @DefaultHandler
    @PlayerOnly
    fun spawn(@Sender sender: Player) {
        sender.teleport(sender.world.spawnLocation)
        sender.sendMessage("Teleported to spawn!")
    }
}
