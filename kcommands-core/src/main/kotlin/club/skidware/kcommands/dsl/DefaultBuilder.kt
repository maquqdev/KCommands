package club.skidware.kcommands.dsl

import club.skidware.kcommands.argument.ArgumentResolverRegistry
import club.skidware.kcommands.model.ArgInfo
import club.skidware.kcommands.model.ConfirmInfo
import club.skidware.kcommands.model.CooldownInfo
import club.skidware.kcommands.model.ExecutorInfo
import club.skidware.kcommands.model.ParameterInfo
import org.bukkit.command.CommandSender
import kotlin.reflect.KFunction

/**
 * DSL builder for a default (root-level) command handler.
 *
 * Configures arguments, permissions, async behavior, cooldowns, confirmations,
 * and the execution block.
 */
class DefaultBuilder(
    @PublishedApi internal val resolverRegistry: ArgumentResolverRegistry
) {
    @PublishedApi internal val args = mutableListOf<ArgInfo>()
    var permission: String? = null
    var description: String? = null
    var playerOnly: Boolean = false
    var consoleOnly: Boolean = false
    var async: Boolean = false
    var cooldownSeconds: Int = 0
    var cooldownMessage: String = ""
    var cooldownBypass: String = ""
    var confirmMessage: String = ""
    var confirmTimeout: Int = 15

    private var executeBlock: ((CommandSender, Map<String, Any?>) -> Unit)? = null

    /**
     * Declares a typed argument with the given [name].
     *
     * @param T the argument type (must have a registered resolver)
     * @param name the argument name
     * @param block optional configuration block for the argument
     * @return the argument builder
     */
    inline fun <reified T : Any> arg(name: String, block: ArgBuilder<T>.() -> Unit = {}): ArgBuilder<T> {
        val builder = ArgBuilder(name, T::class, resolverRegistry)
        builder.block()
        args.add(builder.build())
        return builder
    }

    /** Sets the execution block that runs when this handler is invoked. */
    fun execute(block: (sender: CommandSender, args: Map<String, Any?>) -> Unit) {
        this.executeBlock = block
    }

    internal fun build(): ExecutorInfo? {
        val handler = executeBlock ?: return null

        val wrapperInstance = DslExecutorWrapper(handler)
        val wrapperFunction = DslExecutorWrapper::class.members
            .filterIsInstance<KFunction<*>>()
            .first { it.name == "execute" }

        val parameters = mutableListOf<ParameterInfo>()

        // The DslExecutorWrapper.execute(sender) has one parameter (plus the instance param).
        // Find the sender parameter for mapping.
        val senderParam = wrapperFunction.parameters.firstOrNull { it.name == "sender" }
        if (senderParam != null) {
            parameters.add(ParameterInfo(kParam = senderParam, isSender = true))
        }

        return ExecutorInfo(
            function = wrapperFunction,
            instance = wrapperInstance,
            parameters = parameters,
            args = args.toList(),
            permission = permission,
            description = description,
            isAsync = async,
            cooldown = if (cooldownSeconds > 0) CooldownInfo(cooldownSeconds, cooldownMessage, cooldownBypass) else null,
            confirm = if (confirmMessage.isNotEmpty()) ConfirmInfo(confirmMessage, confirmTimeout) else null,
            playerOnly = playerOnly,
            consoleOnly = consoleOnly
        )
    }
}

/**
 * Wrapper that bridges the DSL execute lambda with the reflection-based dispatcher.
 *
 * The dispatcher calls [execute] via KFunction.callBy(), passing the sender.
 * The parsed args are provided via [setCurrentArgs] just before invocation
 * by the dispatcher (which detects DslExecutorWrapper instances).
 */
class DslExecutorWrapper(
    private val handler: (CommandSender, Map<String, Any?>) -> Unit
) {
    @Volatile
    private var currentArgs: Map<String, Any?> = emptyMap()

    fun setCurrentArgs(args: Map<String, Any?>) {
        this.currentArgs = args
    }

    fun execute(sender: CommandSender) {
        handler(sender, currentArgs)
    }
}
