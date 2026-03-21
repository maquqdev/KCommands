package club.skidware.kcommands

import club.skidware.kcommands.argument.ArgumentResolver
import club.skidware.kcommands.argument.ArgumentResolverRegistry
import club.skidware.kcommands.argument.BuiltinResolvers
import club.skidware.kcommands.completion.CompletionRegistry
import club.skidware.kcommands.completion.TabCompleter
import club.skidware.kcommands.di.DependencyContainer
import club.skidware.kcommands.dsl.CommandDslScope
import club.skidware.kcommands.error.CommandErrorFormatter
import club.skidware.kcommands.execution.CommandDispatcher
import club.skidware.kcommands.execution.DefaultErrorHandler
import club.skidware.kcommands.execution.ErrorHandler
import club.skidware.kcommands.message.CommandMessages
import club.skidware.kcommands.message.DefaultMessageProvider
import club.skidware.kcommands.message.IMessageProvider
import club.skidware.kcommands.middleware.*
import club.skidware.kcommands.model.CommandNode
import club.skidware.kcommands.registration.BrigadierRegistrar
import club.skidware.kcommands.registration.BukkitRegistrar
import club.skidware.kcommands.registration.CommandRegistrar
import club.skidware.kcommands.registration.PlatformDetector
import club.skidware.kcommands.scanner.CommandScanner
import club.skidware.kcommands.scanner.ValidationPass
import org.bukkit.plugin.Plugin
import kotlin.reflect.KClass

/**
 * Main entry point for the KCommands framework.
 *
 * Provides methods to set up the command system, register command classes or DSL-defined commands,
 * and manage argument resolvers. Must be initialized via [setup] before registering commands.
 *
 * ```kotlin
 * KCommands.setup(plugin) {
 *     brigadier(true)
 *     logging(true)
 *     messages { copy(noPermission = "<red>Nope!") }
 * }
 *
 * KCommands.register(plugin, MyCommand())
 * ```
 */
object KCommands {

    /** Registry for argument type resolvers used to parse command arguments. */
    val resolverRegistry = ArgumentResolverRegistry()

    /** Registry for custom tab completer. */
    val completionRegistry = CompletionRegistry()

    /** Dependency injection container for resolving constructor parameters. */
    val di = DependencyContainer()

    private val middlewares = mutableListOf<CommandMiddleware>()
    private val registeredNodes = mutableListOf<CommandNode>()
    private var registrar: CommandRegistrar? = null
    private var plugin: Plugin? = null
    /** The active error handler for command execution errors. */
    var errorHandler: ErrorHandler = DefaultErrorHandler()
        private set
    private var messageProvider: IMessageProvider = DefaultMessageProvider()
    private var asyncMiddleware: AsyncMiddleware? = null
    private var confirmMiddleware: ConfirmMiddleware? = null
    private var cooldownMiddleware: CooldownMiddleware? = null
    private var initialized = false

    /**
     * Initializes the KCommands framework for the given plugin.
     *
     * Registers built-in resolvers, applies configuration, sets up the command registrar
     * (Brigadier or Bukkit), and initializes built-in middleware (permissions, cooldowns, etc.).
     *
     * @param plugin the owning Bukkit plugin
     * @param block optional configuration block to customize messages, error handling, middleware, etc.
     */
    fun setup(plugin: Plugin, block: KCommandsConfig.() -> Unit = {}) {
        this.plugin = plugin

        // Register builtin resolvers
        BuiltinResolvers.registerAll(resolverRegistry)

        // Apply user config
        val config = KCommandsConfig()
        config.block()

        // Apply config
        if (config.customMessageProvider != null) {
            messageProvider = config.customMessageProvider!!
        } else if (config.customMessages != null) {
            messageProvider = DefaultMessageProvider(config.customMessages!!)
        }
        errorHandler = if (config.customErrorHandler != null) {
            config.customErrorHandler!!
        } else {
            DefaultErrorHandler(messageProvider)
        }

        // Setup registrar
        registrar = if (config.useBrigadier && PlatformDetector.hasBrigadier) {
            BrigadierRegistrar()
        } else {
            BukkitRegistrar()
        }

        // Setup built-in middleware
        asyncMiddleware = AsyncMiddleware(plugin)
        cooldownMiddleware = CooldownMiddleware(errorHandler)
        confirmMiddleware = ConfirmMiddleware(messageProvider)

        // Add built-in middleware in order
        middlewares.clear()
        middlewares.add(PlayerOnlyMiddleware(errorHandler))
        middlewares.add(PermissionMiddleware(errorHandler))
        middlewares.add(cooldownMiddleware!!)
        if (config.enableLogging) {
            middlewares.add(LoggingMiddleware(plugin.logger))
        }

        // Add user middleware
        middlewares.addAll(config.userMiddlewares)

        // Add confirm middleware last (it may block execution)
        middlewares.add(confirmMiddleware!!)

        initialized = true
    }

    /**
     * Registers one or more annotation-based command class instances.
     *
     * Each instance is validated and scanned for `@Command`, `@DefaultHandler`, `@SubCommand`, etc.
     * Invalid commands are logged as errors and skipped.
     *
     * @param plugin the owning Bukkit plugin
     * @param instances command class instances annotated with `@Command`
     */
    fun register(plugin: Plugin, vararg instances: Any) {
        ensureSetup(plugin)

        val scanner = CommandScanner(resolverRegistry) { klass ->
            try {
                val instance = di.get(klass as KClass<Any>)
                instance as? TabCompleter
            } catch (_: Exception) {
                try {
                    klass.java.getDeclaredConstructor().newInstance() as? TabCompleter
                } catch (_: Exception) {
                    null
                }
            }
        }

        val validator = ValidationPass(resolverRegistry) { klass ->
            try {
                klass.java.getDeclaredConstructor().newInstance()
            } catch (_: Exception) {
                null
            }
        }

        for (instance in instances) {
            // Validate
            val errors = validator.validate(instance)
            if (errors.isNotEmpty()) {
                val formatted = CommandErrorFormatter.format(errors)
                plugin.logger.severe(formatted)
                continue
            }

            // Scan
            try {
                val node = scanner.scan(instance)
                registerNode(plugin, node)
            } catch (e: Exception) {
                plugin.logger.severe("[KCommands] Failed to register ${instance::class.simpleName}: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Registers commands defined using the KCommands DSL.
     *
     * ```kotlin
     * KCommands.register(plugin) {
     *     command("hello") {
     *         description("Says hello")
     *         default {
     *             execute { sender, _ -> sender.sendMessage("Hello!") }
     *         }
     *     }
     * }
     * ```
     *
     * @param plugin the owning Bukkit plugin
     * @param block DSL scope for defining commands
     */
    fun register(plugin: Plugin, block: CommandDslScope.() -> Unit) {
        ensureSetup(plugin)

        val scope = CommandDslScope(resolverRegistry)
        scope.block()

        for (node in scope.commands) {
            registerNode(plugin, node)
        }
    }

    /**
     * Registers a custom argument resolver for the reified type [T].
     *
     * @param T the argument type to resolve
     * @param resolver the resolver implementation
     */
    inline fun <reified T : Any> registerResolver(resolver: ArgumentResolver<T>) {
        resolverRegistry.register(T::class, resolver)
    }

    /**
     * Registers a custom argument resolver for the specified [klass].
     *
     * @param T the argument type to resolve
     * @param klass the KClass of the argument type
     * @param resolver the resolver implementation
     */
    fun <T : Any> registerResolver(klass: KClass<T>, resolver: ArgumentResolver<T>) {
        resolverRegistry.register(klass, resolver)
    }

    /**
     * Unregisters all commands, clears cooldowns, pending confirmations, completers, and middleware.
     * Resets the framework to an uninitialized state.
     */
    fun unregisterAll() {
        val p = plugin ?: return
        registrar?.unregisterAll(p)
        registeredNodes.clear()
        cooldownMiddleware?.clearAllCooldowns()
        confirmMiddleware?.clearAll()
        completionRegistry.unregisterAll()
        middlewares.clear()
        initialized = false
    }

    /** Returns an immutable snapshot of all registered command nodes. */
    fun getRegisteredNodes(): List<CommandNode> = registeredNodes.toList()

    /** Returns the confirm middleware instance, or null if not yet initialized. */
    fun getConfirmMiddleware(): ConfirmMiddleware? = confirmMiddleware

    /** Returns the cooldown middleware instance, or null if not yet initialized. */
    fun getCooldownMiddleware(): CooldownMiddleware? = cooldownMiddleware

    private fun ensureSetup(plugin: Plugin) {
        if (!initialized) {
            setup(plugin)
        }
    }

    private fun registerNode(plugin: Plugin, node: CommandNode) {
        val dispatcher = CommandDispatcher(
            plugin = plugin,
            node = node,
            resolverRegistry = resolverRegistry,
            errorHandler = errorHandler,
            middlewares = middlewares,
            asyncMiddleware = asyncMiddleware,
            confirmMiddleware = confirmMiddleware
        )

        registrar?.register(plugin, node, dispatcher)
        registeredNodes.add(node)
    }

    /**
     * Configuration DSL for [KCommands.setup].
     *
     * Allows customizing messages, error handling, middleware, resolvers, and DI bindings.
     */
    class KCommandsConfig {
        internal var customMessages: CommandMessages? = null
        internal var customMessageProvider: IMessageProvider? = null
        internal var customErrorHandler: ErrorHandler? = null
        internal var useBrigadier: Boolean = true
        internal var enableLogging: Boolean = false
        internal val userMiddlewares = mutableListOf<CommandMiddleware>()

        /** Configures custom command messages by transforming the default [CommandMessages]. */
        fun messages(block: CommandMessages.() -> CommandMessages) {
            customMessages = CommandMessages().block()
        }

        /** Sets a pre-built [CommandMessages] instance for message customization. */
        fun messages(messages: CommandMessages) {
            customMessages = messages
        }

        /** Sets a fully custom [IMessageProvider] implementation for message resolution. */
        fun messageProvider(provider: IMessageProvider) {
            customMessageProvider = provider
        }

        /** Sets a custom [ErrorHandler] for all command error scenarios. */
        fun errorHandler(handler: ErrorHandler) {
            customErrorHandler = handler
        }

        /** Enables or disables Brigadier-based command registration (requires Paper 1.20.6+). */
        fun brigadier(enabled: Boolean) {
            useBrigadier = enabled
        }

        /** Enables or disables command execution logging to the plugin logger. */
        fun logging(enabled: Boolean) {
            enableLogging = enabled
        }

        /** Adds a custom [CommandMiddleware] to the execution pipeline. */
        fun middleware(middleware: CommandMiddleware) {
            userMiddlewares.add(middleware)
        }

        /** Registers a custom argument resolver for the reified type [T]. */
        inline fun <reified T : Any> resolver(resolver: ArgumentResolver<T>) {
            KCommands.resolverRegistry.register(T::class, resolver)
        }

        /** Registers a custom argument resolver for the given [klass]. */
        fun <T : Any> resolver(klass: KClass<T>, resolver: ArgumentResolver<T>) {
            KCommands.resolverRegistry.register(klass, resolver)
        }

        /** Binds an instance of type [T] in the dependency injection container. */
        inline fun <reified T : Any> bind(instance: T) {
            KCommands.di.bind(T::class, instance)
        }

        /** Binds an instance to the given [klass] in the dependency injection container. */
        fun <T : Any> bind(klass: KClass<T>, instance: T) {
            KCommands.di.bind(klass, instance)
        }
    }
}
