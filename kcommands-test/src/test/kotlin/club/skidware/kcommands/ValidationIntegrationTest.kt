package club.skidware.kcommands

import club.skidware.kcommands.annotation.*
import club.skidware.kcommands.error.*
import club.skidware.kcommands.scanner.ValidationPass
import club.skidware.kcommands.argument.BuiltinResolvers
import club.skidware.kcommands.argument.ArgumentResolverRegistry
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ValidationIntegrationTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: Plugin
    private lateinit var registry: ArgumentResolverRegistry
    private lateinit var validator: ValidationPass

    @BeforeEach
    fun setup() {
        server = MockBukkit.mock()
        plugin = MockBukkit.createMockPlugin()
        registry = ArgumentResolverRegistry()
        BuiltinResolvers.registerAll(registry)
        validator = ValidationPass(registry)
    }

    @AfterEach
    fun teardown() {
        KCommands.unregisterAll()
        MockBukkit.unmock()
    }

    class NoCommandAnnotation {
        @DefaultHandler
        fun run(@Sender sender: Player) {}
    }

    @Test
    fun `class without @Command annotation produces error`() {
        val errors = validator.validate(NoCommandAnnotation())
        assertTrue(errors.any { it is MissingCommandAnnotation },
            "Should detect missing @Command annotation: $errors")
    }

    @Command("empty")
    class EmptyCommand

    @Test
    fun `command with no handlers produces error`() {
        val errors = validator.validate(EmptyCommand())
        assertTrue(errors.any { it is NoHandlers },
            "Should detect no handlers: $errors")
    }

    @Command("nosender")
    class NoSenderCommand {
        @DefaultHandler
        fun run(@Arg("name") name: String) {}
    }

    @Test
    fun `handler without @Sender produces error`() {
        val errors = validator.validate(NoSenderCommand())
        assertTrue(errors.any { it is MissingSenderParameter },
            "Should detect missing @Sender: $errors")
    }

    @Command("multisender")
    class MultiSenderCommand {
        @DefaultHandler
        fun run(@Sender sender: Player, @Sender sender2: Player) {}
    }

    @Test
    fun `handler with multiple @Sender produces error`() {
        val errors = validator.validate(MultiSenderCommand())
        assertTrue(errors.any { it is MultipleSenderParameters },
            "Should detect multiple @Sender: $errors")
    }

    @Command("noarg")
    class NoArgCommand {
        @DefaultHandler
        fun run(@Sender sender: Player, name: String) {}
    }

    @Test
    fun `parameter without @Arg or @Flag produces error`() {
        val errors = validator.validate(NoArgCommand())
        assertTrue(errors.any { it is MissingArgAnnotation },
            "Should detect parameter without @Arg: $errors")
    }

    @Command("badorder")
    class BadOrderCommand {
        @DefaultHandler
        fun run(
            @Sender sender: Player,
            @Arg("optional") optional: String = "default",
            @Arg("required") required: String
        ) {}
    }

    @Test
    fun `required arg after optional produces error`() {
        val errors = validator.validate(BadOrderCommand())
        assertTrue(errors.any { it is RequiredAfterOptional },
            "Should detect required after optional: $errors")
    }

    @Command("greedybad")
    class GreedyNotLastCommand {
        @DefaultHandler
        fun run(
            @Sender sender: Player,
            @Arg("message") @Greedy message: String,
            @Arg("extra") extra: String
        ) {}
    }

    @Test
    fun `greedy arg not last produces error`() {
        val errors = validator.validate(GreedyNotLastCommand())
        assertTrue(errors.any { it is GreedyNotLast },
            "Should detect @Greedy not last: $errors")
    }

    @Command("greedyint")
    class GreedyIntCommand {
        @DefaultHandler
        fun run(
            @Sender sender: Player,
            @Arg("value") @Greedy value: Int
        ) {}
    }

    @Test
    fun `greedy on non-String type produces error`() {
        val errors = validator.validate(GreedyIntCommand())
        assertTrue(errors.any { it is GreedyNotString },
            "Should detect @Greedy on non-String: $errors")
    }

    @Command("rangebad")
    class RangeOnStringCommand {
        @DefaultHandler
        fun run(
            @Sender sender: Player,
            @Arg("name") @Range(min = 0.0, max = 10.0) name: String
        ) {}
    }

    @Test
    fun `range on non-numeric type produces error`() {
        val errors = validator.validate(RangeOnStringCommand())
        assertTrue(errors.any { it is RangeOnNonNumeric },
            "Should detect @Range on non-numeric: $errors")
    }

    @Command("rangeinv")
    class RangeInvertedCommand {
        @DefaultHandler
        fun run(
            @Sender sender: Player,
            @Arg("value") @Range(min = 100.0, max = 1.0) value: Int
        ) {}
    }

    @Test
    fun `inverted range produces error`() {
        val errors = validator.validate(RangeInvertedCommand())
        assertTrue(errors.any { it is RangeInverted },
            "Should detect inverted @Range: $errors")
    }

    @Command("conflict")
    class PlayerConsoleConflict {
        @DefaultHandler
        @PlayerOnly
        @ConsoleOnly
        fun run(@Sender sender: Player) {}
    }

    @Test
    fun `playerOnly and consoleOnly together produces error`() {
        val errors = validator.validate(PlayerConsoleConflict())
        assertTrue(errors.any { it is PlayerOnlyOnConsoleOnly },
            "Should detect @PlayerOnly + @ConsoleOnly conflict: $errors")
    }

    @Command("permhandler")
    class PermissionOnHandlerCommand {
        @DefaultHandler
        @Permission("bad.perm")
        fun run(@Sender sender: Player) {}
    }

    @Test
    fun `permission on defaultHandler produces error`() {
        val errors = validator.validate(PermissionOnHandlerCommand())
        assertTrue(errors.any { it is PermissionOnHandler },
            "Should detect @Permission on @DefaultHandler: $errors")
    }

    @Command("valid")
    @Permission("valid.use")
    @Description("A valid command")
    class ValidCommand {
        @DefaultHandler
        fun run(@Sender sender: Player, @Arg("name") name: String) {}

        @SubCommand("sub")
        @Permission("valid.sub")
        fun sub(@Sender sender: CommandSender, @Arg("value") value: Int) {}
    }

    @Test
    fun `valid command produces no errors`() {
        val errors = validator.validate(ValidCommand())
        assertTrue(errors.isEmpty(), "Valid command should produce no errors: $errors")
    }

    @Test
    fun `registering broken command does not crash and is skipped`() {
        KCommands.setup(plugin) { brigadier(false) }

        KCommands.register(plugin, NoCommandAnnotation())

        assertTrue(KCommands.getRegisteredNodes().isEmpty(),
            "Broken command should not be registered")
    }

    @Test
    fun `registering command with no handlers is skipped`() {
        KCommands.setup(plugin) { brigadier(false) }
        KCommands.register(plugin, EmptyCommand())

        assertTrue(KCommands.getRegisteredNodes().isEmpty(),
            "Command with no handlers should not be registered")
    }

    @Command("dupesub")
    class DuplicateSubCommandNames {
        @SubCommand("action")
        fun action1(@Sender sender: Player) {}

        @SubCommand("action")
        fun action2(@Sender sender: Player) {}
    }

    @Test
    fun `duplicate subcommand names produces error`() {
        val errors = validator.validate(DuplicateSubCommandNames())
        assertTrue(errors.any { it is DuplicateSubCommand },
            "Should detect duplicate @SubCommand names: $errors")
    }

    @Command("dupflag")
    class DuplicateFlagShortCommand {
        @DefaultHandler
        fun run(
            @Sender sender: Player,
            @Arg("target") target: String,
            @Flag("silent", short = 's') silent: Boolean = false,
            @Flag("skip", short = 's') skip: Boolean = false
        ) {}
    }

    @Test
    fun `duplicate flag short character produces error`() {
        val errors = validator.validate(DuplicateFlagShortCommand())
        assertTrue(errors.any { it is FlagDuplicateShort },
            "Should detect duplicate @Flag short: $errors")
    }

    @Command("dupflagname")
    class DuplicateFlagNameCommand {
        @DefaultHandler
        fun run(
            @Sender sender: Player,
            @Arg("target") target: String,
            @Flag("verbose") flag1: Boolean = false,
            @Flag("verbose") flag2: Boolean = false
        ) {}
    }

    @Test
    fun `duplicate flag names produces error`() {
        val errors = validator.validate(DuplicateFlagNameCommand())
        assertTrue(errors.any { it is FlagDuplicateName },
            "Should detect duplicate @Flag name: $errors")
    }

    @Command("badsender")
    class InvalidSenderTypeCommand {
        @DefaultHandler
        fun run(@Sender sender: String) {}
    }

    @Test
    fun `invalid sender type produces error`() {
        val errors = validator.validate(InvalidSenderTypeCommand())
        assertTrue(errors.any { it is InvalidSenderType },
            "Should detect invalid @Sender type: $errors")
    }
}
