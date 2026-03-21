package club.skidware.kcommands.model

import club.skidware.kcommands.argument.ArgumentResolver
import club.skidware.kcommands.completion.TabCompleter
import kotlin.reflect.KClass

/**
 * Describes a single positional argument for a command handler.
 *
 * @property name the argument name (used in usage strings and parsed arg maps)
 * @property type the Kotlin type of the argument
 * @property required whether this argument must be provided
 * @property isGreedy if true, this argument consumes all remaining input tokens
 * @property resolver the resolver used to parse the raw string into [type]
 * @property customCompleter optional custom tab completer for this argument
 * @property range optional numeric range constraint for validation
 */
data class ArgInfo(
    val name: String,
    val type: KClass<*>,
    val required: Boolean,
    val isGreedy: Boolean = false,
    val resolver: ArgumentResolver<*>,
    val customCompleter: TabCompleter? = null,
    val range: ClosedFloatingPointRange<Double>? = null
)
