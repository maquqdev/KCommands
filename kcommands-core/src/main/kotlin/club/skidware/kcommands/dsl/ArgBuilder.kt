package club.skidware.kcommands.dsl

import club.skidware.kcommands.argument.ArgumentResolver
import club.skidware.kcommands.argument.ArgumentResolverRegistry
import club.skidware.kcommands.completion.TabCompleter
import club.skidware.kcommands.execution.CommandContext
import club.skidware.kcommands.model.ArgInfo
import kotlin.reflect.KClass

/**
 * DSL builder for configuring a single command argument.
 *
 * @param T the argument type
 * @param name the argument name
 * @param type the KClass of the argument type
 * @param resolverRegistry the registry for looking up the default resolver
 */
class ArgBuilder<T : Any>(
    val name: String,
    val type: KClass<T>,
    private val resolverRegistry: ArgumentResolverRegistry
) {
    var required: Boolean = true
    var greedy: Boolean = false
    var range: ClosedFloatingPointRange<Double>? = null
    private var customCompleter: TabCompleter? = null
    private var customResolver: ArgumentResolver<T>? = null

    /** Sets a custom tab completer instance for this argument. */
    fun completer(completer: TabCompleter) {
        this.customCompleter = completer
    }

    /** Sets a custom tab completer using a lambda. */
    fun completer(block: (input: String, context: CommandContext) -> List<String>) {
        this.customCompleter = object : TabCompleter {
            override fun complete(input: String, context: CommandContext): List<String> {
                return block(input, context)
            }
        }
    }

    /** Overrides the default resolver for this argument. */
    fun resolver(resolver: ArgumentResolver<T>) {
        this.customResolver = resolver
    }

    /** Sets a numeric range constraint for this argument. */
    fun range(min: Double, max: Double) {
        this.range = min..max
    }

    /** Marks this argument as optional. */
    fun optional() {
        this.required = false
    }

    @PublishedApi internal fun build(): ArgInfo {
        @Suppress("UNCHECKED_CAST")
        val resolver = customResolver
            ?: resolverRegistry.get(type)
            ?: resolverRegistry.getOrCreateEnum(type as KClass<*>) as? ArgumentResolver<T>
            ?: throw IllegalArgumentException("No resolver registered for type ${type.simpleName}")

        return ArgInfo(
            name = name,
            type = type,
            required = required,
            isGreedy = greedy,
            resolver = resolver,
            customCompleter = customCompleter,
            range = range
        )
    }
}
