# KCommands — Developer Exception System

## Cel

Gdy developer popełni błąd w definicji komendy, KCommands NIE crashuje z cryptic stacktrace.
Zamiast tego loguje czytelny, sformatowany raport z:
- CO jest źle
- GDZIE (klasa, metoda, parametr)
- JAK naprawić (sugestia + poprawny kod)

Wszystkie checky wykonują się PRZY REJESTRACJI (scan-time), nie w runtime.

---

## Typy błędów

```kotlin
sealed class CommandDefinitionError(
    val commandClass: String,
    val method: String?,
    val parameter: String?,
    val message: String,
    val suggestion: String,
    val codeHint: String? = null  // poprawny kod jako przykład
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
            @Arg("$parameter") $parameter: ...  // ← dodaj @Arg
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
            @Sender sender: Player,  // ← dodaj to
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
            @Sender sender: Player,  // ← Player, CommandSender, lub ConsoleCommandSender
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
            @Arg("required1") ...,     // ← required najpierw
            @Arg("required2") ...,
            @Arg("optional1") ... = defaultValue  // ← optional na końcu
        )
    """.trimIndent()
)

class GreedyNotLast(
    commandClass: String, method: String, greedyParam: String, afterParam: String
) : CommandDefinitionError(
    commandClass, method, greedyParam,
    message = "@Greedy arg '$greedyParam' is not the last argument (followed by '$afterParam')",
    suggestion = "@Greedy must be the last positional argument — it consumes all remaining input",
    codeHint = """
        fun $method(
            @Sender sender: Player,
            @Arg("other") other: Player,
            @Arg("$greedyParam") @Greedy $greedyParam: String  // ← zawsze ostatni
        )
    """.trimIndent()
)

class GreedyNotString(
    commandClass: String, method: String, greedyParam: String, actualType: String
) : CommandDefinitionError(
    commandClass, method, greedyParam,
    message = "@Greedy arg '$greedyParam' is $actualType, but must be String",
    suggestion = "@Greedy joins remaining args into one String — only String type makes sense",
    codeHint = """
        @Arg("$greedyParam") @Greedy $greedyParam: String  // ← String, nie $actualType
    """.trimIndent()
)

class NoResolver(
    commandClass: String, method: String, parameter: String, type: String
) : CommandDefinitionError(
    commandClass, method, parameter,
    message = "No ArgumentResolver registered for type '$type'",
    suggestion = "Register a resolver in KCommands.setup() or use a built-in type",
    codeHint = """
        // Opcja 1: Zarejestruj resolver
        KCommands.setup(plugin) {
            resolver(${type}::class, ${type}Resolver())
        }
        
        // Opcja 2: Użyj built-in typu
        // Dostępne: String, Int, Long, Double, Float, Boolean,
        //           Player, OfflinePlayer, Material, World,
        //           GameMode, Duration, UUID, dowolny Enum
    """.trimIndent()
)

class AmbiguousOverload(
    commandClass: String, handlers: List<String>, requiredCount: Int
) : CommandDefinitionError(
    commandClass, null, null,
    message = "Ambiguous @DefaultHandler overload — ${handlers.size} handlers with $requiredCount required args: [${handlers.joinToString()}]",
    suggestion = "Each @DefaultHandler must have a unique number of required (non-default) arguments",
    codeHint = """
        // ŹLE — oba mają 1 required arg:
        @DefaultHandler fun a(@Sender s: Player, @Arg("x") x: Player) { }
        @DefaultHandler fun b(@Sender s: Player, @Arg("y") y: String) { }
        
        // DOBRZE — różna ilość required:
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
        @Command("mycommand")  // ← dodaj to
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
        
            @DefaultHandler  // ← dodaj handler
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
    suggestion = "Use unique subcommand names, or use @DefaultHandler for overloads on the same subcommand",
    codeHint = """
        // Jeśli chcesz overloady na subcommand — użyj osobnego nested class:
        @Command("$name")
        class ${name.capitalize()}Sub {
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
        @Arg("$parameter") @Range(min = 0.0, max = 100.0) $parameter: Int  // ← numeric type
    """.trimIndent()
)

class RangeInverted(
    commandClass: String, method: String, parameter: String, min: Double, max: Double
) : CommandDefinitionError(
    commandClass, method, parameter,
    message = "@Range min ($min) > max ($max) on '$parameter'",
    suggestion = "Swap min and max values",
    codeHint = """
        @Range(min = $max, max = $min)  // ← zamień
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
    message = "@Permission on @DefaultHandler '$method' — this only works on @SubCommand or class-level",
    suggestion = "Move @Permission to the @Command class, or use @SubCommand instead of @DefaultHandler",
    codeHint = """
        // Opcja 1: Na klasie (dotyczy wszystkiego)
        @Command("mycommand")
        @Permission("my.permission")
        class MyCommand { ... }
        
        // Opcja 2: Na subcommand
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
    message = "@Async and @Confirm on '$method' — confirm flow requires sync main thread",
    suggestion = "Remove @Async or @Confirm. If you need async after confirm, do it manually inside the method",
    codeHint = null
)

class CooldownNonPositive(
    commandClass: String, method: String, seconds: Int
) : CommandDefinitionError(
    commandClass, method, null,
    message = "@Cooldown(seconds = $seconds) — must be positive",
    suggestion = "Use a positive number of seconds",
    codeHint = """
        @Cooldown(seconds = 5)  // ← > 0
    """.trimIndent()
)

class CompleterNotFound(
    commandClass: String, method: String, parameter: String, completerClass: String
) : CommandDefinitionError(
    commandClass, method, parameter,
    message = "@Complete references '$completerClass' but it couldn't be instantiated",
    suggestion = "Register it in KCommands.setup() or make sure it has a no-arg constructor / is bound in DI",
    codeHint = """
        KCommands.setup(plugin) {
            bind(${completerClass}::class, ${completerClass}(dependencies...))
        }
    """.trimIndent()
)
```

---

## Formatter — ładny output

```kotlin
object CommandErrorFormatter {

    fun format(errors: List<CommandDefinitionError>): String {
        val sb = StringBuilder()

        sb.appendLine()
        sb.appendLine("┌─────────────────────────────────────────────────────────────┐")
        sb.appendLine("│          KCommands — Command Registration Failed            │")
        sb.appendLine("└─────────────────────────────────────────────────────────────┘")
        sb.appendLine()

        // grupuj po klasie
        val grouped = errors.groupBy { it.commandClass }

        for ((className, classErrors) in grouped) {
            sb.appendLine("  ● $className")
            sb.appendLine()

            for ((index, error) in classErrors.withIndex()) {
                val location = buildLocation(error)
                sb.appendLine("    ${index + 1}) $location")
                sb.appendLine("       ✗ ${error.message}")
                sb.appendLine("       → ${error.suggestion}")

                if (error.codeHint != null) {
                    sb.appendLine()
                    sb.appendLine("       Fix:")
                    error.codeHint.lines().forEach { line ->
                        sb.appendLine("       │ $line")
                    }
                }
                sb.appendLine()
            }
        }

        sb.appendLine("  ══════════════════════════════════════════")
        sb.appendLine("  ${errors.size} error(s) — fix them and restart the server.")
        sb.appendLine()

        return sb.toString()
    }

    private fun buildLocation(error: CommandDefinitionError): String {
        return buildString {
            append(error.commandClass)
            if (error.method != null) append("::${error.method}()")
            if (error.parameter != null) append(" → param '${error.parameter}'")
        }
    }
}
```

---

## Przykładowy output w konsoli

### Brak @Arg:
```
┌─────────────────────────────────────────────────────────────┐
│          KCommands — Command Registration Failed            │
└─────────────────────────────────────────────────────────────┘

  ● TpCommand

    1) TpCommand::teleport() → param 'target'
       ✗ Parameter 'target' has no @Arg or @Sender annotation
       → Add @Arg("target") or @Sender to this parameter

       Fix:
       │ fun teleport(
       │     @Sender sender: Player,
       │     @Arg("target") target: ...  // ← dodaj @Arg
       │ )

  ══════════════════════════════════════════
  1 error(s) — fix them and restart the server.
```

### Brak @Sender:
```
  ● HealCommand

    1) HealCommand::heal()
       ✗ No @Sender parameter found
       → Add a parameter annotated with @Sender (Player, CommandSender, or ConsoleCommandSender)

       Fix:
       │ fun heal(
       │     @Sender sender: Player,  // ← dodaj to
       │     ...
       │ )
```

### Wiele błędów naraz:
```
┌─────────────────────────────────────────────────────────────┐
│          KCommands — Command Registration Failed            │
└─────────────────────────────────────────────────────────────┘

  ● BanCommand

    1) BanCommand::ban() → param 'reason'
       ✗ @Greedy arg 'reason' is not the last argument (followed by 'silent')
       → @Greedy must be the last positional argument — it consumes all remaining input

       Fix:
       │ fun ban(
       │     @Sender sender: Player,
       │     @Arg("other") other: Player,
       │     @Arg("reason") @Greedy reason: String  // ← zawsze ostatni
       │ )

    2) BanCommand::ban() → param 'duration'
       ✗ No ArgumentResolver registered for type 'BanDuration'
       → Register a resolver in KCommands.setup() or use a built-in type

       Fix:
       │ // Opcja 1: Zarejestruj resolver
       │ KCommands.setup(plugin) {
       │     resolver(BanDuration::class, BanDurationResolver())
       │ }
       │
       │ // Opcja 2: Użyj built-in typu
       │ // Dostępne: String, Int, Long, Double, Float, Boolean,
       │ //           Player, OfflinePlayer, Material, World,
       │ //           GameMode, Duration, UUID, dowolny Enum

  ● TpCommand

    3) TpCommand
       ✗ Ambiguous @DefaultHandler overload — 2 handlers with 1 required args: [teleportSelf, teleportByName]
       → Each @DefaultHandler must have a unique number of required (non-default) arguments

       Fix:
       │ // ŹLE — oba mają 1 required arg:
       │ @DefaultHandler fun a(@Sender s: Player, @Arg("x") x: Player) { }
       │ @DefaultHandler fun b(@Sender s: Player, @Arg("y") y: String) { }
       │
       │ // DOBRZE — różna ilość required:
       │ @DefaultHandler fun a(@Sender s: Player, @Arg("x") x: Player) { }          // 1 arg
       │ @DefaultHandler fun b(@Sender s: Player, @Arg("x") x: Player, @Arg("y") y: Player) { }  // 2 args

    4) TpCommand::teleportSelf() → param 'target'
       ✗ @Range on 'target' but type is Player (not numeric)
       → @Range works only with Int, Long, Double, Float

       Fix:
       │ @Arg("target") @Range(min = 0.0, max = 100.0) target: Int  // ← numeric type

  ● AdminCommands

    5) AdminCommands
       ✗ No @DefaultHandler or @SubCommand methods found
       → Add at least one method annotated with @DefaultHandler or @SubCommand

       Fix:
       │ @Command("mycommand")
       │ class AdminCommands {
       │
       │     @DefaultHandler  // ← dodaj handler
       │     fun execute(@Sender sender: Player) {
       │         // ...
       │     }
       │ }

  ══════════════════════════════════════════
  5 error(s) — fix them and restart the server.
```

### @Range min > max:
```
  ● SetEloCommand

    1) SetEloCommand::setElo() → param 'elo'
       ✗ @Range min (5000.0) > max (0.0) on 'elo'
       → Swap min and max values

       Fix:
       │ @Range(min = 0.0, max = 5000.0)  // ← zamień
```

### @Async + @Confirm conflict:
```
  ● DangerCommand

    1) DangerCommand::nuke()
       ✗ @Async and @Confirm on 'nuke' — confirm flow requires sync main thread
       → Remove @Async or @Confirm. If you need async after confirm, do it manually inside the method
```

### Required po optional:
```
  ● GiveCommand

    1) GiveCommand::give() → param 'amount'
       ✗ Required arg 'amount' comes after optional arg 'material'
       → Move all required args before optional ones

       Fix:
       │ fun give(
       │     @Sender sender: Player,
       │     @Arg("required1") ...,     // ← required najpierw
       │     @Arg("required2") ...,
       │     @Arg("optional1") ... = defaultValue  // ← optional na końcu
       │ )
```

---

## Implementacja — ValidationPass

```kotlin
object ValidationPass {

    fun validate(
        klass: KClass<*>,
        node: CommandNode,
        resolverRegistry: ArgumentResolverRegistry
    ): List<CommandDefinitionError> {
        val errors = mutableListOf<CommandDefinitionError>()
        val className = klass.simpleName ?: "Unknown"

        // === Class-level checks ===

        // 1. @Command present?
        if (!klass.hasAnnotation<Command>()) {
            errors.add(MissingCommandAnnotation(className))
            return errors  // dalej nie ma sensu
        }

        // 2. Ma jakieś handlery?
        val hasHandlers = node.handlers.all().isNotEmpty()
        val hasChildren = node.children.isNotEmpty()
        if (!hasHandlers && !hasChildren) {
            errors.add(NoHandlers(className))
            return errors
        }

        // === Handler-level checks ===

        for (handler in node.handlers.all()) {
            val methodName = handler.function.name
            val params = handler.function.parameters

            // 3. @Sender present?
            val senderParams = params.filter { it.findAnnotation<Sender>() != null }
            if (senderParams.isEmpty()) {
                errors.add(MissingSenderParameter(className, methodName))
            }
            if (senderParams.size > 1) {
                errors.add(MultipleSenderParameters(className, methodName, senderParams.map { it.name ?: "?" }))
            }

            // 4. @Sender type valid?
            for (sp in senderParams) {
                val type = sp.type.classifier as? KClass<*>
                if (type != null && type !in VALID_SENDER_TYPES) {
                    errors.add(InvalidSenderType(className, methodName, sp.name ?: "?", type.simpleName ?: "?"))
                }
            }

            // 5. Wszystkie non-sender params mają @Arg lub @Flag?
            for (param in params) {
                if (param.kind == KParameter.Kind.INSTANCE) continue  // 'this'
                if (param.findAnnotation<Sender>() != null) continue
                if (param.findAnnotation<Arg>() == null && param.findAnnotation<Flag>() == null) {
                    errors.add(MissingArgAnnotation(className, methodName, param.name ?: "?"))
                }
            }

            // 6. Required po optional?
            var seenOptional = false
            for (argInfo in handler.args) {
                if (argInfo.isGreedy) continue
                if (seenOptional && argInfo.required) {
                    val lastOptional = handler.args.lastOrNull { !it.required && handler.args.indexOf(it) < handler.args.indexOf(argInfo) }
                    errors.add(RequiredAfterOptional(className, methodName, argInfo.name, lastOptional?.name ?: "?"))
                }
                if (!argInfo.required) seenOptional = true
            }

            // 7. @Greedy last?
            val greedyIndex = handler.args.indexOfFirst { it.isGreedy }
            if (greedyIndex != -1 && greedyIndex != handler.args.lastIndex) {
                val after = handler.args[greedyIndex + 1]
                errors.add(GreedyNotLast(className, methodName, handler.args[greedyIndex].name, after.name))
            }

            // 8. @Greedy is String?
            if (greedyIndex != -1) {
                val greedyArg = handler.args[greedyIndex]
                if (greedyArg.type != String::class) {
                    errors.add(GreedyNotString(className, methodName, greedyArg.name, greedyArg.type.simpleName ?: "?"))
                }
            }

            // 9. Resolver exists?
            for (argInfo in handler.args) {
                if (resolverRegistry.get(argInfo.type) == null && resolverRegistry.getOrCreateEnum(argInfo.type) == null) {
                    errors.add(NoResolver(className, methodName, argInfo.name, argInfo.type.simpleName ?: "?"))
                }
            }

            // 10. @Range on numeric?
            for (argInfo in handler.args) {
                if (argInfo.range != null && argInfo.type !in NUMERIC_TYPES) {
                    errors.add(RangeOnNonNumeric(className, methodName, argInfo.name, argInfo.type.simpleName ?: "?"))
                }
            }

            // 11. @Range min <= max?
            for (argInfo in handler.args) {
                if (argInfo.range != null && argInfo.range.start > argInfo.range.endInclusive) {
                    errors.add(RangeInverted(className, methodName, argInfo.name, argInfo.range.start, argInfo.range.endInclusive))
                }
            }

            // 12. @PlayerOnly + @ConsoleOnly?
            if (handler.playerOnly && handler.consoleOnly) {
                errors.add(PlayerOnlyOnConsoleOnly(className, methodName))
            }

            // 13. @Async + @Confirm?
            if (handler.isAsync && handler.confirm != null) {
                errors.add(AsyncConfirmConflict(className, methodName))
            }

            // 14. @Cooldown positive?
            if (handler.cooldown != null && handler.cooldown.seconds <= 0) {
                errors.add(CooldownNonPositive(className, methodName, handler.cooldown.seconds))
            }

            // 15. Flag duplicates?
            val flagsByShort = handler.parameters
                .filter { it.kParam.findAnnotation<Flag>() != null }
                .groupBy { it.kParam.findAnnotation<Flag>()!!.short }
                .filter { it.key != ' ' && it.value.size > 1 }
            for ((short, params2) in flagsByShort) {
                errors.add(FlagDuplicateShort(className, methodName, short, params2.map { it.kParam.name ?: "?" }))
            }

            val flagsByName = handler.parameters
                .filter { it.kParam.findAnnotation<Flag>() != null }
                .groupBy { it.kParam.findAnnotation<Flag>()!!.name }
                .filter { it.value.size > 1 }
            for ((name, params2) in flagsByName) {
                errors.add(FlagDuplicateName(className, methodName, name, params2.map { it.kParam.name ?: "?" }))
            }

            // 16. @Complete class resolvable?
            for (param in handler.parameters) {
                val completeAnn = param.kParam.findAnnotation<Complete>()
                if (completeAnn != null) {
                    try {
                        // próbuj stworzyć instancję (DI lub no-arg constructor)
                    } catch (e: Exception) {
                        errors.add(CompleterNotFound(className, methodName, param.kParam.name ?: "?", completeAnn.value.simpleName ?: "?"))
                    }
                }
            }
        }

        // === Overload ambiguity check ===

        // 17. Dwa @DefaultHandler z tą samą ilością required args?
        val handlersByRequiredCount = node.handlers.all()
            .groupBy { h -> h.args.count { it.required } }
            .filter { it.value.size > 1 }

        for ((count, handlers) in handlersByRequiredCount) {
            errors.add(AmbiguousOverload(className, handlers.map { it.function.name }, count))
        }

        // === SubCommand duplicate check ===

        // 18. Dwa @SubCommand z tą samą nazwą (nie overload)?
        // (overloady subcommand → nested class z @DefaultHandler)
        val subsByName = klass.memberFunctions
            .filter { it.hasAnnotation<SubCommand>() }
            .groupBy { it.findAnnotation<SubCommand>()!!.name }
            .filter { it.value.size > 1 }

        for ((name, methods) in subsByName) {
            errors.add(DuplicateSubCommand(className, name, methods.map { it.name }))
        }

        // === Recurse into children ===
        for ((_, child) in node.children) {
            // find nested class for this child
            // validate recursively
        }

        return errors
    }

    private val VALID_SENDER_TYPES = setOf(
        Player::class,
        CommandSender::class,
        ConsoleCommandSender::class,
        RemoteConsoleCommandSender::class
    )

    private val NUMERIC_TYPES = setOf(
        Int::class, Long::class, Double::class, Float::class
    )
}
```

---

## Integracja z KCommands.register()

```kotlin
object KCommands {

    fun register(plugin: Plugin, vararg commandInstances: Any) {
        val allErrors = mutableListOf<CommandDefinitionError>()

        for (instance in commandInstances) {
            val klass = instance::class
            val scanner = CommandScanner(resolverRegistry)

            // skanuj — buduj node tree
            val node = try {
                scanner.scan(instance)
            } catch (e: Exception) {
                // nawet scan się nie udał → fatal
                plugin.logger.severe("Failed to scan ${klass.simpleName}: ${e.message}")
                continue
            }

            // waliduj
            val errors = ValidationPass.validate(klass, node, resolverRegistry)

            if (errors.isNotEmpty()) {
                allErrors.addAll(errors)
                continue  // nie rejestruj, ale sprawdzaj dalej
            }

            // wszystko ok → rejestruj
            val dispatcher = CommandDispatcher(node, middlewares, errorHandler)
            CommandMapInjector.register(plugin, node, dispatcher)
            registeredNodes.add(node)

            plugin.logger.info("Registered /${node.name} (${countHandlers(node)} handlers)")
        }

        // loguj WSZYSTKIE błędy naraz (nie po jednym)
        if (allErrors.isNotEmpty()) {
            val report = CommandErrorFormatter.format(allErrors)
            for (line in report.lines()) {
                plugin.logger.severe(line)
            }
        }
    }
}
```

---

## Pełna lista checkowanych błędów (18)

| #  | Check                        | Kiedy                                           |
|----|------------------------------|-------------------------------------------------|
| 1  | MissingCommandAnnotation     | Klasa bez @Command                              |
| 2  | NoHandlers                   | Brak @DefaultHandler i @SubCommand              |
| 3  | MissingSenderParameter       | Metoda bez @Sender                              |
| 4  | MultipleSenderParameters     | >1 parametr z @Sender                           |
| 5  | InvalidSenderType            | @Sender na nie-sender typie                     |
| 6  | MissingArgAnnotation         | Parametr bez @Arg, @Sender, @Flag               |
| 7  | RequiredAfterOptional        | Required arg po optional                        |
| 8  | GreedyNotLast                | @Greedy nie na ostatnim arg                     |
| 9  | GreedyNotString              | @Greedy na typie != String                      |
| 10 | NoResolver                   | Brak ArgumentResolver dla typu                  |
| 11 | RangeOnNonNumeric            | @Range na nie-numerycznym typie                 |
| 12 | RangeInverted                | @Range min > max                                |
| 13 | PlayerOnlyOnConsoleOnly      | @PlayerOnly + @ConsoleOnly razem                |
| 14 | AsyncConfirmConflict         | @Async + @Confirm razem                         |
| 15 | CooldownNonPositive          | @Cooldown(seconds <= 0)                         |
| 16 | FlagDuplicateShort/Name      | Duplikat flag short char lub name               |
| 17 | AmbiguousOverload            | Dwa @DefaultHandler z tą samą required count    |
| 18 | DuplicateSubCommand          | Dwa @SubCommand z tą samą nazwą                 |
| 19 | CompleterNotFound            | @Complete class nie da się stworzyć             |
