package club.skidware.kcommands.execution

import club.skidware.kcommands.model.CommandNode
import club.skidware.kcommands.model.ExecutorInfo

/**
 * Navigates the [CommandNode] tree to find the best matching handler for a given set of arguments.
 *
 * Prioritizes subcommand matches before falling back to handlers on the current node.
 */
object HandlerMatcher {

    /** The result of matching arguments against the command tree. */
    data class MatchResult(
        val node: CommandNode,
        val handler: ExecutorInfo,
        val remainingArgs: List<String>,
        val parentPath: String
    )

    /**
     * Recursively matches [args] against the command tree starting at [node].
     *
     * @param node the current command node to match against
     * @param args the remaining arguments to match
     * @param parentPath the accumulated parent command path
     * @return the match result, or null if no handler matches
     */
    fun match(
        node: CommandNode,
        args: List<String>,
        parentPath: String = ""
    ): MatchResult? {
        // Priority 1: Try matching a subcommand first
        if (args.isNotEmpty()) {
            val subName = args[0].lowercase()
            val childNode = findChild(node, subName)
            if (childNode != null) {
                val childPath = if (parentPath.isEmpty()) node.name else "$parentPath ${node.name}"
                val subResult = match(childNode, args.drop(1), childPath)
                if (subResult != null) return subResult
            }
        }

        // Priority 2: Match a handler on the current node
        val filteredArgs = args.filter { !it.startsWith("-") || it.toDoubleOrNull() != null }
        val handler = node.handlers.match(filteredArgs.size)
        if (handler != null) {
            return MatchResult(
                node = node,
                handler = handler,
                remainingArgs = args,
                parentPath = parentPath
            )
        }

        return null
    }

    /** Finds a child node by name or alias (case-insensitive). */
    fun findChild(node: CommandNode, name: String): CommandNode? {
        val lowered = name.lowercase()
        node.children[lowered]?.let { return it }
        return node.children.values.firstOrNull { child ->
            child.aliases.any { it.lowercase() == lowered }
        }
    }

    /** Collects all subcommand names and aliases for the given [node]. */
    fun collectAvailableSubcommands(node: CommandNode): List<String> {
        val names = mutableListOf<String>()
        for ((name, child) in node.children) {
            names.add(name)
            names.addAll(child.aliases)
        }
        return names.distinct()
    }
}
