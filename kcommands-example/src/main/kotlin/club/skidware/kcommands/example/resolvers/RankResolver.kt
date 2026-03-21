package club.skidware.kcommands.example.resolvers

import club.skidware.kcommands.argument.ArgumentException
import club.skidware.kcommands.argument.ArgumentResolver
import club.skidware.kcommands.execution.CommandContext

class RankResolver : ArgumentResolver<Rank> {
    private val ranks = mapOf(
        "member" to Rank("Member", 0, "&7"),
        "vip" to Rank("VIP", 10, "&a"),
        "moderator" to Rank("Moderator", 50, "&9"),
        "admin" to Rank("Admin", 100, "&c"),
        "owner" to Rank("Owner", 999, "&4")
    )

    override fun resolve(input: String, context: CommandContext): Rank {
        return ranks[input.lowercase()]
            ?: throw ArgumentException("Rank '$input' not found! Available: ${ranks.keys.joinToString()}")
    }

    override fun complete(input: String, context: CommandContext): List<String> {
        return ranks.keys.filter { it.startsWith(input.lowercase()) }
    }
}
