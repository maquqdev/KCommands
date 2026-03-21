package club.skidware.kcommands.di

import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

/**
 * Simple dependency injection container for binding and resolving instances by type.
 *
 * Supports manual binding and automatic constructor injection via [createInstance].
 */
class DependencyContainer {
    private val bindings = mutableMapOf<KClass<*>, Any>()

    /**
     * Binds an [instance] to the given [klass] type.
     *
     * @param T the binding type
     * @param klass the KClass to bind to
     * @param instance the instance to register
     */
    fun <T : Any> bind(klass: KClass<T>, instance: T) {
        bindings[klass] = instance
    }

    /**
     * Retrieves the bound instance for the given [klass], or null if not bound.
     *
     * @param T the expected type
     * @param klass the KClass to look up
     * @return the bound instance, or null
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(klass: KClass<T>): T? = bindings[klass] as? T

    /**
     * Creates a new instance of [klass] by resolving all primary constructor parameters
     * from this container.
     *
     * @param T the type to create
     * @param klass the KClass to instantiate
     * @return the created instance
     * @throws IllegalArgumentException if the class has no primary constructor or a dependency is missing
     */
    fun <T : Any> createInstance(klass: KClass<T>): T {
        val constructor = klass.primaryConstructor
            ?: throw IllegalArgumentException("${klass.simpleName} has no primary constructor")
        val args = constructor.parameters.associateWith { param ->
            val paramKlass = param.type.classifier as? KClass<*>
                ?: throw IllegalArgumentException("Cannot resolve parameter type for '${param.name}'")
            get(paramKlass)
                ?: throw IllegalArgumentException("No binding for ${paramKlass.simpleName}")
        }
        return constructor.callBy(args)
    }

    /** Returns true if a binding exists for the given [klass]. */
    fun has(klass: KClass<*>): Boolean = bindings.containsKey(klass)
}
