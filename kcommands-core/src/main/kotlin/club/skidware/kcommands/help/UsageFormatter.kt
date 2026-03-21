package club.skidware.kcommands.help

import club.skidware.kcommands.model.ArgInfo
import club.skidware.kcommands.model.CommandNode
import club.skidware.kcommands.model.ExecutorInfo

/**
 * Generates usage strings for commands, using `<required>` and `[optional]` notation.
 */
object UsageFormatter {

    /**
     * Formats a single usage line for the given [executor] within [node].
     *
     * @param node the command node
     * @param executor the specific handler variant
     * @param parentPath the parent command path prefix (e.g., "parent sub")
     * @return formatted usage string like `/parent sub command <arg1> [arg2]`
     */
    fun format(node: CommandNode, executor: ExecutorInfo, parentPath: String = ""): String {
        val path = buildPath(node, parentPath)
        val argPart = executor.args.joinToString(" ") { formatArg(it) }
        return if (argPart.isBlank()) "/$path" else "/$path $argPart"
    }

    /**
     * Formats all usage lines for a command node and its children, recursively.
     *
     * @param node the root command node
     * @param parentPath the parent command path prefix
     * @return newline-separated usage strings for all handlers and subcommands
     */
    fun formatAll(node: CommandNode, parentPath: String = ""): String {
        val lines = mutableListOf<String>()
        val path = buildPath(node, parentPath)

        for (executor in node.handlers.all()) {
            lines.add(format(node, executor, parentPath))
        }

        for ((_, child) in node.children) {
            lines.add(formatAll(child, path))
        }

        return lines.joinToString("\n")
    }

    /** Alias for [format] -- formats a single usage line for a handler. */
    fun formatUsageLine(node: CommandNode, executor: ExecutorInfo, parentPath: String = ""): String {
        return format(node, executor, parentPath)
    }

    /**
     * Formats a brief, single-line usage summary for a command node.
     *
     * Shows subcommand options as `<sub1|sub2>` or handler variants inline.
     *
     * @param node the command node
     * @param parentPath the parent command path prefix
     * @return a compact usage string
     */
    fun formatBrief(node: CommandNode, parentPath: String = ""): String {
        val path = buildPath(node, parentPath)
        val handlers = node.handlers.all()

        return when {
            handlers.isEmpty() && node.children.isNotEmpty() -> {
                val subNames = node.children.keys.sorted().joinToString("|")
                "/$path <$subNames>"
            }
            handlers.size == 1 -> format(node, handlers[0], parentPath)
            else -> {
                val variants = handlers.joinToString(" | ") { exec ->
                    exec.args.joinToString(" ") { formatArg(it) }
                }
                "/$path $variants"
            }
        }
    }

    private fun formatArg(arg: ArgInfo): String {
        val name = if (arg.isGreedy) "${arg.name}..." else arg.name
        return if (arg.required) "<$name>" else "[$name]"
    }

    private fun buildPath(node: CommandNode, parentPath: String): String {
        return if (parentPath.isBlank()) node.name else "$parentPath ${node.name}"
    }
}
