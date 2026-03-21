package club.skidware.kcommands.example.commands

import club.skidware.kcommands.annotation.*
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.attribute.Attribute
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@Command("eserver")
@Permission("example.server")
@Description("Server management")
class ServerCommand {
    @DefaultHandler
    fun help(@Sender sender: CommandSender) {
        sender.sendMessage("Usage: /eserver <world|player> ...")
    }

    @Command("world")
    @Description("World management")
    class WorldCommands {
        @SubCommand("list")
        @Description("List all worlds")
        fun list(@Sender sender: CommandSender) {
            Bukkit.getWorlds().forEach {
                sender.sendMessage("- ${it.name} (${it.environment.name.lowercase()})")
            }
        }

        @SubCommand("info")
        @Description("World information")
        fun info(
            @Sender sender: CommandSender,
            @Arg("world") world: World
        ) {
            sender.sendMessage("World: ${world.name}")
            sender.sendMessage("Environment: ${world.environment}")
            sender.sendMessage("Players: ${world.players.size}")
            sender.sendMessage("Seed: ${world.seed}")
        }
    }

    @Command("player")
    @Description("Player management")
    class PlayerCommands {
        @SubCommand("list")
        @Description("List online players")
        fun list(@Sender sender: CommandSender) {
            val players = Bukkit.getOnlinePlayers()
            sender.sendMessage("Online (${players.size}): ${players.joinToString { it.name }}")
        }

        @SubCommand("info")
        @Description("Player information")
        fun info(
            @Sender sender: CommandSender,
            @Arg("player") target: Player
        ) {
            sender.sendMessage("Name: ${target.name}")
            sender.sendMessage("Health: ${target.health}/${target.getAttribute(Attribute.MAX_HEALTH)?.value}")
            sender.sendMessage("Food: ${target.foodLevel}")
            sender.sendMessage("Location: ${target.location.blockX}, ${target.location.blockY}, ${target.location.blockZ}")
            sender.sendMessage("Game Mode: ${target.gameMode}")
        }
    }
}
