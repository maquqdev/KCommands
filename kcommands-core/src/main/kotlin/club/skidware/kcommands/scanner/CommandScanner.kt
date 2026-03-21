package club.skidware.kcommands.scanner

import club.skidware.kcommands.annotation.Command
import club.skidware.kcommands.annotation.Permission
import club.skidware.kcommands.annotation.Description
import club.skidware.kcommands.argument.ArgumentResolverRegistry
import club.skidware.kcommands.completion.TabCompleter
import club.skidware.kcommands.model.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.primaryConstructor

/**
 * Scans an annotated command class instance and builds a [CommandNode] tree.
 *
 * Reads `@Command`, `@DefaultHandler`, `@SubCommand`, and related annotations
 * via reflection to construct the full command hierarchy with handlers.
 *
 * @param resolverRegistry the registry used to look up argument resolvers
 * @param completerFactory optional factory for resolving `@Complete` tab completer references
 */
class CommandScanner(
    private val resolverRegistry: ArgumentResolverRegistry,
    private val completerFactory: ((KClass<*>) -> TabCompleter?)? = null
) {

    /**
     * Scans the given command class [instance] and builds a [CommandNode] tree.
     *
     * @param instance an object annotated with `@Command`
     * @return the root command node with all handlers and subcommands
     * @throws IllegalArgumentException if the class has no `@Command` annotation
     */
    fun scan(instance: Any): CommandNode {
        val klass = instance::class
        val cmdAnn = klass.findAnnotation<Command>()
            ?: throw IllegalArgumentException("${klass.simpleName} has no @Command annotation")

        val node = CommandNode(
            name = cmdAnn.name.lowercase(),
            aliases = cmdAnn.aliases.map { it.lowercase() },
            permission = klass.findAnnotation<Permission>()?.node,
            permissionMessage = klass.findAnnotation<Permission>()?.message?.takeIf { it.isNotEmpty() },
            description = klass.findAnnotation<Description>()?.value
        )

        for (func in klass.memberFunctions) {
            when {
                AnnotationReader.isDefaultHandler(func) -> {
                    node.handlers.add(buildExecutor(func, instance))
                }
                AnnotationReader.isSubCommand(func) -> {
                    val subName = AnnotationReader.getSubCommandName(func)!!.lowercase()
                    val subAliases = AnnotationReader.getSubCommandAliases(func).map { it.lowercase() }
                    val childNode = node.children.getOrPut(subName) {
                        CommandNode(
                            name = subName,
                            aliases = subAliases,
                            permission = AnnotationReader.getPermission(func),
                            permissionMessage = AnnotationReader.getPermissionMessage(func),
                            description = AnnotationReader.getDescription(func)
                        )
                    }
                    childNode.handlers.add(buildExecutor(func, instance))
                }
            }
        }

        // Scan nested classes with @Command
        for (nested in klass.nestedClasses) {
            if (nested.hasAnnotation<Command>()) {
                val nestedInstance = createNestedInstance(nested, instance)
                val childNode = scan(nestedInstance)
                node.children[childNode.name] = childNode
            }
        }

        return node
    }

    private fun buildExecutor(func: KFunction<*>, instance: Any): ExecutorInfo {
        val parameters = mutableListOf<ParameterInfo>()
        val args = mutableListOf<ArgInfo>()

        for (param in func.parameters) {
            if (param.kind == KParameter.Kind.INSTANCE) continue

            when {
                AnnotationReader.isSender(param) -> {
                    parameters.add(ParameterInfo(kParam = param, isSender = true))
                }
                AnnotationReader.isFlag(param) -> {
                    val (flagName, flagShort, _) = AnnotationReader.getFlag(param)!!
                    parameters.add(ParameterInfo(
                        kParam = param,
                        isFlag = true,
                        flagName = flagName,
                        flagShort = flagShort
                    ))
                }
                else -> {
                    val argName = AnnotationReader.getArgName(param)
                    if (argName != null) {
                        val type = param.type.classifier as? KClass<*> ?: continue
                        val isGreedy = AnnotationReader.isGreedy(param)
                        val range = AnnotationReader.getRange(param)
                        val completeClass = AnnotationReader.getComplete(param)
                        val customCompleter = completeClass?.let { completerFactory?.invoke(it) }

                        val resolver = resolverRegistry.get(type)
                            ?: resolverRegistry.getOrCreateEnum(type)

                        val isRequired = !param.isOptional

                        args.add(ArgInfo(
                            name = argName,
                            type = type,
                            required = isRequired,
                            isGreedy = isGreedy,
                            resolver = resolver ?: club.skidware.kcommands.argument.resolvers.StringResolver,
                            customCompleter = customCompleter,
                            range = range
                        ))

                        parameters.add(ParameterInfo(
                            kParam = param,
                            isArg = true,
                            argName = argName
                        ))
                    }
                }
            }
        }

        return ExecutorInfo(
            function = func,
            instance = instance,
            parameters = parameters,
            args = args,
            permission = AnnotationReader.getPermission(func),
            permissionMessage = AnnotationReader.getPermissionMessage(func),
            description = AnnotationReader.getDescription(func),
            isAsync = AnnotationReader.isAsync(func),
            cooldown = AnnotationReader.getCooldown(func),
            confirm = AnnotationReader.getConfirm(func),
            playerOnly = AnnotationReader.isPlayerOnly(func),
            consoleOnly = AnnotationReader.isConsoleOnly(func)
        )
    }

    private fun createNestedInstance(nested: KClass<*>, parent: Any): Any {
        val constructor = nested.primaryConstructor
        return if (constructor != null && constructor.parameters.isEmpty()) {
            constructor.call()
        } else if (constructor != null && constructor.parameters.size == 1) {
            constructor.call(parent)
        } else {
            nested.java.getDeclaredConstructor().newInstance()
        }
    }
}
