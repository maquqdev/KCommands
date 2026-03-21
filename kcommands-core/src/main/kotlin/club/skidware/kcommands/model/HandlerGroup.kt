package club.skidware.kcommands.model

/**
 * A collection of [ExecutorInfo] handlers registered at a single [CommandNode].
 *
 * Supports multiple handler overloads differentiated by their required argument count.
 */
class HandlerGroup {
    private val handlers = mutableListOf<ExecutorInfo>()

    /** Adds an executor handler to this group. */
    fun add(executor: ExecutorInfo) {
        handlers.add(executor)
    }

    /** Returns an immutable snapshot of all handlers in this group. */
    fun all(): List<ExecutorInfo> = handlers.toList()

    /**
     * Finds the best matching handler for the given [argCount].
     *
     * Prefers handlers with more arguments. Accounts for optional and greedy parameters.
     *
     * @param argCount the number of positional arguments provided
     * @return the best matching handler, or null if none matches
     */
    fun match(argCount: Int): ExecutorInfo? {
        return handlers
            .sortedByDescending { it.args.size }
            .firstOrNull { handler ->
                val required = handler.args.count { it.required }
                val total = handler.args.size
                val hasGreedy = handler.args.any { it.isGreedy }
                val upper = if (hasGreedy) Int.MAX_VALUE else total
                argCount in required..upper
            }
    }

    /** Returns the argument lists for all handler variants (used for usage display). */
    fun usageVariants(): List<List<ArgInfo>> = handlers.map { it.args }

    /** Returns true if no handlers are registered. */
    fun isEmpty(): Boolean = handlers.isEmpty()

    /** Returns true if at least one handler is registered. */
    fun isNotEmpty(): Boolean = handlers.isNotEmpty()
}
