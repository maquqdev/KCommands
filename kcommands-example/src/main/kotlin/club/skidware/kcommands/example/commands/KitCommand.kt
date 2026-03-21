package club.skidware.kcommands.example.commands

import club.skidware.kcommands.annotation.*
import club.skidware.kcommands.example.resolvers.Kit
import org.bukkit.entity.Player

@Command("ekit")
@Permission("example.kit")
@Description("Kit management")
class KitCommand {
    @DefaultHandler
    @PlayerOnly
    fun list(@Sender sender: Player) {
        sender.sendMessage("Available kits: starter, pvp, builder, admin")
    }

    @DefaultHandler
    fun giveKit(
        @Sender sender: Player,
        @Arg("kit") kit: Kit
    ) {
        sender.sendMessage("Received kit: ${kit.name} (${kit.items.size} items)")
    }

    @SubCommand("create")
    @Permission("example.kit.admin")
    @Description("Create a new kit")
    fun create(
        @Sender sender: Player,
        @Arg("name") name: String
    ) {
        sender.sendMessage("Kit '$name' created from your inventory!")
    }

    @SubCommand("delete")
    @Permission("example.kit.admin")
    @Description("Delete a kit")
    @Confirm(message = "Are you sure you want to delete this kit?", timeoutSeconds = 10)
    fun delete(
        @Sender sender: Player,
        @Arg("name") name: String
    ) {
        sender.sendMessage("Kit '$name' deleted!")
    }
}
