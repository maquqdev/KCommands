package club.skidware.kcommands.argument

import club.skidware.kcommands.argument.resolvers.*
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.World
import org.bukkit.entity.Player
import java.time.Duration
import java.util.UUID

/**
 * Registers all built-in argument resolvers (String, Int, Player, Material, etc.)
 * into the given [ArgumentResolverRegistry]. Called automatically during [KCommands.setup].
 */
object BuiltinResolvers {
    /** Registers all built-in resolvers for primitive types, Bukkit types, and common types. */
    fun registerAll(registry: ArgumentResolverRegistry) {
        registry.register(String::class, StringResolver)
        registry.register(Int::class, IntResolver)
        registry.register(Long::class, LongResolver)
        registry.register(Double::class, DoubleResolver)
        registry.register(Float::class, FloatResolver)
        registry.register(Boolean::class, BooleanResolver)
        registry.register(Player::class, PlayerResolver)
        registry.register(OfflinePlayer::class, OfflinePlayerResolver)
        registry.register(Material::class, MaterialResolver)
        registry.register(World::class, WorldResolver)
        registry.register(GameMode::class, GameModeResolver)
        registry.register(Duration::class, DurationResolver)
        registry.register(UUID::class, UUIDResolver)
        registry.register(Location::class, LocationResolver)
    }
}
