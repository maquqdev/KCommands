package club.skidware.kcommands.example.commands

import club.skidware.kcommands.annotation.*
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender

@Command("serverinfo")
@Permission("example.serverinfo")
@Description("Show server information (console only)")
class ConsoleInfoCommand {
    @DefaultHandler
    @ConsoleOnly
    fun info(@Sender sender: CommandSender) {
        sender.sendMessage("Server: ${Bukkit.getName()} ${Bukkit.getVersion()}")
        sender.sendMessage("Players: ${Bukkit.getOnlinePlayers().size}/${Bukkit.getMaxPlayers()}")
        sender.sendMessage("Worlds: ${Bukkit.getWorlds().joinToString { it.name }}")
    }
}
