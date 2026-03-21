# KCommands — Command Library for Bukkit/Kotlin

## Nazwa robocza: `kcommands`

---

## Cel

Zero boilerplate'u przy tworzeniu komend. Żadnego plugin.yml, żadnego onCommand,
żadnych ręcznych parsowań argumentów. Piszesz funkcję z adnotacjami → działa.

---

## Stack technologiczny

- **Język:** Kotlin 1.9+ (JVM 17+)
- **Refleksja:** `kotlin-reflect` (KFunction, KParameter, annotations)
- **Command dispatch:** Brigadier (Paper 1.20.6+ natywnie) z fallbackiem na Bukkit CommandMap
- **Completions:** async-safe, Brigadier SuggestionProvider
- **Build:** Gradle Kotlin DSL
- **Testy:** JUnit 5 + MockBukkit

---

## Jak to wygląda z perspektywy usera

### Prosty — jedna komenda, jeden handler
```kotlin
@Command("heal")
@Permission("duels.admin.heal")
@Description("Ulecz gracza")
class HealCommand {

    @DefaultHandler
    fun heal(
        @Sender sender: Player,
        @Arg("gracz") target: Player
    ) {
        target.health = target.maxHealth
        sender.sendMessage("Uleczono ${target.name}!")
    }
}
```

### Overloady — wiele @DefaultHandler z różną ilością argumentów
```kotlin
@Command("tp")
@Description("Teleportuj się do gracza lub gracza do gracza")
class TpCommand {

    @DefaultHandler
    fun teleportSelf(
        @Sender sender: Player,
        @Arg("gracz") target: Player
    ) {
        sender.teleport(target)
        sender.sendMessage("Przeteleportowano do ${target.name}!")
    }

    @DefaultHandler
    fun teleportOther(
        @Sender sender: Player,
        @Arg("gracz") target: Player,
        @Arg("gracz2") target2: Player
    ) {
        target.teleport(target2)
        sender.sendMessage("Przeteleportowano ${target.name} do ${target2.name}!")
    }
}
```

Generuje automatycznie:
```
/tp              → "Poprawne użycie:"
                    "  /tp <gracz>"
                    "  /tp <gracz> <gracz2>"
/tp Notch        → teleportuje sendera do Notch
/tp Notch Steve  → teleportuje Notch do Steve
/tp zlyNick      → "Gracz 'zlyNick' nie jest online!"
/tp [TAB]        → [Notch, Steve, jeb_]
```

### Subkomendy + default handler razem
```kotlin
@Command("kit")
@Description("Zarządzanie kitami")
class KitCommands(private val kitManager: KitManager) {

    // /kit → lista kitów
    @DefaultHandler
    fun list(@Sender sender: Player) {
        kitManager.all().forEach { sender.sendMessage("- ${it.name}") }
    }

    // /kit <nazwa> → info o kicie
    @DefaultHandler
    fun info(
        @Sender sender: Player,
        @Arg("kit") @Complete(KitCompleter::class) kit: String
    ) {
        sender.sendMessage("Info o kicie: $kit")
    }

    // /kit create <nazwa>
    @SubCommand("create")
    @Permission("duels.admin.kit")
    fun create(
        @Sender sender: Player,
        @Arg("nazwa") name: String
    ) {
        kitManager.create(name, sender.inventory)
        sender.sendMessage("Kit $name utworzony!")
    }

    // /kit delete <nazwa>
    @SubCommand("delete")
    @Permission("duels.admin.kit")
    @Confirm("Czy na pewno chcesz usunąć kit {nazwa}?")
    fun delete(
        @Sender sender: Player,
        @Arg("nazwa") @Complete(KitCompleter::class) name: String
    ) {
        kitManager.delete(name)
        sender.sendMessage("Kit $name usunięty!")
    }
}
```

Priorytet: subcommand name > default handler po arg count.
`/kit create` → subcommand "create", NIE handler z argiem "create" jako kit name.

### Nested subkomendy
```kotlin
@Command("admin")
@Permission("duels.admin")
class AdminCommands {

    @Command("rank")
    class RankCommands {

        @SubCommand("set")
        fun set(
            @Sender sender: Player,
            @Arg("gracz") target: OfflinePlayer,
            @Arg("ranga") @Complete(RankCompleter::class) rank: String
        ) { /* ... */ }

        @SubCommand("info")
        fun info(
            @Sender sender: Player,
            @Arg("gracz") target: OfflinePlayer
        ) { /* ... */ }
    }

    @Command("perm")
    class PermCommands {

        @SubCommand("add")
        fun add(
            @Sender sender: Player,
            @Arg("gracz") target: OfflinePlayer,
            @Arg("permisja") perm: String
        ) { /* ... */ }

        @SubCommand("remove")
        fun remove(
            @Sender sender: Player,
            @Arg("gracz") target: OfflinePlayer,
            @Arg("permisja") perm: String
        ) { /* ... */ }
    }
}
```

Generuje:
```
/admin rank set <gracz> <ranga>
/admin rank info <gracz>
/admin perm add <gracz> <permisja>
/admin perm remove <gracz> <permisja>
```

### Flagi + Greedy
```kotlin
@Command("ban")
@Permission("duels.admin.ban")
class BanCommand {

    @DefaultHandler
    fun ban(
        @Sender sender: Player,
        @Arg("gracz") target: OfflinePlayer,
        @Arg("powód") @Greedy reason: String = "Brak powodu",
        @Flag("silent", short = 's') silent: Boolean = false,
        @Flag("duration", short = 'd') duration: Duration = Duration.ofDays(30)
    ) {
        // /ban Notch cheating -s -d 7d
        // /ban Notch --silent --duration 7d Using hacks
    }
}
```

### Custom argument resolver
```kotlin
data class Kit(val name: String, val items: List<ItemStack>)

class KitResolver(private val kitManager: KitManager) : ArgumentResolver<Kit> {
    override fun resolve(input: String, context: CommandContext): Kit {
        return kitManager.getByName(input)
            ?: throw ArgumentException("Kit '$input' nie istnieje!")
    }

    override fun complete(input: String, context: CommandContext): List<String> {
        return kitManager.all()
            .map { it.name }
            .filter { it.startsWith(input, ignoreCase = true) }
    }
}

// rejestracja
KCommands.setup(this) {
    resolver(Kit::class, KitResolver(kitManager))
}

// użycie — automatyczny resolve + tab complete
@Command("kitinfo")
class KitInfoCommand {
    @DefaultHandler
    fun info(
        @Sender sender: Player,
        @Arg("kit") kit: Kit
    ) {
        sender.sendMessage("Kit: ${kit.name}, itemów: ${kit.items.size}")
    }
}
```

### DSL alternatywa
```kotlin
KCommands.register(plugin) {

    command("spawn") {
        permission("duels.spawn")
        playerOnly()

        execute { sender ->
            sender.teleport(spawnLocation)
        }
    }

    command("tp") {
        description("Teleportacja")

        default {
            arg<Player>("gracz")
            execute { sender, args ->
                sender.teleport(args.get<Player>("gracz"))
            }
        }

        default {
            arg<Player>("gracz")
            arg<Player>("gracz2")
            execute { sender, args ->
                args.get<Player>("gracz").teleport(args.get<Player>("gracz2"))
            }
        }
    }

    command("kit") {

        default {
            execute { sender ->
                kitManager.all().forEach { sender.sendMessage("- ${it.name}") }
            }
        }

        sub("create") {
            permission("duels.admin.kit")
            arg<String>("nazwa")
            execute { sender, args ->
                kitManager.create(args["nazwa"], sender.inventory)
            }
        }

        sub("delete") {
            permission("duels.admin.kit")
            arg<String>("nazwa") { completer { kitManager.all().map { it.name } } }
            confirm("Usunąć kit {nazwa}?", timeout = 15)
            cooldown(5)
            execute { sender, args ->
                kitManager.delete(args["nazwa"])
            }
        }
    }
}
```

Oba API (adnotacje + DSL) budują ten sam CommandNode tree. Można mieszać.

### Pełny plugin example
```kotlin
class DuelsPlugin : JavaPlugin() {

    private lateinit var kitManager: KitManager
    private lateinit var queueManager: QueueManager
    private lateinit var config: LobbyConfig

    override fun onEnable() {
        kitManager = KitManager(this)
        queueManager = QueueManager(this)
        config = YamlConfigManager.load<LobbyConfig>(dataFolder.resolve("config.yml"))

        KCommands.setup(this) {
            // custom resolvers
            resolver(Kit::class, KitResolver(kitManager))

            // DI bindings
            bind(KitManager::class, kitManager)
            bind(QueueManager::class, queueManager)
            bind(LobbyConfig::class, config)

            // custom middleware
            middleware(AuditMiddleware(auditLog))
        }

        KCommands.register(this,
            TpCommand(),
            HealCommand(),
            KitCommands(kitManager),
            AdminCommands(),
            BanCommand(),
            QueueCommands(queueManager),
            SpawnCommand(config)
        )
    }

    override fun onDisable() {
        KCommands.unregisterAll()
    }
}
```

---

## Architektura — pakiety

```
kcommands/
├── annotation/
│   ├── Command.kt              — @Command("name", aliases)
│   ├── SubCommand.kt           — @SubCommand("name", aliases)
│   ├── DefaultHandler.kt       — @DefaultHandler (root handler, wiele overloadów)
│   ├── Permission.kt           — @Permission("node", message?)
│   ├── Description.kt          — @Description("tekst")
│   ├── Arg.kt                  — @Arg("name")
│   ├── Sender.kt               — @Sender
│   ├── Complete.kt             — @Complete(CompleterClass::class)
│   ├── Flag.kt                 — @Flag("verbose", short = 'v')
│   ├── Greedy.kt               — @Greedy (reszta argumentów jako jeden string)
│   ├── Cooldown.kt             — @Cooldown(seconds, message?, bypassPermission?)
│   ├── Async.kt                — @Async (off main thread)
│   ├── Range.kt                — @Range(min, max)
│   ├── Confirm.kt              — @Confirm("message", timeoutSec)
│   ├── PlayerOnly.kt           — @PlayerOnly
│   ├── ConsoleOnly.kt          — @ConsoleOnly
│   └── Aliases.kt              — @Aliases("a", "b")
│
├── argument/
│   ├── ArgumentResolver.kt     — interfejs: resolve(String, Context) → T + complete()
│   ├── ArgumentException.kt    — rzucany przez resolver gdy input nieprawidłowy
│   ├── ArgumentResolverRegistry.kt
│   ├── BuiltinResolvers.kt     — rejestracja wszystkich builtinów
│   └── resolvers/
│       ├── StringResolver.kt
│       ├── IntResolver.kt
│       ├── LongResolver.kt
│       ├── DoubleResolver.kt
│       ├── FloatResolver.kt
│       ├── BooleanResolver.kt
│       ├── PlayerResolver.kt
│       ├── OfflinePlayerResolver.kt
│       ├── MaterialResolver.kt
│       ├── WorldResolver.kt
│       ├── GameModeResolver.kt
│       ├── EnumResolver.kt         — automatyczny dla dowolnego Enum<T>
│       ├── DurationResolver.kt     — "30s", "5m", "2h1m" → Duration
│       ├── UUIDResolver.kt
│       └── LocationResolver.kt     — "100 64 200" lub "~5 ~ ~-3" (relative)
│
├── completion/
│   ├── TabCompleter.kt          — interfejs
│   ├── CompletionRegistry.kt
│   ├── BuiltinCompleters.kt
│   ├── AsyncCompleter.kt        — completions z async źródeł
│   └── FilteredCompleter.kt     — permission-aware filtering
│
├── execution/
│   ├── CommandDispatcher.kt     — core: parsuj args → match handler → wywołaj
│   ├── HandlerMatcher.kt        — wybór handlera po arg count
│   ├── CommandContext.kt        — sender, args, flags, raw input
│   └── ErrorHandler.kt          — interfejs + DefaultErrorHandler
│
├── model/
│   ├── CommandNode.kt           — drzewo: command → handlers + children
│   ├── ExecutorInfo.kt          — KFunction + instance + parameters
│   ├── ArgInfo.kt               — name, type, required, resolver, completer, range
│   ├── ParameterInfo.kt         — KParameter wrapper z metadanymi
│   └── HandlerGroup.kt          — lista overloaded @DefaultHandler per node
│
├── scanner/
│   ├── CommandScanner.kt        — skanuje klasy z @Command → CommandNode tree
│   ├── AnnotationReader.kt      — wyciąga adnotacje z KFunction/KParameter
│   └── ValidationPass.kt        — waliduje drzewo, zbiera CommandDefinitionError
│
├── error/
│   ├── CommandDefinitionError.kt   — sealed class (19 typów błędów)
│   └── CommandErrorFormatter.kt    — formatuje ładny raport do loggera
│
├── registration/
│   ├── CommandRegistrar.kt      — interfejs
│   ├── BrigadierRegistrar.kt    — Paper 1.20.6+ natywny Brigadier
│   ├── BukkitRegistrar.kt       — fallback: CommandMap reflection
│   ├── CommandMapInjector.kt    — dynamiczna rejestracja bez plugin.yml
│   └── PlatformDetector.kt      — auto-detect Paper vs Spigot, wersja API
│
├── dsl/
│   ├── CommandDsl.kt            — entry point: KCommands.register(plugin) { ... }
│   ├── CommandBuilder.kt
│   ├── SubCommandBuilder.kt
│   ├── DefaultBuilder.kt
│   └── ArgBuilder.kt
│
├── help/
│   ├── HelpGenerator.kt         — auto /command help
│   ├── UsageFormatter.kt        — "Poprawne użycie: /tp <gracz>"
│   └── HelpPaginator.kt         — stronicowanie dla 10+ subkomend
│
├── message/
│   ├── CommandMessages.kt       — data class z domyślnymi komunikatami (KConfig-compatible)
│   └── MessageProvider.kt       — interfejs do podmiany (i18n)
│
├── middleware/
│   ├── CommandMiddleware.kt     — interfejs pre/post execution hooks
│   ├── PermissionMiddleware.kt
│   ├── CooldownMiddleware.kt
│   ├── ConfirmMiddleware.kt
│   ├── AsyncMiddleware.kt
│   ├── PlayerOnlyMiddleware.kt
│   └── LoggingMiddleware.kt
│
├── event/
│   ├── KCommandExecuteEvent.kt  — fired PRZED wykonaniem (cancellable)
│   ├── KCommandCompleteEvent.kt — fired PO wykonaniu
│   └── KCommandErrorEvent.kt    — fired gdy exception
│
├── di/
│   └── DependencyContainer.kt   — prosta mapa KClass → instance
│
└── KCommands.kt                 — główny entry point
```

---

## Fazy implementacji

### FAZA 1 — Core (MVP)
**Cel:** @Command, @SubCommand, @DefaultHandler, @Arg, automatyczny parsing,
tab complete, rejestracja bez plugin.yml. Wiele @DefaultHandler overloadów.

#### 1.1 — Adnotacje
```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Command(
    val name: String,
    vararg val aliases: String = []
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SubCommand(
    val name: String,
    vararg val aliases: String = []
)

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class DefaultHandler

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Arg(val name: String)

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Sender

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Permission(val node: String, val message: String = "")

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Description(val value: String)
```

#### 1.2 — ArgumentResolver + Registry
```kotlin
interface ArgumentResolver<T> {
    fun resolve(input: String, context: CommandContext): T
    fun complete(input: String, context: CommandContext): List<String> = emptyList()
}

class ArgumentException(message: String) : Exception(message)

class ArgumentResolverRegistry {
    private val resolvers = ConcurrentHashMap<KClass<*>, ArgumentResolver<*>>()

    fun <T : Any> register(klass: KClass<T>, resolver: ArgumentResolver<T>)
    fun <T : Any> get(klass: KClass<T>): ArgumentResolver<T>?
    fun getOrCreateEnum(klass: KClass<*>): ArgumentResolver<*>?
}
```

Builtin resolvers:
```kotlin
object StringResolver : ArgumentResolver<String> {
    override fun resolve(input: String, context: CommandContext) = input
}

object IntResolver : ArgumentResolver<Int> {
    override fun resolve(input: String, context: CommandContext): Int =
        input.toIntOrNull() ?: throw ArgumentException("'$input' nie jest liczbą!")
}

object BooleanResolver : ArgumentResolver<Boolean> {
    override fun resolve(input: String, context: CommandContext): Boolean =
        when (input.lowercase()) {
            "true", "yes", "tak", "on", "1" -> true
            "false", "no", "nie", "off", "0" -> false
            else -> throw ArgumentException("'$input' nie jest true/false!")
        }
    override fun complete(input: String, context: CommandContext) = listOf("true", "false")
}

object PlayerResolver : ArgumentResolver<Player> {
    override fun resolve(input: String, context: CommandContext): Player =
        Bukkit.getPlayer(input) ?: throw ArgumentException("Gracz '$input' nie jest online!")

    override fun complete(input: String, context: CommandContext): List<String> {
        val alreadyUsed = context.parsedArgs.values.filterIsInstance<Player>().map { it.name }
        return Bukkit.getOnlinePlayers()
            .map { it.name }
            .filter { it !in alreadyUsed }
            .filter { it.startsWith(input, ignoreCase = true) }
    }
}

object MaterialResolver : ArgumentResolver<Material> {
    override fun resolve(input: String, context: CommandContext): Material =
        Material.matchMaterial(input)
            ?: throw ArgumentException("Nieznany materiał: '$input'")

    override fun complete(input: String, context: CommandContext): List<String> =
        Material.values()
            .asSequence()
            .filter { !it.isLegacy }
            .map { it.name.lowercase() }
            .filter { it.startsWith(input.lowercase()) }
            .take(30)
            .toList()
}

object DurationResolver : ArgumentResolver<Duration> {
    private val PATTERN = Regex("(\\d+)([smhd])")

    override fun resolve(input: String, context: CommandContext): Duration {
        var total = Duration.ZERO
        PATTERN.findAll(input.lowercase()).forEach { match ->
            val amount = match.groupValues[1].toLong()
            total += when (match.groupValues[2]) {
                "s" -> Duration.ofSeconds(amount)
                "m" -> Duration.ofMinutes(amount)
                "h" -> Duration.ofHours(amount)
                "d" -> Duration.ofDays(amount)
                else -> throw ArgumentException("Nieznana jednostka: ${match.groupValues[2]}")
            }
        }
        if (total == Duration.ZERO) throw ArgumentException("Zły format czasu: '$input'. Użyj np. 30s, 5m, 2h")
        return total
    }

    override fun complete(input: String, context: CommandContext) =
        listOf("30s", "1m", "5m", "10m", "30m", "1h", "12h", "1d", "7d", "30d")
}

class EnumResolver<T : Enum<T>>(private val enumClass: Class<T>) : ArgumentResolver<T> {
    override fun resolve(input: String, context: CommandContext): T =
        enumClass.enumConstants.firstOrNull { it.name.equals(input, ignoreCase = true) }
            ?: throw ArgumentException(
                "'$input' — dostępne: ${enumClass.enumConstants.joinToString { it.name.lowercase() }}"
            )

    override fun complete(input: String, context: CommandContext): List<String> =
        enumClass.enumConstants
            .map { it.name.lowercase() }
            .filter { it.startsWith(input.lowercase()) }
}

object LocationResolver : ArgumentResolver<Location> {
    override fun resolve(input: String, context: CommandContext): Location { /* "100 64 200" lub "~5 ~ ~-3" */ }
    override fun complete(input: String, context: CommandContext) = listOf("~ ~ ~", "~1 ~ ~")
}
```

#### 1.3 — Model
```kotlin
data class CommandNode(
    val name: String,
    val aliases: List<String> = emptyList(),
    val permission: String? = null,
    val permissionMessage: String? = null,
    val description: String? = null,
    val handlers: HandlerGroup = HandlerGroup(),
    val children: MutableMap<String, CommandNode> = mutableMapOf()
)

class HandlerGroup {
    private val handlers = mutableListOf<ExecutorInfo>()

    fun add(executor: ExecutorInfo)
    fun all(): List<ExecutorInfo> = handlers.toList()

    fun match(argCount: Int): ExecutorInfo? {
        return handlers
            .sortedByDescending { it.args.size }
            .firstOrNull { handler ->
                val required = handler.args.count { it.required }
                val total = handler.args.size
                argCount in required..total
            }
    }

    fun usageVariants(): List<List<ArgInfo>> = handlers.map { it.args }
}

data class ExecutorInfo(
    val function: KFunction<*>,
    val instance: Any?,
    val parameters: List<ParameterInfo>,
    val args: List<ArgInfo>,
    val permission: String? = null,
    val description: String? = null,
    val isAsync: Boolean = false,
    val cooldown: CooldownInfo? = null,
    val confirm: ConfirmInfo? = null,
    val playerOnly: Boolean = false,
    val consoleOnly: Boolean = false
)

data class ArgInfo(
    val name: String,
    val type: KClass<*>,
    val required: Boolean,
    val isGreedy: Boolean = false,
    val resolver: ArgumentResolver<*>,
    val customCompleter: TabCompleter? = null,
    val range: ClosedRange<Double>? = null
)

data class CommandContext(
    val sender: CommandSender,
    val label: String,
    val rawArgs: Array<String>,
    val parsedArgs: MutableMap<String, Any?> = mutableMapOf(),
    val flags: MutableSet<String> = mutableSetOf()
) {
    val player: Player? get() = sender as? Player
    val isPlayer: Boolean get() = sender is Player

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(name: String): T = parsedArgs[name] as T
}
```

#### 1.4 — CommandScanner
```kotlin
class CommandScanner(private val resolverRegistry: ArgumentResolverRegistry) {

    fun scan(instance: Any): CommandNode {
        val klass = instance::class
        val cmdAnn = klass.findAnnotation<Command>()
            ?: throw IllegalArgumentException("${klass.simpleName} brakuje @Command")

        val node = CommandNode(
            name = cmdAnn.name,
            aliases = cmdAnn.aliases.toList(),
            permission = klass.findAnnotation<Permission>()?.node,
            description = klass.findAnnotation<Description>()?.value
        )

        for (func in klass.memberFunctions) {
            when {
                func.hasAnnotation<DefaultHandler>() -> {
                    node.handlers.add(buildExecutor(func, instance))
                }
                func.hasAnnotation<SubCommand>() -> {
                    val sub = func.findAnnotation<SubCommand>()!!
                    val childNode = node.children.getOrPut(sub.name) {
                        CommandNode(name = sub.name, aliases = sub.aliases.toList())
                    }
                    childNode.handlers.add(buildExecutor(func, instance))
                }
            }
        }

        for (nested in klass.nestedClasses) {
            if (nested.hasAnnotation<Command>()) {
                val nestedInstance = createInstance(nested, instance)
                val childNode = scan(nestedInstance)
                node.children[childNode.name] = childNode
            }
        }

        return node
    }

    private fun buildExecutor(func: KFunction<*>, instance: Any): ExecutorInfo {
        // wyciągnij @Arg, @Sender, @Permission, @Async, @Cooldown, @Confirm
        // zbuduj ArgInfo z resolverami
    }
}
```

#### 1.5 — CommandDispatcher
```kotlin
class CommandDispatcher(
    private val root: CommandNode,
    private val middlewares: List<CommandMiddleware>,
    private val errorHandler: ErrorHandler
) {
    fun dispatch(sender: CommandSender, rawArgs: Array<String>): Boolean {
        val context = CommandContext(sender, root.name, rawArgs)

        // 1. Nawiguj po drzewie — matchuj subcommands
        var currentNode = root
        var argOffset = 0
        while (argOffset < rawArgs.size) {
            val token = rawArgs[argOffset]
            val child = currentNode.children[token.lowercase()]
                ?: currentNode.children.values.firstOrNull { token.lowercase() in it.aliases }
            if (child != null) {
                currentNode = child
                argOffset++
            } else break
        }

        // 2. Permisja
        val perm = currentNode.permission ?: root.permission
        if (perm != null && !sender.hasPermission(perm)) {
            errorHandler.onNoPermission(context, perm)
            return true
        }

        // 3. Match handler po ilości argumentów
        val handlerArgs = rawArgs.drop(argOffset).toTypedArray()
        val handler = currentNode.handlers.match(handlerArgs.size)

        if (handler == null) {
            if (handlerArgs.isEmpty() && currentNode.children.isNotEmpty()) {
                errorHandler.onShowHelp(context, currentNode)
            } else {
                errorHandler.onMissingArgument(
                    context,
                    currentNode.handlers.all().firstOrNull()?.args?.firstOrNull()?.name ?: "",
                    UsageFormatter.formatAll(currentNode)
                )
            }
            return true
        }

        // 4. PlayerOnly / ConsoleOnly
        if (handler.playerOnly && sender !is Player) {
            errorHandler.onPlayerOnly(context)
            return true
        }

        // 5. Resolve argumenty
        for ((index, argInfo) in handler.args.withIndex()) {
            val raw = handlerArgs.getOrNull(index)

            if (raw == null) {
                if (!argInfo.required) break
                errorHandler.onMissingArgument(context, argInfo.name, UsageFormatter.formatAll(currentNode))
                return true
            }

            val input = if (argInfo.isGreedy) handlerArgs.drop(index).joinToString(" ") else raw

            try {
                val resolved = argInfo.resolver.resolve(input, context)
                if (argInfo.range != null && resolved is Number) {
                    if (resolved.toDouble() !in argInfo.range) {
                        errorHandler.onOutOfRange(context, argInfo.name, resolved, argInfo.range)
                        return true
                    }
                }
                context.parsedArgs[argInfo.name] = resolved
            } catch (e: ArgumentException) {
                errorHandler.onInvalidArgument(context, argInfo.name, input, e.message!!)
                return true
            }

            if (argInfo.isGreedy) break
        }

        // 6. Middleware pipeline
        for (middleware in middlewares) {
            if (!middleware.before(context, handler)) return true
        }

        // 7. Execute
        var exception: Exception? = null
        try {
            val functionArgs = buildFunctionArgs(handler, sender, context)
            handler.function.callBy(functionArgs)
        } catch (e: Exception) {
            exception = e
            errorHandler.onCommandException(context, e)
        }

        // 8. Post-middleware
        for (middleware in middlewares.reversed()) {
            middleware.after(context, handler, exception)
        }

        return true
    }

    fun complete(sender: CommandSender, rawArgs: Array<String>): List<String> {
        // nawiguj po drzewie
        // jeśli aktualny token matchuje częściowo subcommand → suggest subcommands
        // jeśli w handlerze → resolver.complete() / customCompleter
        // filtruj po permission
        // limit 30
    }
}
```

#### 1.6 — UsageFormatter
```kotlin
object UsageFormatter {

    fun formatAll(node: CommandNode): String {
        val variants = mutableListOf<String>()
        val basePath = buildPath(node)

        for (handler in node.handlers.all()) {
            val argsPart = handler.args.joinToString(" ") { arg ->
                if (arg.required) "<${arg.name}>" else "[${arg.name}]"
            }
            variants.add("/$basePath $argsPart".trim())
        }

        if (variants.isEmpty()) {
            for ((name, child) in node.children) {
                val desc = child.description?.let { " — $it" } ?: ""
                variants.add("/$basePath $name$desc")
            }
        }

        return when {
            variants.isEmpty() -> "Poprawne użycie: /$basePath"
            variants.size == 1 -> "Poprawne użycie: ${variants[0]}"
            else -> "Poprawne użycie:\n" + variants.joinToString("\n") { "  $it" }
        }
    }
}
```

#### 1.7 — Rejestracja bez plugin.yml
```kotlin
object CommandMapInjector {
    fun register(plugin: Plugin, node: CommandNode, dispatcher: CommandDispatcher) {
        val command = object : org.bukkit.command.Command(
            node.name, node.description ?: "", "/${node.name}", node.aliases
        ) {
            override fun execute(sender: CommandSender, label: String, args: Array<String>) =
                dispatcher.dispatch(sender, args)

            override fun tabComplete(sender: CommandSender, alias: String, args: Array<String>) =
                dispatcher.complete(sender, args)
        }

        node.permission?.let { command.permission = it }

        val commandMap = Bukkit.getServer().javaClass
            .getDeclaredField("commandMap")
            .apply { isAccessible = true }
            .get(Bukkit.getServer()) as CommandMap

        commandMap.register(plugin.name, command)
    }
}
```

#### 1.8 — KCommands entry point
```kotlin
object KCommands {
    private val resolverRegistry = ArgumentResolverRegistry()
    private val registeredNodes = mutableListOf<CommandNode>()
    private val middlewares = mutableListOf<CommandMiddleware>()
    private val di = DependencyContainer()
    private lateinit var plugin: Plugin
    private var errorHandler: ErrorHandler = DefaultErrorHandler()

    fun setup(plugin: Plugin, block: KCommandsConfig.() -> Unit = {}) {
        this.plugin = plugin
        val config = KCommandsConfig().apply(block)
        BuiltinResolvers.registerAll(resolverRegistry)
        config.resolvers.forEach { (klass, resolver) -> resolverRegistry.register(klass, resolver) }
        config.errorHandler?.let { errorHandler = it }
        config.middlewares.forEach { middlewares.add(it) }
        config.bindings.forEach { (klass, instance) -> di.bind(klass, instance) }
    }

    fun register(plugin: Plugin, vararg commandInstances: Any) {
        val allErrors = mutableListOf<CommandDefinitionError>()
        val scanner = CommandScanner(resolverRegistry)

        for (instance in commandInstances) {
            val node = try {
                scanner.scan(instance)
            } catch (e: Exception) {
                plugin.logger.severe("Failed to scan ${instance::class.simpleName}: ${e.message}")
                continue
            }

            val errors = ValidationPass.validate(instance::class, node, resolverRegistry)
            if (errors.isNotEmpty()) {
                allErrors.addAll(errors)
                continue
            }

            val dispatcher = CommandDispatcher(node, middlewares, errorHandler)
            CommandMapInjector.register(plugin, node, dispatcher)
            registeredNodes.add(node)
            plugin.logger.info("Registered /${node.name}")
        }

        if (allErrors.isNotEmpty()) {
            val report = CommandErrorFormatter.format(allErrors)
            for (line in report.lines()) {
                plugin.logger.severe(line)
            }
        }
    }

    fun register(plugin: Plugin, block: CommandDslBuilder.() -> Unit) { /* DSL */ }
    fun <T : Any> registerResolver(klass: KClass<T>, resolver: ArgumentResolver<T>) { resolverRegistry.register(klass, resolver) }
    fun unregisterAll() { /* cleanup CommandMap */ }
}
```

#### Testy fazy 1:
- Prosta komenda bez argumentów
- Komenda z 1 required arg (String, Int, Player)
- @DefaultHandler overload: 1 arg vs 2 args
- @DefaultHandler + @SubCommand priorytet
- Nested subcommands: /admin rank set
- Tab completion: subcommands → player names → enum values
- Context-aware completion: /tp Notch [TAB] nie sugeruje Notch ponownie
- Brak permisji → message
- Zły argument → "Gracz 'xxx' nie jest online!"
- Brakujący argument → usage (single vs multi-line)
- Aliasy na komendach i subkomendach
- Enum auto-resolver
- Custom ArgumentResolver

---

### FAZA 2 — Developer Exception System
**Cel:** przy rejestracji komend, jeśli developer popełni błąd w adnotacjach,
KCommands loguje czytelny raport z: CO jest źle, GDZIE, i JAK naprawić (z kodem).

Zbiera WSZYSTKIE błędy ze WSZYSTKICH komend naraz — nie crashuje na pierwszym.

#### 2.1 — CommandDefinitionError sealed class
```kotlin
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
    message = "Ambiguous @DefaultHandler — ${handlers.size} handlers with $requiredCount required args: [${handlers.joinToString()}]",
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
    suggestion = "Use unique names, or use nested class with @DefaultHandler for overloads",
    codeHint = """
        // Overloady na subcommand — użyj nested class:
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
        @Range(min = ${max}, max = ${min})  // ← zamień
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
    suggestion = "Register it in KCommands.setup() or ensure it has a no-arg constructor / DI binding",
    codeHint = """
        KCommands.setup(plugin) {
            bind(${completerClass}::class, ${completerClass}(dependencies...))
        }
    """.trimIndent()
)
```

#### 2.2 — ValidationPass
```kotlin
object ValidationPass {

    fun validate(
        klass: KClass<*>,
        node: CommandNode,
        resolverRegistry: ArgumentResolverRegistry
    ): List<CommandDefinitionError> {
        val errors = mutableListOf<CommandDefinitionError>()
        val className = klass.simpleName ?: "Unknown"

        // 1. @Command present?
        if (!klass.hasAnnotation<Command>()) {
            errors.add(MissingCommandAnnotation(className))
            return errors
        }

        // 2. Has handlers?
        if (node.handlers.all().isEmpty() && node.children.isEmpty()) {
            errors.add(NoHandlers(className))
            return errors
        }

        for (handler in node.handlers.all()) {
            val methodName = handler.function.name
            val params = handler.function.parameters

            // 3. @Sender present?
            val senderParams = params.filter { it.findAnnotation<Sender>() != null }
            if (senderParams.isEmpty()) errors.add(MissingSenderParameter(className, methodName))
            if (senderParams.size > 1) errors.add(MultipleSenderParameters(className, methodName, senderParams.mapNotNull { it.name }))

            // 4. @Sender type valid?
            for (sp in senderParams) {
                val type = sp.type.classifier as? KClass<*>
                if (type != null && type !in VALID_SENDER_TYPES)
                    errors.add(InvalidSenderType(className, methodName, sp.name ?: "?", type.simpleName ?: "?"))
            }

            // 5. All non-sender params annotated?
            for (param in params) {
                if (param.kind == KParameter.Kind.INSTANCE) continue
                if (param.findAnnotation<Sender>() != null) continue
                if (param.findAnnotation<Arg>() == null && param.findAnnotation<Flag>() == null)
                    errors.add(MissingArgAnnotation(className, methodName, param.name ?: "?"))
            }

            // 6. Required after optional?
            var seenOptional = false
            for (argInfo in handler.args) {
                if (argInfo.isGreedy) continue
                if (seenOptional && argInfo.required) {
                    val lastOpt = handler.args.lastOrNull { !it.required && handler.args.indexOf(it) < handler.args.indexOf(argInfo) }
                    errors.add(RequiredAfterOptional(className, methodName, argInfo.name, lastOpt?.name ?: "?"))
                }
                if (!argInfo.required) seenOptional = true
            }

            // 7. @Greedy last?
            val greedyIdx = handler.args.indexOfFirst { it.isGreedy }
            if (greedyIdx != -1 && greedyIdx != handler.args.lastIndex)
                errors.add(GreedyNotLast(className, methodName, handler.args[greedyIdx].name, handler.args[greedyIdx + 1].name))

            // 8. @Greedy is String?
            if (greedyIdx != -1 && handler.args[greedyIdx].type != String::class)
                errors.add(GreedyNotString(className, methodName, handler.args[greedyIdx].name, handler.args[greedyIdx].type.simpleName ?: "?"))

            // 9. Resolver exists?
            for (argInfo in handler.args) {
                if (resolverRegistry.get(argInfo.type) == null && resolverRegistry.getOrCreateEnum(argInfo.type) == null)
                    errors.add(NoResolver(className, methodName, argInfo.name, argInfo.type.simpleName ?: "?"))
            }

            // 10. @Range on numeric?
            for (argInfo in handler.args) {
                if (argInfo.range != null && argInfo.type !in NUMERIC_TYPES)
                    errors.add(RangeOnNonNumeric(className, methodName, argInfo.name, argInfo.type.simpleName ?: "?"))
            }

            // 11. @Range min <= max?
            for (argInfo in handler.args) {
                if (argInfo.range != null && argInfo.range.start > argInfo.range.endInclusive)
                    errors.add(RangeInverted(className, methodName, argInfo.name, argInfo.range.start, argInfo.range.endInclusive))
            }

            // 12. @PlayerOnly + @ConsoleOnly?
            if (handler.playerOnly && handler.consoleOnly)
                errors.add(PlayerOnlyOnConsoleOnly(className, methodName))

            // 13. @Async + @Confirm?
            if (handler.isAsync && handler.confirm != null)
                errors.add(AsyncConfirmConflict(className, methodName))

            // 14. @Cooldown positive?
            if (handler.cooldown != null && handler.cooldown.seconds <= 0)
                errors.add(CooldownNonPositive(className, methodName, handler.cooldown.seconds))

            // 15-16. Flag duplicates
            val flagParams = handler.parameters.filter { it.kParam.findAnnotation<Flag>() != null }
            flagParams.groupBy { it.kParam.findAnnotation<Flag>()!!.short }
                .filter { it.key != ' ' && it.value.size > 1 }
                .forEach { (short, dups) -> errors.add(FlagDuplicateShort(className, methodName, short, dups.mapNotNull { it.kParam.name })) }
            flagParams.groupBy { it.kParam.findAnnotation<Flag>()!!.name }
                .filter { it.value.size > 1 }
                .forEach { (name, dups) -> errors.add(FlagDuplicateName(className, methodName, name, dups.mapNotNull { it.kParam.name })) }

            // 17. @Complete resolvable?
            for (param in handler.parameters) {
                val completeAnn = param.kParam.findAnnotation<Complete>()
                if (completeAnn != null) {
                    try { /* instantiate */ }
                    catch (e: Exception) { errors.add(CompleterNotFound(className, methodName, param.kParam.name ?: "?", completeAnn.value.simpleName ?: "?")) }
                }
            }
        }

        // 18. Ambiguous overloads?
        node.handlers.all()
            .groupBy { h -> h.args.count { it.required } }
            .filter { it.value.size > 1 }
            .forEach { (count, handlers) -> errors.add(AmbiguousOverload(className, handlers.map { it.function.name }, count)) }

        // 19. Duplicate subcommand names?
        klass.memberFunctions
            .filter { it.hasAnnotation<SubCommand>() }
            .groupBy { it.findAnnotation<SubCommand>()!!.name }
            .filter { it.value.size > 1 }
            .forEach { (name, methods) -> errors.add(DuplicateSubCommand(className, name, methods.map { it.name })) }

        // Recurse children
        for ((_, child) in node.children) {
            // validate recursively
        }

        return errors
    }

    private val VALID_SENDER_TYPES = setOf(Player::class, CommandSender::class, ConsoleCommandSender::class, RemoteConsoleCommandSender::class)
    private val NUMERIC_TYPES = setOf(Int::class, Long::class, Double::class, Float::class)
}
```

#### 2.3 — CommandErrorFormatter
```kotlin
object CommandErrorFormatter {

    fun format(errors: List<CommandDefinitionError>): String {
        val sb = StringBuilder()

        sb.appendLine()
        sb.appendLine("┌─────────────────────────────────────────────────────────────┐")
        sb.appendLine("│          KCommands — Command Registration Failed            │")
        sb.appendLine("└─────────────────────────────────────────────────────────────┘")
        sb.appendLine()

        val grouped = errors.groupBy { it.commandClass }

        for ((className, classErrors) in grouped) {
            sb.appendLine("  ● $className")
            sb.appendLine()

            for ((index, error) in classErrors.withIndex()) {
                val location = buildString {
                    append(error.commandClass)
                    if (error.method != null) append("::${error.method}()")
                    if (error.parameter != null) append(" → param '${error.parameter}'")
                }

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
}
```

#### Przykładowy output:
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
       ✗ Ambiguous @DefaultHandler — 2 handlers with 1 required args: [teleportSelf, teleportByName]
       → Each @DefaultHandler must have a unique number of required (non-default) arguments

       Fix:
       │ // ŹLE — oba mają 1 required arg:
       │ @DefaultHandler fun a(@Sender s: Player, @Arg("x") x: Player) { }
       │ @DefaultHandler fun b(@Sender s: Player, @Arg("y") y: String) { }
       │
       │ // DOBRZE — różna ilość required:
       │ @DefaultHandler fun a(@Sender s: Player, @Arg("x") x: Player) { }          // 1 arg
       │ @DefaultHandler fun b(@Sender s: Player, @Arg("x") x: Player, @Arg("y") y: Player) { }  // 2 args

  ● AdminCommands

    4) AdminCommands
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
  4 error(s) — fix them and restart the server.
```

#### Pełna lista checkowanych błędów (19):

| #  | Typ                          | Opis                                            |
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
| 16 | FlagDuplicateShort           | Duplikat flag short char                        |
| 17 | FlagDuplicateName            | Duplikat flag name                              |
| 18 | AmbiguousOverload            | Dwa @DefaultHandler z tą samą required count    |
| 19 | DuplicateSubCommand          | Dwa @SubCommand z tą samą nazwą                 |
| 20 | CompleterNotFound            | @Complete class nie da się stworzyć             |

#### Testy fazy 2:
- Każdy z 20 typów błędów → poprawny error message + codeHint
- Wiele błędów w jednej klasie → wszystkie w raporcie
- Wiele klas z błędami → grupowanie po klasie
- Zero błędów → brak outputu
- Błąd w nested class → poprawna ścieżka
- CodeHint zawiera poprawny Kotlin syntax

---

### FAZA 3 — Error handling + Messages
**Cel:** czytelne runtime błędy (player-facing), konfigurowane komunikaty.

#### 3.1 — ErrorHandler
```kotlin
interface ErrorHandler {
    fun onNoPermission(context: CommandContext, permission: String)
    fun onPlayerOnly(context: CommandContext)
    fun onConsoleOnly(context: CommandContext)
    fun onInvalidArgument(context: CommandContext, argName: String, input: String, expected: String)
    fun onMissingArgument(context: CommandContext, argName: String, usage: String)
    fun onOutOfRange(context: CommandContext, argName: String, value: Number, range: ClosedRange<Double>)
    fun onCooldown(context: CommandContext, remaining: Duration)
    fun onCommandException(context: CommandContext, exception: Exception)
    fun onShowHelp(context: CommandContext, node: CommandNode)
    fun onUnknownSubcommand(context: CommandContext, input: String, available: List<String>)
}
```

#### 3.2 — CommandMessages (KConfig data class)
```kotlin
data class CommandMessages(
    val noPermission: String = "<red>Brak uprawnień! <dark_gray>({PERMISSION})",
    val playerOnly: String = "<red>Ta komenda jest tylko dla graczy!",
    val consoleOnly: String = "<red>Ta komenda jest tylko dla konsoli!",
    val invalidArg: String = "<red>Nieprawidłowy argument <gold>{ARG}</gold>: <white>{INPUT}<red>. {EXPECTED}",
    val missingArg: String = "<red>Brak wymaganego argumentu: <gold>{ARG}",
    val usage: String = "<gray>{USAGE}",
    val outOfRange: String = "<red>Wartość <gold>{ARG}</gold> musi być między <white>{MIN}</white> a <white>{MAX}</white>!",
    val cooldown: String = "<red>Poczekaj <gold>{TIME}</gold> przed ponownym użyciem!",
    val unknownSubcmd: String = "<red>Nieznana subkomenda: <gold>{INPUT}<red>. Dostępne: <yellow>{AVAILABLE}",
    val confirm: String = "<yellow>Wpisz <gold>/confirm</gold> w ciągu <white>{TIME}s</white> aby potwierdzić.",
    val confirmExpired: String = "<red>Czas na potwierdzenie minął.",
    val commandError: String = "<red>Wystąpił błąd przy wykonywaniu komendy!",
    val helpHeader: String = "<gold><bold>★ {COMMAND}</bold></gold>",
    val helpEntry: String = "<gray>  {USAGE} <dark_gray>— {DESCRIPTION}",
    val helpFooter: String = "<gray>Strona <white>{PAGE}<gray>/<white>{PAGES}"
)
```

#### 3.3 — MessageProvider (i18n-ready)
```kotlin
interface MessageProvider {
    fun get(key: String, sender: CommandSender): String
}

class DefaultMessageProvider(val messages: CommandMessages = CommandMessages()) : MessageProvider
class LocaleMessageProvider(private val locales: Map<String, CommandMessages>) : MessageProvider
```

---

### FAZA 4 — Flagi, @Greedy, @Complete, @Range
**Cel:** zaawansowane argumenty.

#### 4.1 — @Flag
```kotlin
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Flag(val name: String, val short: Char = ' ', val description: String = "")
```

Parsing: scan raw args → wyciągnij flagi → reszta = pozycyjne.
Flagi mogą być w dowolnej pozycji: `/ban --silent Notch cheating` = `/ban Notch cheating --silent`

#### 4.2 — @Greedy
```kotlin
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Greedy
```

MUSI być ostatni positional arg, MUSI być String. ValidationPass to sprawdza.

#### 4.3 — @Complete
```kotlin
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Complete(val value: KClass<out TabCompleter>)

interface TabCompleter {
    fun complete(input: String, context: CommandContext): List<String>
}
```

Priorytet: @Complete > resolver.complete()

#### 4.4 — @Range
```kotlin
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Range(val min: Double, val max: Double)
```

---

### FAZA 5 — Middleware pipeline
**Cel:** modularna pre/post execution logika.

```kotlin
interface CommandMiddleware {
    fun before(context: CommandContext, handler: ExecutorInfo): Boolean = true
    fun after(context: CommandContext, handler: ExecutorInfo, exception: Exception? = null) {}
}
```

Wbudowane: PermissionMiddleware, CooldownMiddleware, ConfirmMiddleware, AsyncMiddleware, LoggingMiddleware.

Custom:
```kotlin
KCommands.setup(this) {
    middleware(AuditMiddleware(auditLog))
    middleware(RateLimitMiddleware(maxPerMinute = 30))
}
```

Pipeline: middleware A.before → B.before → execute → B.after → A.after

#### CooldownManager
```kotlin
class CooldownManager {
    private val cooldowns = ConcurrentHashMap<String, Instant>()
    fun getRemaining(player: Player, command: String): Duration?
    fun set(player: Player, command: String, duration: Duration)
    fun clear(player: Player, command: String)
    fun startCleanupTask(plugin: Plugin)  // co minutę usuwaj wygasłe
}
```

#### ConfirmationManager
```kotlin
class ConfirmationManager {
    data class PendingAction(val action: () -> Unit, val expiresAt: Instant)
    private val pending = ConcurrentHashMap<UUID, PendingAction>()
    fun request(player: Player, id: String, message: String, timeout: Duration, action: () -> Unit)
    fun confirm(player: Player): Boolean
    fun cancel(player: Player)
}
```

@Confirm z placeholder support:
```kotlin
@Confirm("Czy na pewno chcesz zbanować {gracz} na {duration}?")
fun ban(@Sender sender: Player, @Arg("gracz") target: OfflinePlayer, @Arg("duration") duration: Duration)
// /ban Notch 7d → "Czy na pewno chcesz zbanować Notch na 7d?"
```

---

### FAZA 6 — Help system
**Cel:** automatyczny /command help, permission-aware, clickable.

```
/kit help

★ Kit Commands
  [/kit <kit>]           — Info o kicie           ← klik → suggest
  [/kit create <nazwa>]  — Utwórz nowy kit        ← klik → suggest
  [/kit delete <nazwa>]  — Usuń kit
  [/kit list]            — Lista kitów             ← klik → run

Strona 1/1
```

Gracz bez `duels.admin.kit` widzi tylko komendy do których ma dostęp.
10+ entries → paginacja: `/kit help 2`

---

### FAZA 7 — Bukkit Events

```kotlin
class KCommandExecuteEvent(
    val sender: CommandSender, val command: String,
    val args: Array<String>, val node: CommandNode
) : Event(), Cancellable

class KCommandCompleteEvent(
    val sender: CommandSender, val command: String,
    val success: Boolean, val exception: Exception?
) : Event()
```

---

### FAZA 8 — DSL builder
Alternatywa do adnotacji. Oba budują ten sam CommandNode tree. Patrz sekcja "DSL alternatywa" wyżej.

---

### FAZA 9 — Brigadier integration (Paper 1.20.6+)
Auto-detect: Paper 1.20.6+ → Brigadier (kolorowane argumenty w kliencie), inaczej → CommandMap.

```kotlin
object PlatformDetector {
    fun hasBrigadier(): Boolean = try {
        Class.forName("io.papermc.paper.command.brigadier.Commands")
        true
    } catch (e: ClassNotFoundException) { false }
}
```

---

### FAZA 10 — Dependency Injection
Prosta mapa KClass → instance. Nie Spring.

```kotlin
KCommands.setup(this) {
    bind(KitManager::class, kitManager)
    bind(QueueManager::class, queueManager)
}

// auto-injected w constructor
@Command("kit")
class KitCommands(private val kitManager: KitManager) { }
```

```kotlin
class DependencyContainer {
    private val bindings = mutableMapOf<KClass<*>, Any>()
    fun <T : Any> bind(klass: KClass<T>, instance: T) { bindings[klass] = instance }
    fun <T : Any> get(klass: KClass<T>): T? = bindings[klass] as? T
    fun <T : Any> createInstance(klass: KClass<T>): T {
        val constructor = klass.primaryConstructor!!
        val args = constructor.parameters.associateWith { param ->
            val paramKlass = param.type.classifier as KClass<*>
            get(paramKlass) ?: throw IllegalArgumentException("No binding for ${paramKlass.simpleName}")
        }
        return constructor.callBy(args)
    }
}
```

---

## Kolejność implementacji

```
Tydzień 1:   FAZA 1.1–1.4  — Adnotacje, resolvers, scanner, model
Tydzień 2:   FAZA 1.5–1.6  — Dispatcher, handler matching, usage formatter
Tydzień 3:   FAZA 1.7–1.8  — CommandMap injection, KCommands entry
Tydzień 4:   FAZA 2        — Developer exception system (ValidationPass + formatter)
Tydzień 5:   FAZA 3        — Runtime error handling, messages, MessageProvider
Tydzień 6:   FAZA 4        — Flagi, @Greedy, @Complete, @Range
Tydzień 7:   FAZA 5        — Middleware pipeline
Tydzień 8:   FAZA 6        — Help system
Tydzień 9:   FAZA 7        — Bukkit events
Tydzień 10:  FAZA 8        — DSL builder
Tydzień 11:  FAZA 9        — Brigadier integration
Tydzień 12:  FAZA 10       — Dependency injection
```

---

## Edge case'y

1. **Handler overload ambiguity:** dwa @DefaultHandler z tą samą required count → error przy scan
2. **Subcommand vs arg collision:** "create" to subcommand czy arg? → subcommand ZAWSZE wygrywa
3. **Trailing space:** "/kit " → completuj subcommands
4. **Empty greedy:** "/msg Notch " → @Greedy = "" → użyj defaulta lub error
5. **Aliasy na każdym poziomie:** /k c test = /kit create test
6. **Case sensitivity:** komendy case-insensitive, args zależne od resolver
7. **Sender types:** Player, Console, RemoteConsole, CommandBlock, ProxiedSender
8. **Async + player quit:** sprawdź isOnline przed execution
9. **Confirm + player quit:** wyczyść pending
10. **Cooldown reset:** po restart serwera (in-memory domyślnie)
11. **Namespace collision:** /duels:kit jeśli inny plugin ma /kit
12. **Plugin reload:** wyrejestruj stare, zarejestruj nowe
13. **Numeric subcommand:** /ft 3 → match children first
14. **Tab completion limit:** max 30-50
15. **Brigadier sync:** rejestracja PRZED player join
16. **Greedy + flags:** flagi wyciągnięte, reszta = greedy string
17. **Flag at any position:** --silent może być gdziekolwiek
18. **Optional arg + flag confusion:** "/cmd --flag" vs "/cmd value --flag"
19. **Nested permission inheritance:** @Permission na class → dziedziczy na subcommands
20. **Scan-time vs runtime errors:** ValidationPass (dev errors) vs ErrorHandler (player errors)

---

## Performance

- **CommandNode tree:** build raz przy register
- **Resolver registry:** ConcurrentHashMap, O(1) lookup
- **Reflection cache:** metadata w ExecutorInfo, odczytane raz przy scan
- **Tab completion:** filter server-side, limit 30, async jeśli heavy
- **CooldownManager:** cleanup co minutę
- **Middleware list:** stała tablica, nie twórz co dispatch
- **EnumResolver:** cache constants
- **MaterialResolver:** cache filtered list (bez legacy)
- **String interning:** command/subcommand names

---

## Struktura Gradle

```
kcommands/
├── build.gradle.kts
├── settings.gradle.kts
├── kcommands-core/
│   ├── build.gradle.kts      — kotlin, kotlin-reflect, paper-api (compileOnly)
│   └── src/
└── kcommands-test/
    ├── build.gradle.kts      — MockBukkit, JUnit 5
    └── src/
```

Jeden moduł (komendy są inherentnie Bukkit-specific).
