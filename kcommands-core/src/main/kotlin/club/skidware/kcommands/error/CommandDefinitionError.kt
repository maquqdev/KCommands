package club.skidware.kcommands.error

/**
 * Sealed hierarchy of errors detected during command class validation.
 *
 * Each subclass represents a specific validation failure (e.g., missing annotation,
 * invalid parameter type, conflicting annotations). Contains the error location,
 * a human-readable message, a fix suggestion, and an optional code hint.
 */
sealed class CommandDefinitionError(
    val commandClass: String,
    val method: String?,
    val parameter: String?,
    val message: String,
    val suggestion: String,
    val codeHint: String? = null
)

class MissingArgAnnotation(
    commandClass: String, method: String, parameter: String
) : CommandDefinitionError(
    commandClass, method, parameter,
    message = "Parameter '$parameter' has no @Arg or @Sender annotation",
    suggestion = "Add @Arg(\"$parameter\") or @Sender to this parameter",
    codeHint = """
        fun $method(
            @Sender sender: Player,
            @Arg("$parameter") $parameter: ...  // <- add @Arg
        )
    """.trimIndent()
)

class MissingSenderParameter(
    commandClass: String, method: String
) : CommandDefinitionError(
    commandClass, method, null,
    message = "No @Sender parameter found",
    suggestion = "Add a parameter annotated with @Sender (Player, CommandSender, or ConsoleCommandSender)",
    codeHint = """
        fun $method(
            @Sender sender: Player,  // <- add this
            ...
        )
    """.trimIndent()
)

class MultipleSenderParameters(
    commandClass: String, method: String, params: List<String>
) : CommandDefinitionError(
    commandClass, method, null,
    message = "Multiple @Sender parameters: [${params.joinToString()}]",
    suggestion = "Only one parameter can be annotated with @Sender",
    codeHint = null
)

class InvalidSenderType(
    commandClass: String, method: String, parameter: String, actualType: String
) : CommandDefinitionError(
    commandClass, method, parameter,
    message = "@Sender type '$actualType' is not a valid sender type",
    suggestion = "Use Player, CommandSender, or ConsoleCommandSender",
    codeHint = """
        fun $method(
            @Sender sender: Player,  // <- Player, CommandSender, or ConsoleCommandSender
            ...
        )
    """.trimIndent()
)

class RequiredAfterOptional(
    commandClass: String, method: String, requiredParam: String, optionalParam: String
) : CommandDefinitionError(
    commandClass, method, requiredParam,
    message = "Required arg '$requiredParam' comes after optional arg '$optionalParam'",
    suggestion = "Move all required args before optional ones",
    codeHint = """
        fun $method(
            @Sender sender: Player,
            @Arg("required1") ...,     // <- required first
            @Arg("required2") ...,
            @Arg("optional1") ... = defaultValue  // <- optional last
        )
    """.trimIndent()
)

class GreedyNotLast(
    commandClass: String, method: String, greedyParam: String, afterParam: String
) : CommandDefinitionError(
    commandClass, method, greedyParam,
    message = "@Greedy arg '$greedyParam' is not the last argument (followed by '$afterParam')",
    suggestion = "@Greedy must be the last positional argument - it consumes all remaining input",
    codeHint = """
        fun $method(
            @Sender sender: Player,
            @Arg("other") other: Player,
            @Arg("$greedyParam") @Greedy $greedyParam: String  // <- always last
        )
    """.trimIndent()
)

class GreedyNotString(
    commandClass: String, method: String, greedyParam: String, actualType: String
) : CommandDefinitionError(
    commandClass, method, greedyParam,
    message = "@Greedy arg '$greedyParam' is $actualType, but must be String",
    suggestion = "@Greedy joins remaining args into one String - only String type makes sense",
    codeHint = """
        @Arg("$greedyParam") @Greedy $greedyParam: String  // <- String, not $actualType
    """.trimIndent()
)

class NoResolver(
    commandClass: String, method: String, parameter: String, type: String
) : CommandDefinitionError(
    commandClass, method, parameter,
    message = "No ArgumentResolver registered for type '$type'",
    suggestion = "Register a resolver in KCommands.setup() or use a built-in type",
    codeHint = """
        // Option 1: Register a resolver
        KCommands.setup(plugin) {
            resolver(${type}::class, ${type}Resolver())
        }

        // Option 2: Use a built-in type
        // Available: String, Int, Long, Double, Float, Boolean,
        //           Player, OfflinePlayer, Material, World,
        //           GameMode, Duration, UUID, any Enum
    """.trimIndent()
)

class AmbiguousOverload(
    commandClass: String, handlers: List<String>, requiredCount: Int
) : CommandDefinitionError(
    commandClass, null, null,
    message = "Ambiguous @DefaultHandler overload - ${handlers.size} handlers with $requiredCount required args: [${handlers.joinToString()}]",
    suggestion = "Each @DefaultHandler must have a unique number of required (non-default) arguments",
    codeHint = """
        // WRONG - both have 1 required arg:
        @DefaultHandler fun a(@Sender s: Player, @Arg("x") x: Player) { }
        @DefaultHandler fun b(@Sender s: Player, @Arg("y") y: String) { }

        // CORRECT - different required count:
        @DefaultHandler fun a(@Sender s: Player, @Arg("x") x: Player) { }          // 1 arg
        @DefaultHandler fun b(@Sender s: Player, @Arg("x") x: Player, @Arg("y") y: Player) { }  // 2 args
    """.trimIndent()
)

class MissingCommandAnnotation(
    commandClass: String
) : CommandDefinitionError(
    commandClass, null, null,
    message = "Class '$commandClass' has no @Command annotation",
    suggestion = "Add @Command(\"name\") to the class",
    codeHint = """
        @Command("mycommand")  // <- add this
        class $commandClass {
            ...
        }
    """.trimIndent()
)

class NoHandlers(
    commandClass: String
) : CommandDefinitionError(
    commandClass, null, null,
    message = "No @DefaultHandler or @SubCommand methods found",
    suggestion = "Add at least one method annotated with @DefaultHandler or @SubCommand",
    codeHint = """
        @Command("mycommand")
        class $commandClass {

            @DefaultHandler  // <- add handler
            fun execute(@Sender sender: Player) {
                // ...
            }
        }
    """.trimIndent()
)

class DuplicateSubCommand(
    commandClass: String, name: String, methods: List<String>
) : CommandDefinitionError(
    commandClass, null, null,
    message = "Duplicate @SubCommand(\"$name\") in methods: [${methods.joinToString()}]",
    suggestion = "Use unique names, or use nested class with @DefaultHandler for overloads",
    codeHint = """
        // For overloads on subcommand - use nested class:
        @Command("$name")
        class ${name.replaceFirstChar { it.uppercase() }}Sub {
            @DefaultHandler fun variant1(...) { }
            @DefaultHandler fun variant2(...) { }
        }
    """.trimIndent()
)

class RangeOnNonNumeric(
    commandClass: String, method: String, parameter: String, actualType: String
) : CommandDefinitionError(
    commandClass, method, parameter,
    message = "@Range on '$parameter' but type is $actualType (not numeric)",
    suggestion = "@Range works only with Int, Long, Double, Float",
    codeHint = """
        @Arg("$parameter") @Range(min = 0.0, max = 100.0) $parameter: Int  // <- numeric type
    """.trimIndent()
)

class RangeInverted(
    commandClass: String, method: String, parameter: String, min: Double, max: Double
) : CommandDefinitionError(
    commandClass, method, parameter,
    message = "@Range min ($min) > max ($max) on '$parameter'",
    suggestion = "Swap min and max values",
    codeHint = """
        @Range(min = $max, max = $min)  // <- swap
    """.trimIndent()
)

class FlagDuplicateShort(
    commandClass: String, method: String, short: Char, params: List<String>
) : CommandDefinitionError(
    commandClass, method, null,
    message = "Duplicate @Flag short '-$short' on parameters: [${params.joinToString()}]",
    suggestion = "Each @Flag must have a unique short character",
    codeHint = null
)

class FlagDuplicateName(
    commandClass: String, method: String, name: String, params: List<String>
) : CommandDefinitionError(
    commandClass, method, null,
    message = "Duplicate @Flag name '--$name' on parameters: [${params.joinToString()}]",
    suggestion = "Each @Flag must have a unique name",
    codeHint = null
)

class PermissionOnHandler(
    commandClass: String, method: String
) : CommandDefinitionError(
    commandClass, method, null,
    message = "@Permission on @DefaultHandler '$method' - this only works on @SubCommand or class-level",
    suggestion = "Move @Permission to the @Command class, or use @SubCommand instead of @DefaultHandler",
    codeHint = """
        // Option 1: On the class (applies to everything)
        @Command("mycommand")
        @Permission("my.permission")
        class MyCommand { ... }

        // Option 2: On subcommand
        @SubCommand("sub")
        @Permission("my.permission")
        fun sub(...) { ... }
    """.trimIndent()
)

class PlayerOnlyOnConsoleOnly(
    commandClass: String, method: String
) : CommandDefinitionError(
    commandClass, method, null,
    message = "Both @PlayerOnly and @ConsoleOnly on '$method'",
    suggestion = "Use one or the other, not both",
    codeHint = null
)

class AsyncConfirmConflict(
    commandClass: String, method: String
) : CommandDefinitionError(
    commandClass, method, null,
    message = "@Async and @Confirm on '$method' - confirm flow requires sync main thread",
    suggestion = "Remove @Async or @Confirm. If you need async after confirm, do it manually inside the method",
    codeHint = null
)

class CooldownNonPositive(
    commandClass: String, method: String, seconds: Int
) : CommandDefinitionError(
    commandClass, method, null,
    message = "@Cooldown(seconds = $seconds) - must be positive",
    suggestion = "Use a positive number of seconds",
    codeHint = """
        @Cooldown(seconds = 5)  // <- > 0
    """.trimIndent()
)

class CompleterNotFound(
    commandClass: String, method: String, parameter: String, completerClass: String
) : CommandDefinitionError(
    commandClass, method, parameter,
    message = "@Complete references '$completerClass' but it couldn't be instantiated",
    suggestion = "Register it in KCommands.setup() or ensure it has a no-arg constructor / DI binding",
    codeHint = """
        KCommands.setup(plugin) {
            bind(${completerClass}::class, ${completerClass}(dependencies...))
        }
    """.trimIndent()
)
