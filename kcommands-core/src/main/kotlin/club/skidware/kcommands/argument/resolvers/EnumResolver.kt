package club.skidware.kcommands.argument.resolvers

import club.skidware.kcommands.argument.ArgumentException
import club.skidware.kcommands.argument.ArgumentResolver
import club.skidware.kcommands.execution.CommandContext

/** Generic resolver for any enum type. Matches enum constant names case-insensitively. */
class EnumResolver(private val enumClass: Class<out Enum<*>>) : ArgumentResolver<Enum<*>> {
    private val constants: Array<out Enum<*>> = enumClass.enumConstants

    override fun resolve(input: String, context: CommandContext): Enum<*> =
        constants.firstOrNull { it.name.equals(input, ignoreCase = true) }
            ?: throw ArgumentException(
                "'$input' is not a valid ${enumClass.simpleName}. " +
                    "Valid values: ${constants.joinToString(", ") { it.name.lowercase() }}"
            )

    override fun complete(input: String, context: CommandContext): List<String> =
        constants
            .map { it.name.lowercase() }
            .filter { it.startsWith(input.lowercase()) }
}
