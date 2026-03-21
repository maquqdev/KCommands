package club.skidware.kcommands.commands

import club.skidware.kcommands.annotation.*
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@Command("heal")
@Permission("test.heal")
@Description("Heal a player")
class HealCommand {
    var lastHealed: Player? = null

    @DefaultHandler
    fun heal(@Sender sender: Player) {
        sender.health = 20.0
        lastHealed = sender
    }
}

@Command("tp", "teleport")
@Description("Teleport command")
class TpCommand {
    var teleportedSelf = false
    var teleportedOther = false

    @DefaultHandler
    fun teleportSelf(
        @Sender sender: Player,
        @Arg("target") target: Player
    ) {
        teleportedSelf = true
    }

    @DefaultHandler
    fun teleportOther(
        @Sender sender: Player,
        @Arg("target") target: Player,
        @Arg("destination") destination: Player
    ) {
        teleportedOther = true
    }
}

@Command("greet")
class GreetCommand {
    var lastMessage: String? = null

    @DefaultHandler
    fun greet(
        @Sender sender: Player,
        @Arg("message") @Greedy message: String
    ) {
        lastMessage = message
    }
}

@Command("give")
@Description("Give items")
class GiveCommand {
    var lastAmount: Int? = null

    @DefaultHandler
    fun give(
        @Sender sender: Player,
        @Arg("amount") @Range(min = 1.0, max = 64.0) amount: Int
    ) {
        lastAmount = amount
    }
}

@Command("admin")
@Permission("admin.base")
class AdminCommands {
    var reloaded = false
    var lastDebugState: Boolean? = null

    @SubCommand("reload")
    @Permission("admin.reload")
    fun reload(@Sender sender: CommandSender) {
        reloaded = true
        sender.sendMessage("Reloaded!")
    }

    @SubCommand("debug")
    fun debug(
        @Sender sender: CommandSender,
        @Arg("enabled") enabled: Boolean
    ) {
        lastDebugState = enabled
        sender.sendMessage("Debug: $enabled")
    }
}

@Command("info")
class InfoCommand {
    var noArgsExecuted = false
    var lastNameArg: String? = null

    @DefaultHandler
    fun noArgs(@Sender sender: Player) {
        noArgsExecuted = true
        sender.sendMessage("Info command - no args")
    }

    @DefaultHandler
    fun withName(
        @Sender sender: Player,
        @Arg("name") name: String
    ) {
        lastNameArg = name
        sender.sendMessage("Info: $name")
    }
}

@Command("playeronly")
class PlayerOnlyCommand {
    var executed = false

    @DefaultHandler
    @PlayerOnly
    fun execute(@Sender sender: Player) {
        executed = true
        sender.sendMessage("Player only!")
    }
}

@Command("flagtest")
class FlagTestCommand {
    var silentFlag = false
    var countFlag = 0

    @DefaultHandler
    fun execute(
        @Sender sender: Player,
        @Arg("target") target: String,
        @Flag("silent", short = 's') silent: Boolean = false,
        @Flag("count", short = 'c') count: Int = 1
    ) {
        silentFlag = silent
        countFlag = count
    }
}
