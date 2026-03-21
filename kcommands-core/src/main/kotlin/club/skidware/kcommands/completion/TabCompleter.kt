package club.skidware.kcommands.completion

import club.skidware.kcommands.execution.CommandContext

/**
 * Provides tab-completion suggestions for a command argument.
 *
 * Implement this interface to supply custom completion logic, then reference it
 * via `@Complete(MyCompleter::class)` on an `@Arg` parameter.
 */
interface TabCompleter {
    /**
     * Returns completion suggestions for the given partial [input].
     *
     * @param input the current partial input being typed
     * @param context the current command execution context
     * @return list of suggested completions
     */
    fun complete(input: String, context: CommandContext): List<String>
}
