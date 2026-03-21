# KCommands

**Annotation-driven command framework for Bukkit/Paper.** Define commands as plain Kotlin classes and let the framework handle everything else.

![CI](https://github.com/maquqdev/KCommands/actions/workflows/ci.yml/badge.svg) [![JitPack](https://jitpack.io/v/maquq/KCommands.svg)](https://jitpack.io/#maquq/KCommands) ![License](https://img.shields.io/github/license/maquq/KCommands) ![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?logo=kotlin&logoColor=white) ![Paper](https://img.shields.io/badge/Paper-1.21.4-blue)

## Why KCommands?

- **Zero boilerplate** - No manual argument parsing, no `CommandExecutor` wiring, no hand-rolled tab completion
- **Type-safe by default** - Arguments are resolved and validated before your handler is called
- **Catches mistakes early** - Invalid command definitions produce formatted error reports at startup, not silent failures at runtime
- **Drop-in ready** - Works with Paper Brigadier out of the box; falls back to Bukkit CommandMap automatically

<details>
<summary><b>Before & After</b></summary>

**Raw Bukkit:**

```kotlin
class HealCommand : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, cmd: Command,
                           label: String, args: Array<String>): Boolean {
        if (sender !is Player) { sender.sendMessage("Players only!"); return true }
        if (!sender.hasPermission("myplugin.heal")) { sender.sendMessage("No permission!"); return true }
        sender.health = 20.0
        sender.sendMessage("Healed!")
        return true
    }

    override fun onTabComplete(sender: CommandSender, cmd: Command,
                               label: String, args: Array<String>): List<String> = emptyList()
}
```

**KCommands:**

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

</details>

## Features

### Core

- `@Command` / `@SubCommand` / `@DefaultHandler` - declarative command structure
- **Auto-parsing** - 14 built-in argument resolvers with tab completion
- **Method overloading** - multiple handlers matched by argument count
- **Aliases** - `@Aliases("h", "hp")` for alternative command names

### Arguments

- **14 built-in resolvers** - Player, Material, World, Duration, UUID, Location, enums, primitives
- **Custom resolvers** - implement `ArgumentResolver<T>` for any type
- `@Range(min, max)` - numeric validation at parse time
- `@Greedy` - consume all remaining input into a single String
- `@Flag("name", short = 'n')` - boolean and valued flags

### Safety

- **Developer exception system** - formatted error reports for invalid definitions
- `@PlayerOnly` / `@ConsoleOnly` - sender type restrictions
- `@Cooldown(seconds = 5)` - per-player rate limiting with bypass permissions
- `@Confirm` - require explicit confirmation before execution

### Extensibility

- `IMessageProvider` / `CommandMessages` - full i18n support
- `CommandMiddleware` - hook into the execution pipeline
- **DSL API** - programmatic alternative to annotations
- **Brigadier auto-detection** - native Paper integration with Bukkit fallback

## Quick Start

**1. Add the dependency**

```kotlin
repositories { maven("https://jitpack.io") }
dependencies { implementation("com.github.maquq:KCommands:VERSION") }
```

**2. Create a command**

```kotlin
@Command("heal")
@Permission("myplugin.heal")
class HealCommand {
    @DefaultHandler
    fun heal(@Sender sender: Player) {
        sender.health = 20.0
        sender.sendMessage("Healed!")
    }
}
```

**3. Register in your plugin**

```kotlin
class MyPlugin : JavaPlugin() {
    override fun onEnable() {
        KCommands.setup(this)
        KCommands.register(this, HealCommand())
    }
}
```

That's it. Parsing, tab completion, permissions, and error handling are all automatic.

## Installation

### Gradle (Kotlin DSL)

```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.maquq:KCommands:VERSION")
}
```

### Maven

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

> Replace `VERSION` with a release tag or commit hash from [JitPack](https://jitpack.io/#maquq/KCommands).

## Examples

### Subcommands

```kotlin
@Command("admin")
@Permission("myplugin.admin")
class AdminCommand {

    @SubCommand("reload")
    fun reload(@Sender sender: CommandSender) {
        sender.sendMessage("Config reloaded!")
    }

    @SubCommand("debug")
    fun debug(@Sender sender: CommandSender, @Arg("enabled") enabled: Boolean) {
        sender.sendMessage("Debug mode: $enabled")
    }
}
```

### Overloaded Handlers

Multiple `@DefaultHandler` methods are matched by argument count automatically.

```kotlin
@Command("tp")
class TpCommand {

    @DefaultHandler
    fun self(@Sender sender: Player, @Arg("target") target: Player) {
        sender.teleport(target)
    }

    @DefaultHandler
    fun other(@Sender sender: Player, @Arg("who") who: Player, @Arg("target") target: Player) {
        who.teleport(target)
    }
}
// /tp Steve -> teleports you to Steve
// /tp Steve Alex -> teleports Steve to Alex
```

### Flags and Greedy Strings

```kotlin
@Command("ban")
class BanCommand {

    @DefaultHandler
    fun ban(
        @Sender sender: Player,
        @Arg("target") target: String,
        @Arg("reason") @Greedy reason: String,
        @Flag("silent", short = 's') silent: Boolean = false
    ) {
        if (!silent) broadcast("$target was banned: $reason")
    }
}
// /ban Steve cheating with hacks -s
```

### Range Validation and Cooldowns

```kotlin
@Command("give")
class GiveCommand {

    @DefaultHandler
    @Cooldown(seconds = 5)
    fun give(
        @Sender sender: Player,
        @Arg("amount") @Range(min = 1.0, max = 64.0) amount: Int
    ) {
        // amount is guaranteed to be in [1, 64]
    }
}
```

### DSL Alternative

```kotlin
KCommands.register(plugin) {
    command("kit") {
        permission("myplugin.kit")
        description("Give a kit to a player")

        sub("give") {
            default {
                arg<Player>("target")
                arg<String>("kit")
                execute { sender, args ->
                    val target = args["target"] as Player
                    val kit = args["kit"] as String
                    sender.sendMessage("Gave kit $kit to ${target.name}")
                }
            }
        }
    }
}
```

## Developer Exception System

KCommands validates every command at registration time. Invalid definitions produce clear, formatted error reports instead of silent failures or stack traces.

```
+-------------------------------------------------------------+
|          KCommands - Command Registration Failed             |
+-------------------------------------------------------------+

  * MyBrokenCommand

    1) MyBrokenCommand::execute() -> param 'target'
       x Parameter 'target' has no @Arg or @Sender annotation
       -> Add @Arg("target") or @Sender to this parameter

       Fix:
       | fun execute(
       |     @Sender sender: Player,
       |     @Arg("target") target: ...  // <- add @Arg
       | )

  ==========================================
  1 error(s) - fix them and restart the server.
```

Detected errors include missing annotations, invalid sender types, duplicate overloads, `@Greedy` not in last position, unregistered resolver types, `@Range` on non-numeric parameters, conflicting flags, and more.

## Built-in Argument Resolvers

| Type | Input Example | Tab Completion |
|------|---------------|----------------|
| `String` | `hello` | - |
| `Int` | `42` | - |
| `Long` | `123456789` | - |
| `Double` | `3.14` | - |
| `Float` | `1.5` | - |
| `Boolean` | `true` / `false` | `true`, `false` |
| `Player` | `Steve` | Online players |
| `OfflinePlayer` | `Steve` | Known players |
| `Material` | `DIAMOND_SWORD` | All materials |
| `World` | `world_nether` | Loaded worlds |
| `GameMode` | `CREATIVE` | All game modes |
| `Duration` | `30s`, `5m`, `1h` | - |
| `UUID` | `550e8400-...` | - |
| `Location` | `100,64,200` | - |
| Any `Enum` | constant name | All constants |

Custom resolvers can be registered via `KCommands.setup { resolver(...) }` or `KCommands.registerResolver(...)`.

## Documentation

For detailed documentation, guides, and API reference, visit the [Wiki](https://github.com/maquqdev/KCommands/wiki).

## Contributing

Contributions are welcome. Please open an issue to discuss larger changes before submitting a pull request.

1. Fork the repository
2. Create a feature branch
3. Submit a pull request

## License

[MIT](LICENSE)

---

<p align="center">Built with <a href="https://kotlinlang.org">Kotlin</a> for the Bukkit/Paper ecosystem.</p>
