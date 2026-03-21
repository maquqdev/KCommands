package club.skidware.kcommands.middleware

import club.skidware.kcommands.execution.CommandContext
import club.skidware.kcommands.execution.ErrorHandler
import club.skidware.kcommands.model.ExecutorInfo
import org.bukkit.entity.Player
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Middleware that enforces per-player cooldowns on command handlers annotated with `@Cooldown`.
 *
 * Tracks cooldown expiry times in a thread-safe map keyed by player UUID, command, and method.
 */
class CooldownMiddleware(
    private val errorHandler: ErrorHandler
) : CommandMiddleware {

    private val cooldowns = ConcurrentHashMap<String, Instant>()

    override fun before(context: CommandContext, handler: ExecutorInfo): Boolean {
        val cooldownInfo = handler.cooldown ?: return true
        val player = context.sender as? Player ?: return true

        // Check bypass permission
        if (cooldownInfo.bypassPermission.isNotEmpty() && player.hasPermission(cooldownInfo.bypassPermission)) {
            return true
        }

        val key = buildKey(player.uniqueId.toString(), context.label, handler.function.name)
        val expiry = cooldowns[key]

        if (expiry != null && Instant.now().isBefore(expiry)) {
            val remaining = Duration.between(Instant.now(), expiry)
            errorHandler.onCooldown(context, remaining)
            return false
        }

        // Set cooldown
        cooldowns[key] = Instant.now().plusSeconds(cooldownInfo.seconds.toLong())
        return true
    }

    /** Clears a specific cooldown for the given player, command, and method. */
    fun clearCooldown(uuid: String, commandName: String, methodName: String) {
        cooldowns.remove(buildKey(uuid, commandName, methodName))
    }

    /** Clears all cooldowns for all players. */
    fun clearAllCooldowns() {
        cooldowns.clear()
    }

    /** Clears all cooldowns for a specific player by UUID. */
    fun clearPlayerCooldowns(uuid: String) {
        cooldowns.keys.removeIf { it.startsWith("$uuid:") }
    }

    private fun buildKey(uuid: String, commandName: String, methodName: String): String {
        return "$uuid:$commandName:$methodName"
    }
}
