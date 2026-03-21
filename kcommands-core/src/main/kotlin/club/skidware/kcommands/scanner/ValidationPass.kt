package club.skidware.kcommands.scanner

import club.skidware.kcommands.annotation.*
import club.skidware.kcommands.argument.ArgumentResolverRegistry
import club.skidware.kcommands.error.*
import club.skidware.kcommands.model.CommandNode
import club.skidware.kcommands.model.ExecutorInfo
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.command.RemoteConsoleCommandSender
import org.bukkit.entity.Player
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberFunctions

/**
 * Validates annotated command classes and [CommandNode] trees for definition errors
 * before registration.
 *
 * Performs 20+ checks including missing annotations, invalid parameter types,
 * argument ordering, resolver availability, and conflicting annotations.
 *
 * @param resolverRegistry the registry used to verify resolver availability
 * @param completerFactory optional factory for verifying `@Complete` references
 */
class ValidationPass(
    private val resolverRegistry: ArgumentResolverRegistry,
    private val completerFactory: ((KClass<*>) -> Any?)? = null
) {

    private val validSenderTypes: Set<KClass<*>> = setOf(
        Player::class,
        CommandSender::class,
        ConsoleCommandSender::class,
        RemoteConsoleCommandSender::class
    )

    private val numericTypes: Set<KClass<*>> = setOf(
        Int::class,
        Long::class,
        Double::class,
        Float::class
    )

    /**
     * Validates an annotated command class instance before scanning.
     * Performs all 20 checks against the raw class and its annotated methods.
     */
    fun validate(instance: Any): List<CommandDefinitionError> {
        val errors = mutableListOf<CommandDefinitionError>()
        val klass = instance::class
        val className = klass.simpleName ?: klass.qualifiedName ?: "Unknown"

        // 1. Check @Command annotation present
        if (!klass.hasAnnotation<Command>()) {
            errors.add(MissingCommandAnnotation(className))
            return errors // Cannot continue without @Command
        }

        val handlerFunctions = klass.memberFunctions.filter { AnnotationReader.isDefaultHandler(it) }
        val subCommandFunctions = klass.memberFunctions.filter { AnnotationReader.isSubCommand(it) }
        val allHandlerFunctions = handlerFunctions + subCommandFunctions

        // 2. Check has handlers (also consider nested @Command classes)
        val hasNestedCommands = klass.nestedClasses.any { it.hasAnnotation<Command>() }
        if (allHandlerFunctions.isEmpty() && !hasNestedCommands) {
            errors.add(NoHandlers(className))
        }

        // 19. Check no duplicate SubCommand names
        val subCommandsByName = subCommandFunctions.groupBy {
            AnnotationReader.getSubCommandName(it)!!.lowercase()
        }
        for ((name, funcs) in subCommandsByName) {
            if (funcs.size > 1) {
                errors.add(DuplicateSubCommand(
                    className, name, funcs.map { it.name }
                ))
            }
        }

        // Validate each handler function (both @DefaultHandler and @SubCommand)
        for (func in allHandlerFunctions) {
            errors.addAll(validateFunction(func, className))
        }

        // 18. Check no ambiguous handler overloads (only among @DefaultHandler methods)
        if (handlerFunctions.size > 1) {
            errors.addAll(checkAmbiguousOverloads(handlerFunctions, className))
        }

        return errors
    }

    /**
     * Validates an already-built CommandNode tree.
     * Useful for post-scan validation of the constructed model.
     */
    fun validateNode(node: CommandNode, className: String): List<CommandDefinitionError> {
        val errors = mutableListOf<CommandDefinitionError>()

        // 2. Check has handlers
        if (node.handlers.isEmpty() && node.children.isEmpty()) {
            errors.add(NoHandlers(className))
        }

        // Validate all handlers in this node
        for (executor in node.handlers.all()) {
            errors.addAll(validateExecutor(executor, className))
        }

        // 18. Check ambiguous overloads within handler group
        val allHandlers = node.handlers.all()
        if (allHandlers.size > 1) {
            errors.addAll(checkAmbiguousExecutorOverloads(allHandlers, className))
        }

        // Recursively validate children
        for ((_, child) in node.children) {
            errors.addAll(validateNode(child, className))
        }

        return errors
    }

    private fun validateFunction(func: KFunction<*>, className: String): List<CommandDefinitionError> {
        val errors = mutableListOf<CommandDefinitionError>()
        val methodName = func.name
        val params = func.parameters.filter { it.kind != KParameter.Kind.INSTANCE }

        // 3. Check @Sender present
        val senderParams = params.filter { AnnotationReader.isSender(it) }
        if (senderParams.isEmpty()) {
            errors.add(MissingSenderParameter(className, methodName))
        }

        // 4. Check no multiple @Sender
        if (senderParams.size > 1) {
            errors.add(MultipleSenderParameters(
                className, methodName,
                senderParams.map { it.name ?: "param${it.index}" }
            ))
        }

        // 5. Check @Sender type is valid
        for (senderParam in senderParams) {
            val type = senderParam.type.classifier as? KClass<*>
            if (type != null && type !in validSenderTypes) {
                errors.add(InvalidSenderType(
                    className, methodName,
                    senderParam.name ?: "param${senderParam.index}",
                    type.simpleName ?: type.toString()
                ))
            }
        }

        // Collect non-sender, non-instance params
        val nonSenderParams = params.filter { !AnnotationReader.isSender(it) }

        // 6. Check all non-sender params have @Arg or @Flag
        for (param in nonSenderParams) {
            if (AnnotationReader.getArgName(param) == null && !AnnotationReader.isFlag(param)) {
                errors.add(MissingArgAnnotation(
                    className, methodName,
                    param.name ?: "param${param.index}"
                ))
            }
        }

        // Collect @Arg params for ordering checks
        val argParams = nonSenderParams.filter { AnnotationReader.getArgName(it) != null }

        // 7. Check no required after optional
        var firstOptional: KParameter? = null
        for (param in argParams) {
            if (param.isOptional) {
                if (firstOptional == null) firstOptional = param
            } else {
                if (firstOptional != null) {
                    errors.add(RequiredAfterOptional(
                        className, methodName,
                        param.name ?: "param${param.index}",
                        firstOptional.name ?: "param${firstOptional.index}"
                    ))
                }
            }
        }

        // 8. Check @Greedy is last arg
        val greedyParams = argParams.filter { AnnotationReader.isGreedy(it) }
        for (greedyParam in greedyParams) {
            val greedyIndex = argParams.indexOf(greedyParam)
            if (greedyIndex < argParams.size - 1) {
                val afterParam = argParams[greedyIndex + 1]
                errors.add(GreedyNotLast(
                    className, methodName,
                    greedyParam.name ?: "param${greedyParam.index}",
                    afterParam.name ?: "param${afterParam.index}"
                ))
            }
        }

        // 9. Check @Greedy is String type
        for (greedyParam in greedyParams) {
            val type = greedyParam.type.classifier as? KClass<*>
            if (type != null && type != String::class) {
                errors.add(GreedyNotString(
                    className, methodName,
                    greedyParam.name ?: "param${greedyParam.index}",
                    type.simpleName ?: type.toString()
                ))
            }
        }

        // 10. Check resolver exists for each arg type
        for (param in argParams) {
            val type = param.type.classifier as? KClass<*> ?: continue
            if (!resolverRegistry.has(type)) {
                errors.add(NoResolver(
                    className, methodName,
                    param.name ?: "param${param.index}",
                    type.simpleName ?: type.toString()
                ))
            }
        }

        // 11. Check @Range on numeric types only
        for (param in argParams) {
            val range = AnnotationReader.getRange(param)
            if (range != null) {
                val type = param.type.classifier as? KClass<*>
                if (type != null && type !in numericTypes) {
                    errors.add(RangeOnNonNumeric(
                        className, methodName,
                        param.name ?: "param${param.index}",
                        type.simpleName ?: type.toString()
                    ))
                }
            }
        }

        // 12. Check @Range min <= max
        for (param in argParams) {
            val range = AnnotationReader.getRange(param)
            if (range != null && range.start > range.endInclusive) {
                errors.add(RangeInverted(
                    className, methodName,
                    param.name ?: "param${param.index}",
                    range.start,
                    range.endInclusive
                ))
            }
        }

        // 13. Check no @PlayerOnly + @ConsoleOnly together
        if (AnnotationReader.isPlayerOnly(func) && AnnotationReader.isConsoleOnly(func)) {
            errors.add(PlayerOnlyOnConsoleOnly(className, methodName))
        }

        // 14. Check no @Async + @Confirm together
        if (AnnotationReader.isAsync(func) && AnnotationReader.getConfirm(func) != null) {
            errors.add(AsyncConfirmConflict(className, methodName))
        }

        // 15. Check @Cooldown seconds > 0
        val cooldown = AnnotationReader.getCooldown(func)
        if (cooldown != null && cooldown.seconds <= 0) {
            errors.add(CooldownNonPositive(className, methodName, cooldown.seconds))
        }

        // 16. Check no duplicate Flag short chars
        val flagParams = nonSenderParams.filter { AnnotationReader.isFlag(it) }
        val flagsByShort = flagParams
            .mapNotNull { param ->
                val flag = AnnotationReader.getFlag(param)
                if (flag != null && flag.second != ' ') {
                    flag.second to (param.name ?: "param${param.index}")
                } else null
            }
            .groupBy { it.first }

        for ((short, entries) in flagsByShort) {
            if (entries.size > 1) {
                errors.add(FlagDuplicateShort(
                    className, methodName, short,
                    entries.map { it.second }
                ))
            }
        }

        // 17. Check no duplicate Flag names
        val flagsByName = flagParams
            .mapNotNull { param ->
                val flag = AnnotationReader.getFlag(param)
                if (flag != null) {
                    flag.first to (param.name ?: "param${param.index}")
                } else null
            }
            .groupBy { it.first }

        for ((name, entries) in flagsByName) {
            if (entries.size > 1) {
                errors.add(FlagDuplicateName(
                    className, methodName, name,
                    entries.map { it.second }
                ))
            }
        }

        // Check @Permission on @DefaultHandler (error-should be on class or @SubCommand)
        if (AnnotationReader.isDefaultHandler(func) && AnnotationReader.getPermission(func) != null) {
            errors.add(PermissionOnHandler(className, methodName))
        }

        // 20. Check @Complete class can be resolved
        for (param in argParams) {
            val completeClass = AnnotationReader.getComplete(param) ?: continue
            val resolved = try {
                completerFactory?.invoke(completeClass)
            } catch (_: Exception) {
                null
            }
            if (resolved == null) {
                val canInstantiate = try {
                    completeClass.java.getDeclaredConstructor()
                    true
                } catch (_: NoSuchMethodException) {
                    false
                }
                if (!canInstantiate) {
                    errors.add(CompleterNotFound(
                        className, methodName,
                        param.name ?: "param${param.index}",
                        completeClass.simpleName ?: completeClass.toString()
                    ))
                }
            }
        }

        return errors
    }

    /**
     * Validates a pre-built ExecutorInfo from a CommandNode.
     * Used by [validateNode] for post-scan validation.
     */
    private fun validateExecutor(executor: ExecutorInfo, className: String): List<CommandDefinitionError> {
        val errors = mutableListOf<CommandDefinitionError>()
        val methodName = executor.function.name

        // 13. Check no @PlayerOnly + @ConsoleOnly together
        if (executor.playerOnly && executor.consoleOnly) {
            errors.add(PlayerOnlyOnConsoleOnly(className, methodName))
        }

        // 14. Check no @Async + @Confirm together
        if (executor.isAsync && executor.confirm != null) {
            errors.add(AsyncConfirmConflict(className, methodName))
        }

        // 15. Check @Cooldown seconds > 0
        if (executor.cooldown != null && executor.cooldown.seconds <= 0) {
            errors.add(CooldownNonPositive(className, methodName, executor.cooldown.seconds))
        }

        // Validate args ordering and constraints
        val args = executor.args
        var firstOptional: String? = null
        for ((index, arg) in args.withIndex()) {
            if (!arg.required) {
                if (firstOptional == null) firstOptional = arg.name
            } else {
                // 7. Required after optional
                if (firstOptional != null) {
                    errors.add(RequiredAfterOptional(className, methodName, arg.name, firstOptional))
                }
            }

            // 8. Greedy not last
            if (arg.isGreedy && index < args.size - 1) {
                errors.add(GreedyNotLast(className, methodName, arg.name, args[index + 1].name))
            }

            // 9. Greedy not String
            if (arg.isGreedy && arg.type != String::class) {
                errors.add(GreedyNotString(
                    className, methodName, arg.name,
                    arg.type.simpleName ?: arg.type.toString()
                ))
            }

            // 11. Range on non-numeric
            if (arg.range != null && arg.type !in numericTypes) {
                errors.add(RangeOnNonNumeric(
                    className, methodName, arg.name,
                    arg.type.simpleName ?: arg.type.toString()
                ))
            }

            // 12. Range inverted
            if (arg.range != null && arg.range.start > arg.range.endInclusive) {
                errors.add(RangeInverted(
                    className, methodName, arg.name,
                    arg.range.start, arg.range.endInclusive
                ))
            }
        }

        // 16. Duplicate flag short
        val flagShorts = executor.parameters
            .filter { it.isFlag && it.flagShort != ' ' }
            .groupBy { it.flagShort }

        for ((short, flagInfos) in flagShorts) {
            if (flagInfos.size > 1) {
                errors.add(FlagDuplicateShort(
                    className, methodName, short,
                    flagInfos.map { it.flagName ?: "unknown" }
                ))
            }
        }

        // 17. Duplicate flag names
        val flagNames = executor.parameters
            .filter { it.isFlag }
            .groupBy { it.flagName }

        for ((name, flagInfos) in flagNames) {
            if (name != null && flagInfos.size > 1) {
                errors.add(FlagDuplicateName(
                    className, methodName, name,
                    flagInfos.map { it.flagName ?: "unknown" }
                ))
            }
        }

        return errors
    }

    /**
     * Detects ambiguous @DefaultHandler overloads by grouping handlers
     * with the same number of required args.
     */
    private fun checkAmbiguousOverloads(
        handlers: List<KFunction<*>>,
        className: String
    ): List<CommandDefinitionError> {
        val errors = mutableListOf<CommandDefinitionError>()

        val byRequiredCount = handlers.groupBy { func ->
            func.parameters
                .filter { it.kind != KParameter.Kind.INSTANCE }
                .filter { AnnotationReader.getArgName(it) != null }
                .count { !it.isOptional }
        }

        for ((count, func) in byRequiredCount) {
            if (func.size > 1) {
                errors.add(AmbiguousOverload(
                    className,
                    func.map { it.name },
                    count
                ))
            }
        }

        return errors
    }

    /**
     * Detects ambiguous overloads among already-built ExecutorInfo objects.
     */
    private fun checkAmbiguousExecutorOverloads(
        handlers: List<ExecutorInfo>,
        className: String
    ): List<CommandDefinitionError> {
        val errors = mutableListOf<CommandDefinitionError>()

        val byRequiredCount = handlers.groupBy { executor ->
            executor.args.count { it.required }
        }

        for ((count, executors) in byRequiredCount) {
            if (executors.size > 1) {
                errors.add(AmbiguousOverload(
                    className,
                    executors.map { it.function.name },
                    count
                ))
            }
        }

        return errors
    }
}
