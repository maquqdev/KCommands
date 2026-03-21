package club.skidware.kcommands.argument.resolvers

import club.skidware.kcommands.argument.ArgumentException
import club.skidware.kcommands.argument.ArgumentResolver
import club.skidware.kcommands.execution.CommandContext
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player

/** Resolver that parses "x y z [world]" format into a Bukkit [Location]. Supports relative coordinates with `~`. */
object LocationResolver : ArgumentResolver<Location> {
    override fun resolve(input: String, context: CommandContext): Location {
        val parts = input.split(" ")
        if (parts.size < 3) {
            throw ArgumentException("Location must be in format 'x y z' or 'x y z world'")
        }

        val senderPlayer = context.sender as? Player

        val x = parseCoordinate(parts[0], senderPlayer?.location?.x ?: 0.0, "x")
        val y = parseCoordinate(parts[1], senderPlayer?.location?.y ?: 0.0, "y")
        val z = parseCoordinate(parts[2], senderPlayer?.location?.z ?: 0.0, "z")

        val world = if (parts.size >= 4) {
            Bukkit.getWorld(parts[3])
                ?: throw ArgumentException("World '${parts[3]}' does not exist")
        } else {
            senderPlayer?.world
                ?: Bukkit.getWorlds().firstOrNull()
                ?: throw ArgumentException("No world available and sender is not a player")
        }

        return Location(world, x, y, z)
    }

    private fun parseCoordinate(value: String, senderCoord: Double, axis: String): Double {
        if (value.startsWith("~")) {
            val offset = value.substring(1)
            if (offset.isEmpty()) return senderCoord
            return senderCoord + (offset.toDoubleOrNull()
                ?: throw ArgumentException("'$offset' is not a valid offset for $axis coordinate"))
        }
        return value.toDoubleOrNull()
            ?: throw ArgumentException("'$value' is not a valid $axis coordinate")
    }

    override fun complete(input: String, context: CommandContext): List<String> =
        listOf("~ ~ ~")
}
