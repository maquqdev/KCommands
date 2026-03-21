package club.skidware.kcommands.argument

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Thread-safe registry mapping Kotlin types to their [ArgumentResolver] instances.
 *
 * Used by the framework to look up how to parse each argument type.
 */
class ArgumentResolverRegistry {
    private val resolvers = ConcurrentHashMap<KClass<*>, ArgumentResolver<*>>()

    /**
     * Registers an [ArgumentResolver] for the given type.
     *
     * @param T the argument type
     * @param klass the KClass of the argument type
     * @param resolver the resolver to register
     */
    fun <T : Any> register(klass: KClass<T>, resolver: ArgumentResolver<T>) {
        resolvers[klass] = resolver
    }

    /**
     * Retrieves the resolver for the given type, or null if none is registered.
     *
     * @param T the argument type
     * @param klass the KClass to look up
     * @return the registered resolver, or null
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(klass: KClass<T>): ArgumentResolver<T>? {
        return resolvers[klass] as? ArgumentResolver<T>
    }

    /**
     * Returns or lazily creates an [EnumResolver][club.skidware.kcommands.argument.resolvers.EnumResolver]
     * for the given enum class. Returns null if [klass] is not an enum.
     *
     * @param klass the potential enum class
     * @return the resolver, or null if not an enum
     */
    @Suppress("UNCHECKED_CAST")
    fun getOrCreateEnum(klass: KClass<*>): ArgumentResolver<*>? {
        if (!klass.java.isEnum) return null
        return resolvers.getOrPut(klass) {
            club.skidware.kcommands.argument.resolvers.EnumResolver(klass.java as Class<out Enum<*>>)
        }
    }

    /** Returns true if a resolver is registered for [klass], or if [klass] is an enum type. */
    fun has(klass: KClass<*>): Boolean = resolvers.containsKey(klass) || klass.java.isEnum
}
