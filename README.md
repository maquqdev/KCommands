<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:7F52FF,50:6C3483,100:2980B9&height=220&section=header&text=KCommands&fontSize=75&fontColor=ffffff&fontAlignY=35&desc=Annotation-driven%20command%20framework%20for%20Bukkit%20%2F%20Paper&descSize=18&descAlignY=55&animation=fadeIn" width="100%"/>

<br/>

[![CI](https://github.com/maquqdev/KCommands/actions/workflows/ci.yml/badge.svg)](https://github.com/maquqdev/KCommands/actions)
[![JitPack](https://jitpack.io/v/maquq/KCommands.svg)](https://jitpack.io/#maquq/KCommands)
[![License](https://img.shields.io/github/license/maquq/KCommands?style=flat&color=6C3483)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-%237F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Paper](https://img.shields.io/badge/Paper-1.21.4-%232980B9)](https://papermc.io)
[![JDK](https://img.shields.io/badge/JDK-21+-%23ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org)

**Define commands as plain Kotlin classes. No boilerplate. No manual parsing. No `plugin.yml` entries.**

[📖 Wiki](https://github.com/maquqdev/KCommands/wiki) · [🐛 Issues](https://github.com/maquqdev/KCommands/issues) · [📦 JitPack](https://jitpack.io/#maquq/KCommands)

</div>

---

## ⚡ Why KCommands?

<table>
<tr>
<td width="50%">

### 🚫 Raw Bukkit
```kotlin
class HealCommand : CommandExecutor, TabCompleter {
    override fun onCommand(
        sender: CommandSender, cmd: Command,
        label: String, args: Array<String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Players only!")
            return true
        }
        if (!sender.hasPermission("myplugin.heal")) {
            sender.sendMessage("No permission!")
            return true
        }
        sender.health = 20.0
        sender.sendMessage("Healed!")
        return true
    }

    override fun onTabComplete(
        sender: CommandSender, cmd: Command,
        label: String, args: Array<String>
    ): List<String> = emptyList()
}
```

</td>
<td width="50%">

### ✅ KCommands
```kotlin
@Command("heal")
@Permission("myplugin.heal")
@PlayerOnly
class HealCommand {
    @DefaultHandler
    fun heal(@Sender sender: Player) {
        sender.health = 20.0
        sender.sendMessage("Healed!")
    }
}
```

<br/>

> 🎯 Parsing, tab completion, permissions, sender checks, error handling — **all automatic.**

</td>
</tr>
</table>

---

## 🧩 Features

<table>
<tr>
<td>

### 🏗️ Core
![Command](https://img.shields.io/badge/@Command-declarative_structure-7F52FF?style=flat-square)
![Resolvers](https://img.shields.io/badge/14-built_in_resolvers-2980B9?style=flat-square)
![Overloading](https://img.shields.io/badge/method-overloading-6C3483?style=flat-square)
![Brigadier](https://img.shields.io/badge/brigadier-auto_detection-1ABC9C?style=flat-square)

</td>
<td>

### 🔒 Safety
![Validation](https://img.shields.io/badge/dev-exception_system-e74c3c?style=flat-square)
![PlayerOnly](https://img.shields.io/badge/@PlayerOnly-sender_restriction-e67e22?style=flat-square)
![Cooldown](https://img.shields.io/badge/@Cooldown-rate_limiting-f1c40f?style=flat-square)
![Confirm](https://img.shields.io/badge/@Confirm-two_step_execution-e74c3c?style=flat-square)

</td>
</tr>
<tr>
<td>

### 📦 Arguments
![Range](https://img.shields.io/badge/@Range-numeric_validation-27ae60?style=flat-square)
![Greedy](https://img.shields.io/badge/@Greedy-consume_all_input-2ecc71?style=flat-square)
![Flags](https://img.shields.io/badge/@Flag-boolean_&_valued-16a085?style=flat-square)
![Custom](https://img.shields.io/badge/custom-resolvers-1abc9c?style=flat-square)

</td>
<td>

### 🔌 Extensibility
![i18n](https://img.shields.io/badge/IMessageProvider-full_i18n-9b59b6?style=flat-square)
![Middleware](https://img.shields.io/badge/CommandMiddleware-pipeline_hooks-8e44ad?style=flat-square)
![DSL](https://img.shields.io/badge/DSL-programmatic_API-6C3483?style=flat-square)
![Events](https://img.shields.io/badge/Bukkit-events-7F52FF?style=flat-square)

</td>
</tr>
</table>

---

## 🚀 Quick Start

### 1️⃣ Add the dependency

```kotlin
// build.gradle.kts
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.maquq:KCommands:VERSION")
}
```

<details>
<summary>📋 Maven</summary>

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.maquq</groupId>
    <artifactId>KCommands</artifactId>
    <version>VERSION</version>
</dependency>
```

</details>

> Replace `VERSION` with a release tag or commit hash from [JitPack](https://jitpack.io/#maquq/KCommands).

### 2️⃣ Create a command

```kotlin
@Command("heal")
@Permission("myplugin.heal")
class HealCommand {

    @DefaultHandler
    fun heal(@Sender sender: Player) {
        sender.health = sender.maxHealth
        sender.sendMessage("Healed!")
    }

    @DefaultHandler
    fun healOther(@Sender sender: Player, @Arg("target") target: Player) {
        target.health = target.maxHealth
        sender.sendMessage("Healed ${target.name}!")
    }
}
```

### 3️⃣ Register in your plugin

```kotlin
class MyPlugin : JavaPlugin() {
    override fun onEnable() {
        KCommands.setup(this)
        KCommands.register(this, HealCommand())
    }

    override fun onDisable() {
        KCommands.unregisterAll()
    }
}
```

> No `plugin.yml` entries needed. KCommands injects commands directly into the server.

---

## 📚 Examples

<details>
<summary><b>🌳 Subcommands & Nested Commands</b></summary>

```kotlin
@Command("admin")
@Permission("myplugin.admin")
class AdminCommand {

    @DefaultHandler
    fun help(@Sender sender: CommandSender) {
        sender.sendMessage("Admin commands: user, server")
    }

    @Command("user")
    @Permission("myplugin.admin.user")
    class UserCommands {

        @SubCommand("ban")
        @Permission("myplugin.admin.user.ban")
        fun ban(@Sender sender: CommandSender, @Arg("player") target: String) {
            sender.sendMessage("Banned $target")
        }

        @SubCommand("kick")
        fun kick(@Sender sender: CommandSender, @Arg("player") target: Player) {
            target.kickPlayer("Kicked by admin")
        }
    }
}
```

```
/admin              → help()
/admin user ban     → ban()
/admin user kick    → kick()
```

</details>

<details>
<summary><b>🔀 Overloaded Handlers</b></summary>

Multiple `@DefaultHandler` methods matched by argument count automatically.

```kotlin
@Command("tp")
class TpCommand {

    @DefaultHandler
    fun self(@Sender sender: Player, @Arg("target") target: Player) {
        sender.teleport(target)
    }

    @DefaultHandler
    fun other(
        @Sender sender: Player,
        @Arg("who") who: Player,
        @Arg("target") target: Player
    ) {
        who.teleport(target)
    }
}
// /tp Steve       → teleports you to Steve
// /tp Steve Alex  → teleports Steve to Alex
```

</details>

<details>
<summary><b>🚩 Flags, Greedy & Range</b></summary>

```kotlin
@Command("ban")
class BanCommand {

    @DefaultHandler
    @Cooldown(seconds = 5)
    fun ban(
        @Sender sender: Player,
        @Arg("target") target: String,
        @Arg("reason") @Greedy reason: String,
        @Flag("silent", short = 's') silent: Boolean = false,
        @Flag("duration", short = 'd') duration: Int = 0
    ) {
        if (!silent) broadcast("$target was banned: $reason")
    }
}
// /ban Steve cheating with hacks -s -d 30
```

```kotlin
@Command("give")
class GiveCommand {

    @DefaultHandler
    fun give(
        @Sender sender: Player,
        @Arg("item") material: Material,       // Tab: all materials
        @Arg("amount") @Range(min = 1.0, max = 64.0) amount: Int = 1
    ) {
        sender.inventory.addItem(ItemStack(material, amount))
    }
}
```

</details>

<details>
<summary><b>🧱 DSL API</b></summary>

Programmatic alternative to annotations — useful for dynamic or config-driven commands.

```kotlin
KCommands.register(plugin) {
    command("economy") {
        aliases("eco", "money")
        permission("myplugin.economy")

        default {
            playerOnly = true
            execute { sender, _ ->
                sender.sendMessage("Your balance: $1000")
            }
        }

        sub("pay") {
            default {
                cooldownSeconds = 5
                arg<Player>("target")
                arg<Double>("amount") { range(0.01, 1000000.0) }
                execute { sender, args ->
                    val target = args["target"] as Player
                    val amount = args["amount"] as Double
                    sender.sendMessage("Sent $$amount to ${target.name}")
                }
            }
        }
    }
}
```

</details>

<details>
<summary><b>🔧 Custom Resolvers</b></summary>

```kotlin
class WarpResolver(
    private val warpStorage: WarpStorage
) : ArgumentResolver<Warp> {

    override fun resolve(input: String, context: CommandContext): Warp {
        return warpStorage.getWarp(input)
            ?: throw ArgumentException("Warp '$input' does not exist")
    }

    override fun complete(input: String, context: CommandContext): List<String> {
        return warpStorage.getAllWarps()
            .filter { it.permission == null || context.sender.hasPermission(it.permission) }
            .map { it.name }
            .filter { it.lowercase().startsWith(input.lowercase()) }
    }
}

// Register
KCommands.setup(plugin) {
    resolver(Warp::class, WarpResolver(warpStorage))
}
```

</details>

<details>
<summary><b>⚙️ Middleware</b></summary>

```kotlin
class RateLimitMiddleware(private val maxPerMinute: Int = 20) : CommandMiddleware {

    private val counts = ConcurrentHashMap<String, AtomicInteger>()

    override fun before(context: CommandContext, handler: ExecutorInfo): Boolean {
        val player = context.sender as? Player ?: return true
        val count = counts.getOrPut(player.uniqueId.toString()) { AtomicInteger(0) }

        if (count.incrementAndGet() > maxPerMinute) {
            player.sendMessage("You are sending commands too fast!")
            return false
        }
        return true
    }
}

KCommands.setup(plugin) {
    middleware(RateLimitMiddleware(maxPerMinute = 30))
}
```

Pipeline order: `PlayerOnly → Permission → Cooldown → [Logging] → [Custom] → Confirm → Handler`

</details>

<details>
<summary><b>🌍 Messages & i18n</b></summary>

```kotlin
KCommands.setup(plugin) {
    messages {
        CommandMessages(
            noPermission = "<red>Access denied! <dark_gray>({PERMISSION})",
            cooldown = "<red>Wait <gold>{TIME}</gold> before trying again.",
            missingArg = "<red>Missing argument: <gold>{ARG}",
            unknownSubcmd = "<red>Unknown: <gold>{INPUT}<red>. Try: <yellow>{AVAILABLE}"
        )
    }
}
```

For full i18n, implement `IMessageProvider` backed by config files or a database. All messages use [MiniMessage](https://docs.advntr.dev/minimessage/format.html) format.

</details>

<details>
<summary><b>💉 Dependency Injection</b></summary>

```kotlin
KCommands.setup(plugin) {
    bind(EconomyService::class, economyService)
    bind(WarpStorage::class, warpStorage)
    bind(KitManager::class, kitManager)
}

@Command("balance")
class BalanceCommand(private val economy: EconomyService) {

    @DefaultHandler
    fun balance(@Sender sender: Player) {
        sender.sendMessage("Balance: $${economy.getBalance(sender)}")
    }
}
```

</details>

<details>
<summary><b>📡 Events</b></summary>

KCommands fires Bukkit events at key points during command execution.

```kotlin
class CommandListener : Listener {

    @EventHandler
    fun onExecute(event: KCommandExecuteEvent) {
        // Before handler — cancellable
        logger.info("${event.sender.name} running /${event.command}")
    }

    @EventHandler
    fun onComplete(event: KCommandCompleteEvent) {
        // After handler — success or failure
        if (!event.success) logger.warning("Failed: ${event.exception?.message}")
    }

    @EventHandler
    fun onError(event: KCommandErrorEvent) {
        // Unhandled exception — cancellable
        errorTracker.report(event.exception)
    }
}
```

Execution order: `ExecuteEvent → Middleware (before) → Handler → Middleware (after) → CompleteEvent`

</details>

---

## 🔍 Built-in Resolvers

| Type | Example | Tab Completion |
|:-----|:--------|:---------------|
| `String` | `hello` | — |
| `Int` `Long` `Double` `Float` | `42`, `3.14` | — |
| `Boolean` | `true` / `yes` / `on` | ✅ |
| `Player` | `Steve` | ✅ Online players (context-aware) |
| `OfflinePlayer` | `Steve` | ✅ Online players |
| `Material` | `DIAMOND_SWORD` | ✅ All materials |
| `World` | `world_nether` | ✅ Loaded worlds |
| `GameMode` | `creative` | ✅ All modes |
| `Duration` | `30s`, `2h30m`, `1d` | ✅ Presets |
| `UUID` | `550e8400-e29b-...` | — |
| `Location` | `100 64 200` / `~ ~5 ~` | ✅ `~ ~ ~` |
| Any `Enum` | constant name | ✅ All constants |

---

## 🛡️ Developer Exception System

KCommands validates every command at registration time. Invalid definitions produce formatted console reports — not silent failures.

```
┌─────────────────────────────────────────────────────────────┐
│          KCommands - Command Registration Failed            │
└─────────────────────────────────────────────────────────────┘

  * MyBrokenCommand

    1) MyBrokenCommand::execute() -> param 'target'
       ✗ Parameter 'target' has no @Arg or @Sender annotation
       → Add @Arg("target") or @Sender to this parameter

       Fix:
       │ fun execute(
       │     @Sender sender: Player,
       │     @Arg("target") target: ...  // ← add @Arg
       │ )

  ==========================================
  1 error(s) — fix them and restart the server.
```

<details>
<summary>📋 All 21 detected error types</summary>

| # | Error | What's wrong |
|:--|:------|:-------------|
| 1 | `MissingArgAnnotation` | Parameter lacks `@Arg` or `@Sender` |
| 2 | `MissingSenderParameter` | No `@Sender` parameter in handler |
| 3 | `MultipleSenderParameters` | More than one `@Sender` |
| 4 | `InvalidSenderType` | Sender type not `Player` / `CommandSender` / `ConsoleCommandSender` |
| 5 | `RequiredAfterOptional` | Required arg after an optional one |
| 6 | `GreedyNotLast` | `@Greedy` not on last argument |
| 7 | `GreedyNotString` | `@Greedy` on non-String type |
| 8 | `NoResolver` | No resolver registered for arg type |
| 9 | `AmbiguousOverload` | Two handlers with same required arg count |
| 10 | `MissingCommandAnnotation` | Class missing `@Command` |
| 11 | `NoHandlers` | No `@DefaultHandler` or `@SubCommand` methods |
| 12 | `DuplicateSubCommand` | Same `@SubCommand` name used twice |
| 13 | `RangeOnNonNumeric` | `@Range` on non-numeric parameter |
| 14 | `RangeInverted` | `min > max` in `@Range` |
| 15 | `FlagDuplicateShort` | Two flags share the same short char |
| 16 | `FlagDuplicateName` | Two flags share the same name |
| 17 | `PermissionOnHandler` | `@Permission` on `@DefaultHandler` |
| 18 | `PlayerOnlyOnConsoleOnly` | Both `@PlayerOnly` and `@ConsoleOnly` |
| 19 | `AsyncConfirmConflict` | `@Async` + `@Confirm` on same handler |
| 20 | `CooldownNonPositive` | `@Cooldown(seconds = 0)` or negative |
| 21 | `CompleterNotFound` | `@Complete` class can't be instantiated |

</details>

---

## ⚙️ Configuration

```kotlin
KCommands.setup(plugin) {
    brigadier(true)                                  // Paper Brigadier (auto-fallback to Bukkit)
    logging(true)                                    // Log all command executions
    messages { CommandMessages(/* ... */) }           // Custom messages
    messageProvider(ConfigMessageProvider(config))    // Or full i18n provider
    errorHandler(MyCustomErrorHandler())             // Custom error handling
    middleware(AuditMiddleware(auditLogger))          // Custom middleware
    resolver(Warp::class, WarpResolver(warpStorage)) // Custom resolvers
    bind(EconomyService::class, economyService)      // DI bindings
}
```

---

## 📖 Documentation

Full documentation is available on the **[Wiki](https://github.com/maquqdev/KCommands/wiki)**:

| Section | Topics |
|:--------|:-------|
| **[Getting Started](https://github.com/maquqdev/KCommands/wiki/Getting-Started)** | Installation, first command, registration |
| **[Commands & Handlers](https://github.com/maquqdev/KCommands/wiki/Commands-and-Handlers)** | `@Command`, `@SubCommand`, nested commands, overloading |
| **[Arguments](https://github.com/maquqdev/KCommands/wiki/Arguments)** | `@Arg`, `@Sender`, `@Greedy`, `@Range`, `@Flag` |
| **[Tab Completion](https://github.com/maquqdev/KCommands/wiki/Tab-Completion)** | Auto-completion, `@Complete`, async completers |
| **[Permissions](https://github.com/maquqdev/KCommands/wiki/Permissions-and-Restrictions)** | `@Permission`, `@PlayerOnly`, `@ConsoleOnly` |
| **[Resolvers](https://github.com/maquqdev/KCommands/wiki/Built-in-Resolvers)** | 14 built-in + custom `ArgumentResolver<T>` |
| **[Middleware](https://github.com/maquqdev/KCommands/wiki/Middleware)** | Pipeline, cooldowns, confirmations, custom hooks |
| **[Messages & i18n](https://github.com/maquqdev/KCommands/wiki/Messages-and-i18n)** | `CommandMessages`, `IMessageProvider`, MiniMessage |
| **[DSL API](https://github.com/maquqdev/KCommands/wiki/DSL-API)** | Programmatic command definitions |
| **[Events](https://github.com/maquqdev/KCommands/wiki/Events)** | `KCommandExecuteEvent`, `KCommandCompleteEvent`, `KCommandErrorEvent` |
| **[Dependency Injection](https://github.com/maquqdev/KCommands/wiki/Dependency-Injection)** | `DependencyContainer`, constructor injection |
| **[Exception System](https://github.com/maquqdev/KCommands/wiki/Developer-Exception-System)** | All 21 validation errors with fix suggestions |
| **[Configuration](https://github.com/maquqdev/KCommands/wiki/Configuration)** | Full `KCommandsConfig` reference |

---

## 🤝 Contributing

Contributions are welcome. Please open an issue to discuss larger changes before submitting a PR.

1. Fork the repository
2. Create a feature branch
3. Submit a pull request

## 📄 License

[MIT](LICENSE)

---

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:2980B9,50:6C3483,100:7F52FF&height=120&section=footer" width="100%"/>

</div>
