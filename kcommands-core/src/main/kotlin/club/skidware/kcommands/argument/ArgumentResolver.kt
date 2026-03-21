package club.skidware.kcommands.argument

import club.skidware.kcommands.execution.CommandContext

/**
 * Converts raw string input into a typed value of [T].
 *
 * Implement this interface to support custom argument types in command parameters.
 *
 * @param T the target type this resolver produces
 */
interface ArgumentResolver<T> {
    /**
     * Resolves the given [input] string into a value of type [T].
     *
     * @param input the raw string argument from the command
     * @param context the current command execution context
     * @return the resolved value
     * @throws club.skidware.kcommands.argument.ArgumentException if the input cannot be parsed
     */
    fun resolve(input: String, context: CommandContext): T

    /**
     * Provides tab-completion suggestions for the given partial [input].
     *
     * @param input the current partial input being typed
     * @param context the current command execution context
     * @return list of completion suggestions
     */
    fun complete(input: String, context: CommandContext): List<String> = emptyList()
}
