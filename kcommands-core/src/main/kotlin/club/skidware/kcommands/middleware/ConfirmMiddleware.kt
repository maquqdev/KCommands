package club.skidware.kcommands.middleware

import club.skidware.kcommands.execution.CommandContext
import club.skidware.kcommands.message.DefaultMessageProvider
import club.skidware.kcommands.message.IMessageProvider
import club.skidware.kcommands.model.ExecutorInfo
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.entity.Player
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Middleware that implements a two-step confirmation flow for handlers annotated with `@Confirm`.
 *
 * On first execution, the action is stored as pending and the player is prompted to confirm.
 * The pending action expires after the configured timeout.
 */
class ConfirmMiddleware(
    private val messageProvider: IMessageProvider = DefaultMessageProvider()
) : CommandMiddleware {

    data class PendingAction(
        val action: () -> Unit,
        val expiresAt: Instant,
        val message: String
    )

    private val pending = ConcurrentHashMap<UUID, PendingAction>()
    private val mm = MiniMessage.miniMessage()

    override fun before(context: CommandContext, handler: ExecutorInfo): Boolean {
        val confirmInfo = handler.confirm ?: return true
        val player = context.sender as? Player ?: return true

        val existing = pending[player.uniqueId]
        if (existing != null) {
            // Already has a pending confirmation-this means the execute phase
            // should proceed because confirm() was called. We just remove it.
            return true
        }

        // Store the pending action-the dispatcher will call us back for the actual execution
        // Return false to prevent immediate execution
        return false
    }

    /** Stores a pending action and sends the confirmation prompt to the [player]. */
    fun requestConfirmation(
        player: Player,
        handler: ExecutorInfo,
        context: CommandContext,
        action: () -> Unit
    ) {
        val confirmInfo = handler.confirm ?: return

        pending[player.uniqueId] = PendingAction(
            action = action,
            expiresAt = Instant.now().plusSeconds(confirmInfo.timeoutSeconds.toLong()),
            message = confirmInfo.message
        )

        val msg = messageProvider.confirm
            .replace("{TIME}", confirmInfo.timeoutSeconds.toString())
        player.sendMessage(mm.deserialize(msg))

        if (confirmInfo.message.isNotEmpty() && confirmInfo.message != "Are you sure?") {
            player.sendMessage(mm.deserialize("<yellow>${confirmInfo.message}"))
        }
    }

    /**
     * Executes the pending confirmation action for the given [playerUuid].
     *
     * @return true if a pending action was found and executed, false if expired or not found
     */
    fun confirm(playerUuid: UUID): Boolean {
        val action = pending.remove(playerUuid) ?: return false

        if (Instant.now().isAfter(action.expiresAt)) {
            return false
        }

        action.action()
        return true
    }

    /** Returns true if the player has a non-expired pending confirmation action. */
    fun hasPending(playerUuid: UUID): Boolean {
        val action = pending[playerUuid] ?: return false
        if (Instant.now().isAfter(action.expiresAt)) {
            pending.remove(playerUuid)
            return false
        }
        return true
    }

    /** Cancels the pending confirmation action for the given player. */
    fun cancelPending(playerUuid: UUID) {
        pending.remove(playerUuid)
    }

    /** Removes all expired pending actions from the map. */
    fun cleanExpired() {
        val now = Instant.now()
        pending.entries.removeIf { now.isAfter(it.value.expiresAt) }
    }

    /** Clears all pending confirmation actions. */
    fun clearAll() {
        pending.clear()
    }
}
