package club.skidware.kcommands.event

import club.skidware.kcommands.model.CommandNode
import org.bukkit.command.CommandSender
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/** Bukkit event fired when an unhandled exception occurs during command execution. Cancellable. */
class KCommandErrorEvent(
    val sender: CommandSender,
    val command: String,
    val exception: Exception,
    val node: CommandNode
) : Event(), Cancellable {

    private var cancelled = false

    override fun isCancelled(): Boolean = cancelled

    override fun setCancelled(cancel: Boolean) {
        cancelled = cancel
    }

    override fun getHandlers(): HandlerList = handlerList

    companion object {
        @JvmStatic
        val handlerList = HandlerList()
    }
}
