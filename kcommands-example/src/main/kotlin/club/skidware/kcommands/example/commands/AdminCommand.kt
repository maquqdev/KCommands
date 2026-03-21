package club.skidware.kcommands.example.commands

import club.skidware.kcommands.annotation.*
import org.bukkit.command.CommandSender

@Command("eadmin")
@Permission("example.admin")
@Description("Admin commands")
class AdminCommand {
    @SubCommand("reload")
    @Permission("example.admin.reload")
    @Description("Reload plugin configuration")
    fun reload(@Sender sender: CommandSender) {
        sender.sendMessage("Configuration reloaded!")
    }

    @SubCommand("debug")
    @Permission("example.admin.debug")
    @Description("Toggle debug mode")
    fun debug(
        @Sender sender: CommandSender,
        @Arg("enabled") enabled: Boolean
    ) {
        sender.sendMessage("Debug mode: $enabled")
    }

    @SubCommand("status")
    @Description("Show server status")
    fun status(@Sender sender: CommandSender) {
        val runtime = Runtime.getRuntime()
        val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val maxMem = runtime.maxMemory() / 1024 / 1024
        sender.sendMessage("Memory: ${usedMem}MB / ${maxMem}MB")
        sender.sendMessage("Threads: ${Thread.activeCount()}")
    }
}
