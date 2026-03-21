package club.skidware.kcommands.model

import kotlin.reflect.KParameter

/**
 * Describes how a Kotlin function parameter maps to a command concept
 * (sender, positional argument, or flag).
 *
 * @property kParam the underlying Kotlin reflection parameter
 * @property isSender true if this parameter receives the command sender
 * @property isArg true if this parameter is a positional argument
 * @property isFlag true if this parameter is a flag (e.g., `--verbose`)
 * @property argName the argument name (if [isArg])
 * @property flagName the long flag name (if [isFlag])
 * @property flagShort the short flag character (if [isFlag])
 */
data class ParameterInfo(
    val kParam: KParameter,
    val isSender: Boolean = false,
    val isArg: Boolean = false,
    val isFlag: Boolean = false,
    val argName: String? = null,
    val flagName: String? = null,
    val flagShort: Char = ' '
)
