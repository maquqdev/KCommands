package club.skidware.kcommands.event

import org.bukkit.command.CommandSender
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/** Bukkit event fired after a command execution completes, with success/failure status. */
class KCommandCompleteEvent(
    val sender: CommandSender,
    val command: String,
    val success: Boolean,
    val exception: Exception? = null
) : Event() {

    override fun getHandlers(): HandlerList = handlerList

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
