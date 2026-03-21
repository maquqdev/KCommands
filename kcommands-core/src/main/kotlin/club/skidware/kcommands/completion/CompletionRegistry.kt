package club.skidware.kcommands.completion

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Registry for named and class-keyed [TabCompleter] instances.
 *
 * Allows looking up custom completers by string name or by KClass reference.
 */
class CompletionRegistry {

    private val completers = ConcurrentHashMap<String, TabCompleter>()
    private val classCompleters = ConcurrentHashMap<KClass<*>, TabCompleter>()

    /** Registers a completer by name (case-insensitive). */
    fun register(name: String, completer: TabCompleter) {
        completers[name.lowercase()] = completer
    }

    /** Registers a completer by class type. */
    fun register(klass: KClass<*>, completer: TabCompleter) {
        classCompleters[klass] = completer
    }

    fun get(name: String): TabCompleter? = completers[name.lowercase()]

    fun get(klass: KClass<*>): TabCompleter? = classCompleters[klass]

    fun has(name: String): Boolean = completers.containsKey(name.lowercase())

    fun has(klass: KClass<*>): Boolean = classCompleters.containsKey(klass)

    /** Removes all registered completers. */
    fun unregisterAll() {
        completers.clear()
        classCompleters.clear()
    }
}
