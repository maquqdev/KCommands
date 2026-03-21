package club.skidware.kcommands.execution

import club.skidware.kcommands.argument.ArgumentException
import club.skidware.kcommands.argument.ArgumentResolverRegistry
import club.skidware.kcommands.dsl.DslExecutorWrapper
import club.skidware.kcommands.event.KCommandCompleteEvent
import club.skidware.kcommands.event.KCommandErrorEvent
import club.skidware.kcommands.event.KCommandExecuteEvent
import club.skidware.kcommands.help.UsageFormatter
import club.skidware.kcommands.middleware.AsyncMiddleware
import club.skidware.kcommands.middleware.CommandMiddleware
import club.skidware.kcommands.middleware.ConfirmMiddleware
import club.skidware.kcommands.model.CommandNode
import club.skidware.kcommands.model.ExecutorInfo
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import kotlin.reflect.KParameter

/**
 * Dispatches incoming command executions and tab-completion requests for a single [CommandNode].
 *
 * Handles subcommand routing, argument resolution, middleware pipeline execution,
 * async dispatch, confirmation flow, and Bukkit event firing.
 */
class CommandDispatcher(
    private val plugin: Plugin,
    private val node: CommandNode,
    private val resolverRegistry: ArgumentResolverRegistry,
    private val errorHandler: ErrorHandler,
    private val middlewares: List<CommandMiddleware>,
    private val asyncMiddleware: AsyncMiddleware? = null,
    private val confirmMiddleware: ConfirmMiddleware? = null
) {

    /**
     * Dispatches a command execution for the given [context].
     *
     * Routes to the correct subcommand, resolves arguments, runs the middleware pipeline,
     * and invokes the matched handler. Fires [KCommandExecuteEvent] before execution
     * and [KCommandCompleteEvent] after.
     *
     * @param context the command execution context
     * @return true (always returns true to indicate the command was handled)
     */
    fun dispatch(context: CommandContext): Boolean {
        // Fire pre-execute event
        val executeEvent = KCommandExecuteEvent(context.sender, context.label, context.rawArgs, node)
        Bukkit.getPluginManager().callEvent(executeEvent)
        if (executeEvent.isCancelled) return true

        // Check node-level permission
        if (node.permission != null && !context.sender.hasPermission(node.permission!!)) {
            errorHandler.onNoPermission(context, node.permission!!)
            return true
        }

        val args = context.rawArgs.toList()

        // Navigate subcommand tree
        val matchResult = HandlerMatcher.match(node, args)

        if (matchResult == null) {
            // No match found
            if (args.isNotEmpty() && node.children.isNotEmpty()) {
                val available = HandlerMatcher.collectAvailableSubcommands(node)
                errorHandler.onUnknownSubcommand(context, args[0], available)
            } else {
                errorHandler.onShowHelp(context, node)
            }
            return true
        }

        val matchedNode = matchResult.node
        val handler = matchResult.handler
        val remainingArgs = matchResult.remainingArgs

        // Check subcommand-level permission
        if (matchedNode.permission != null && matchedNode != node && !context.sender.hasPermission(matchedNode.permission!!)) {
            errorHandler.onNoPermission(context, matchedNode.permission!!)
            return true
        }

        // Extract flags and positional args
        val (positionalArgs, flags) = extractFlags(remainingArgs, handler)
        context.flags.putAll(flags)

        // Resolve arguments
        val resolvedArgs: Map<String, Any?>
        try {
            resolvedArgs = resolveArguments(positionalArgs, handler, context, matchedNode, matchResult.parentPath)
        } catch (e: ArgumentException) {
            // Already handled inside resolveArguments
            return true
        } catch (e: Exception) {
            errorHandler.onCommandException(context, e)
            fireErrorEvent(context, e, matchedNode)
            return true
        }
        context.parsedArgs.putAll(resolvedArgs)

        // Execute with middleware pipeline
        executeHandler(context, handler, matchedNode)
        return true
    }

    private fun executeHandler(context: CommandContext, handler: ExecutorInfo, matchedNode: CommandNode) {
        // Run middleware before
        for (middleware in middlewares) {
            if (!middleware.before(context, handler)) {
                // Middleware blocked execution
                // Special case: ConfirmMiddleware blocks to request confirmation
                if (middleware is ConfirmMiddleware && handler.confirm != null && context.sender is Player) {
                    confirmMiddleware?.requestConfirmation(
                        context.sender as Player,
                        handler,
                        context
                    ) {
                        invokeHandler(context, handler, matchedNode)
                    }
                }
                return
            }
        }

        // Check if async
        if (handler.isAsync && asyncMiddleware != null) {
            asyncMiddleware.executeAsync(context, handler) {
                invokeAndFinish(context, handler, matchedNode)
            }
        } else {
            invokeAndFinish(context, handler, matchedNode)
        }
    }

    private fun invokeAndFinish(context: CommandContext, handler: ExecutorInfo, matchedNode: CommandNode) {
        var exception: Exception? = null
        try {
            invokeHandler(context, handler, matchedNode)
        } catch (e: Exception) {
            exception = e
            errorHandler.onCommandException(context, e)
            fireErrorEvent(context, e, matchedNode)
        } finally {
            // Run middleware after
            for (middleware in middlewares) {
                middleware.after(context, handler, exception)
            }
            fireCompleteEvent(context, exception == null, exception)
        }
    }

    private fun invokeHandler(context: CommandContext, handler: ExecutorInfo, matchedNode: CommandNode) {
        // Special handling for DSL-registered commands
        val instance = handler.instance
        if (instance is DslExecutorWrapper) {
            instance.setCurrentArgs(context.parsedArgs.toMap())
        }

        val func = handler.function
        val callArgs = mutableMapOf<KParameter, Any?>()

        // Instance parameter
        for (param in func.parameters) {
            if (param.kind == KParameter.Kind.INSTANCE) {
                callArgs[param] = handler.instance
            }
        }

        // Map annotated parameters
        for (paramInfo in handler.parameters) {
            when {
                paramInfo.isSender -> {
                    callArgs[paramInfo.kParam] = context.sender
                }
                paramInfo.isArg -> {
                    val value = context.parsedArgs[paramInfo.argName]
                    if (value != null) {
                        callArgs[paramInfo.kParam] = value
                    } else if (!paramInfo.kParam.isOptional) {
                        callArgs[paramInfo.kParam] = null
                    }
                    // If optional and value is null, omit from callArgs so default is used
                }
                paramInfo.isFlag -> {
                    val value = context.flags[paramInfo.flagName]
                    if (value != null) {
                        callArgs[paramInfo.kParam] = value
                    } else if (!paramInfo.kParam.isOptional) {
                        // Boolean flags default to false
                        val type = paramInfo.kParam.type.classifier
                        if (type == Boolean::class) {
                            callArgs[paramInfo.kParam] = false
                        } else {
                            callArgs[paramInfo.kParam] = null
                        }
                    }
                }
            }
        }

        func.callBy(callArgs)
    }

    private fun resolveArguments(
        positionalArgs: List<String>,
        handler: ExecutorInfo,
        context: CommandContext,
        matchedNode: CommandNode,
        parentPath: String
    ): Map<String, Any?> {
        val resolved = mutableMapOf<String, Any?>()

        for ((index, argInfo) in handler.args.withIndex()) {
            if (index >= positionalArgs.size) {
                // Missing argument
                if (argInfo.required) {
                    val usage = UsageFormatter.format(matchedNode, handler, parentPath)
                    errorHandler.onMissingArgument(context, argInfo.name, usage)
                    throw ArgumentException("Missing required argument: ${argInfo.name}")
                }
                continue
            }

            val input = if (argInfo.isGreedy) {
                // Greedy: consume all remaining args
                positionalArgs.subList(index, positionalArgs.size).joinToString(" ")
            } else {
                positionalArgs[index]
            }

            try {
                val value = argInfo.resolver.resolve(input, context)

                // Validate range
                if (argInfo.range != null && value is Number) {
                    val doubleVal = value.toDouble()
                    if (doubleVal !in argInfo.range) {
                        errorHandler.onOutOfRange(context, argInfo.name, value, argInfo.range)
                        throw ArgumentException("Value out of range for ${argInfo.name}")
                    }
                }

                resolved[argInfo.name] = value
            } catch (e: ArgumentException) {
                errorHandler.onInvalidArgument(context, argInfo.name, input, e.message ?: "Invalid input")
                throw e
            }

            if (argInfo.isGreedy) break
        }

        return resolved
    }

    private fun extractFlags(
        args: List<String>,
        handler: ExecutorInfo
    ): Pair<List<String>, Map<String, Any?>> {
        val positional = mutableListOf<String>()
        val flags = mutableMapOf<String, Any?>()
        val flagParams = handler.parameters.filter { it.isFlag }

        var i = 0
        while (i < args.size) {
            val arg = args[i]

            if (arg.startsWith("--")) {
                // Long flag: --name or --name value
                val flagName = arg.removePrefix("--")
                val flagParam = flagParams.firstOrNull { it.flagName == flagName }

                if (flagParam != null) {
                    val type = flagParam.kParam.type.classifier
                    if (type == Boolean::class) {
                        flags[flagName] = true
                    } else if (i + 1 < args.size) {
                        i++
                        flags[flagName] = args[i]
                    }
                } else {
                    positional.add(arg)
                }
            } else if (arg.startsWith("-") && arg.length == 2 && !arg[1].isDigit()) {
                // Short flag: -s
                val short = arg[1]
                val flagParam = flagParams.firstOrNull { it.flagShort == short }

                if (flagParam != null) {
                    val type = flagParam.kParam.type.classifier
                    if (type == Boolean::class) {
                        flags[flagParam.flagName ?: short.toString()] = true
                    } else if (i + 1 < args.size) {
                        i++
                        flags[flagParam.flagName ?: short.toString()] = args[i]
                    }
                } else {
                    positional.add(arg)
                }
            } else {
                positional.add(arg)
            }
            i++
        }

        return positional to flags
    }

    /**
     * Generates tab-completion suggestions for the given [context].
     *
     * Provides completions for subcommand names, argument values (via resolvers or custom completers),
     * and flags.
     *
     * @param context the command execution context with partial input
     * @return sorted, deduplicated list of completion suggestions
     */
    fun complete(context: CommandContext): List<String> {
        val args = context.rawArgs.toList()

        // Navigate to the correct node
        var currentNode = node
        var argIndex = 0

        while (argIndex < args.size - 1) {
            val child = HandlerMatcher.findChild(currentNode, args[argIndex])
            if (child != null) {
                currentNode = child
                argIndex++
            } else {
                break
            }
        }

        val currentInput = if (args.isNotEmpty()) args.last() else ""
        val completions = mutableListOf<String>()

        // Complete subcommand names
        if (argIndex == args.size - 1 || args.isEmpty()) {
            for ((name, child) in currentNode.children) {
                if (child.permission != null && !context.sender.hasPermission(child.permission!!)) continue
                if (name.lowercase().startsWith(currentInput.lowercase())) {
                    completions.add(name)
                }
                for (alias in child.aliases) {
                    if (alias.lowercase().startsWith(currentInput.lowercase())) {
                        completions.add(alias)
                    }
                }
            }
        }

        // Complete arguments for current handler
        val positionalIndex = args.size - argIndex - 1
        val handler = currentNode.handlers.all().firstOrNull()
        if (handler != null && positionalIndex in handler.args.indices) {
            val argInfo = handler.args[positionalIndex]

            // Custom completer first
            val customCompletions = argInfo.customCompleter?.complete(currentInput, context)
            if (customCompletions != null) {
                completions.addAll(customCompletions)
            } else {
                // Resolver completions
                completions.addAll(argInfo.resolver.complete(currentInput, context))
            }
        }

        // Complete flags
        if (currentInput.startsWith("-")) {
            val allHandlers = currentNode.handlers.all()
            for (h in allHandlers) {
                for (param in h.parameters.filter { it.isFlag }) {
                    val longFlag = "--${param.flagName}"
                    if (longFlag.startsWith(currentInput)) {
                        completions.add(longFlag)
                    }
                    if (param.flagShort != ' ') {
                        val shortFlag = "-${param.flagShort}"
                        if (shortFlag.startsWith(currentInput)) {
                            completions.add(shortFlag)
                        }
                    }
                }
            }
        }

        return completions.distinct().sorted()
    }

    private fun fireErrorEvent(context: CommandContext, exception: Exception, node: CommandNode) {
        try {
            val event = KCommandErrorEvent(context.sender, context.label, exception, node)
            Bukkit.getPluginManager().callEvent(event)
        } catch (_: Exception) {
            // Ignore event errors
        }
    }

    private fun fireCompleteEvent(context: CommandContext, success: Boolean, exception: Exception?) {
        try {
            val event = KCommandCompleteEvent(context.sender, context.label, success, exception)
            Bukkit.getPluginManager().callEvent(event)
        } catch (_: Exception) {
            // Ignore event errors
        }
    }
}
