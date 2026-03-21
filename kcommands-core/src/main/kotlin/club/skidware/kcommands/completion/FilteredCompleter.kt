package club.skidware.kcommands.completion

import club.skidware.kcommands.execution.CommandContext

/**
 * Tab completer wrapper that filters results by permission and/or a custom predicate.
 *
 * @param delegate the underlying completer to wrap
 * @param permission if set, returns empty results when the sender lacks this permission
 * @param filter optional predicate to further filter completion results
 */
class FilteredCompleter(
    private val delegate: TabCompleter,
    private val permission: String? = null,
    private val filter: ((String, CommandContext) -> Boolean)? = null
) : TabCompleter {

    override fun complete(input: String, context: CommandContext): List<String> {
        if (permission != null && !context.sender.hasPermission(permission)) {
            return emptyList()
        }

        val results = delegate.complete(input, context)

        return if (filter != null) {
            results.filter { filter.invoke(it, context) }
        } else {
            results
        }
    }
}
