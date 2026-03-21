package club.skidware.kcommands.example.commands

import club.skidware.kcommands.annotation.*
import org.bukkit.entity.Player

@Command("espeed")
@Permission("example.speed")
@Description("Set walk or fly speed")
class SetSpeedCommand {
    @SubCommand("walk")
    @PlayerOnly
    @Description("Set walking speed")
    fun walkSpeed(
        @Sender sender: Player,
        @Arg("speed") @Range(min = 0.0, max = 1.0) speed: Float
    ) {
        sender.walkSpeed = speed
        sender.sendMessage("Walk speed set to $speed!")
    }

    @SubCommand("fly")
    @PlayerOnly
    @Description("Set flying speed")
    fun flySpeed(
        @Sender sender: Player,
        @Arg("speed") @Range(min = 0.0, max = 1.0) speed: Float
    ) {
        sender.flySpeed = speed
        sender.sendMessage("Fly speed set to $speed!")
    }
}
