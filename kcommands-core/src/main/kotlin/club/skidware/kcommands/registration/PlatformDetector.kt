package club.skidware.kcommands.registration

/**
 * Detects the server platform capabilities at runtime (Paper, Brigadier support, version).
 */
object PlatformDetector {

    /** Whether the Paper Brigadier command API is available on this server. */
    val hasBrigadier: Boolean by lazy {
        try {
            Class.forName("io.papermc.paper.command.brigadier.Commands")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    /** Whether the server is running Paper (or a Paper fork). */
    val isPaper: Boolean by lazy {
        try {
            Class.forName("io.papermc.paper.event.player.AsyncChatEvent")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    /** The server version string, or "unknown" if it cannot be determined. */
    val serverVersion: String by lazy {
        try {
            org.bukkit.Bukkit.getServer().version
        } catch (_: Exception) {
            "unknown"
        }
    }
}
