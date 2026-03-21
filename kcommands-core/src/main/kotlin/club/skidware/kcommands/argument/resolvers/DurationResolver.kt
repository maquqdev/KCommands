package club.skidware.kcommands.argument.resolvers

import club.skidware.kcommands.argument.ArgumentException
import club.skidware.kcommands.argument.ArgumentResolver
import club.skidware.kcommands.execution.CommandContext
import java.time.Duration

/** Resolver that parses duration strings (e.g., "30s", "5m", "2h30m", "1d") into [Duration]. */
object DurationResolver : ArgumentResolver<Duration> {
    private val PATTERN = Regex("(\\d+)([smhd])")

    override fun resolve(input: String, context: CommandContext): Duration {
        val matches = PATTERN.findAll(input.lowercase()).toList()
        if (matches.isEmpty()) {
            throw ArgumentException("'$input' is not a valid duration (e.g., 30s, 5m, 2h, 1d, 2h30m)")
        }

        var duration = Duration.ZERO
        for (match in matches) {
            val amount = match.groupValues[1].toLong()
            val unit = match.groupValues[2]
            duration = duration.plus(
                when (unit) {
                    "s" -> Duration.ofSeconds(amount)
                    "m" -> Duration.ofMinutes(amount)
                    "h" -> Duration.ofHours(amount)
                    "d" -> Duration.ofDays(amount)
                    else -> throw ArgumentException("Unknown time unit '$unit'")
                }
            )
        }

        return duration
    }

    override fun complete(input: String, context: CommandContext): List<String> =
        listOf("30s", "1m", "5m", "10m", "30m", "1h", "12h", "1d", "7d", "30d")
            .filter { it.startsWith(input.lowercase()) }
}
