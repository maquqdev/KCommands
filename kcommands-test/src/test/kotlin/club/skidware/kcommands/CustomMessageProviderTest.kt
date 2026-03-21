package club.skidware.kcommands

import club.skidware.kcommands.annotation.*
import club.skidware.kcommands.commands.*
import club.skidware.kcommands.message.IMessageProvider
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Custom message provider that uses simple plaintext messages (no MiniMessage tags)
 * and records which messages were sent for verification.
 */
class TrackingMessageProvider : IMessageProvider {
    val sentMessages = mutableListOf<String>()

    override val noPermission: String get() = "CUSTOM_NO_PERM".also { sentMessages.add(it) }
    override val playerOnly: String get() = "CUSTOM_PLAYER_ONLY".also { sentMessages.add(it) }
    override val consoleOnly: String get() = "CUSTOM_CONSOLE_ONLY".also { sentMessages.add(it) }
    override val invalidArg: String get() = "CUSTOM_INVALID_ARG {ARG}".also { sentMessages.add("invalidArg") }
    override val missingArg: String get() = "CUSTOM_MISSING_ARG {ARG}".also { sentMessages.add("missingArg") }
    override val usage: String get() = "CUSTOM_USAGE {USAGE}".also { sentMessages.add("usage") }
    override val outOfRange: String get() = "CUSTOM_OUT_OF_RANGE {ARG} {MIN} {MAX}".also { sentMessages.add("outOfRange") }
    override val cooldown: String get() = "CUSTOM_COOLDOWN {TIME}"
    override val unknownSubcmd: String get() = "CUSTOM_UNKNOWN_SUBCMD {INPUT} {AVAILABLE}".also { sentMessages.add("unknownSubcmd") }
    override val confirm: String get() = "CUSTOM_CONFIRM {TIME}"
    override val confirmExpired: String get() = "CUSTOM_CONFIRM_EXPIRED"
    override val commandError: String get() = "CUSTOM_CMD_ERROR"
    override val helpHeader: String get() = "CUSTOM_HELP_HEADER {COMMAND}"
    override val helpEntry: String get() = "CUSTOM_HELP_ENTRY {USAGE}"
    override val helpFooter: String get() = "CUSTOM_HELP_FOOTER {PAGE}"
}

class CustomMessageProviderTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: Plugin
    private lateinit var trackingProvider: TrackingMessageProvider

    @BeforeEach
    fun setup() {
        server = MockBukkit.mock()
        plugin = MockBukkit.createMockPlugin()
        trackingProvider = TrackingMessageProvider()
        KCommands.setup(plugin) {
            brigadier(false)
            messageProvider(trackingProvider)
        }
    }

    @AfterEach
    fun teardown() {
        KCommands.unregisterAll()
        MockBukkit.unmock()
    }

    @Test
    fun `custom message provider is used for permission denied`() {
        val cmd = HealCommand()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("NoPerm")
        trackingProvider.sentMessages.clear()
        server.dispatchCommand(player, "heal")

        assertNull(cmd.lastHealed, "heal should not execute")
        assertTrue(trackingProvider.sentMessages.contains("CUSTOM_NO_PERM"),
            "Custom noPermission message should be triggered: ${trackingProvider.sentMessages}")
    }

    @Test
    fun `custom message provider is used for player only`() {
        val cmd = PlayerOnlyCommand()
        KCommands.register(plugin, cmd)

        trackingProvider.sentMessages.clear()
        val consoleSender = server.consoleSender
        server.dispatchCommand(consoleSender, "playeronly")

        assertFalse(cmd.executed, "PlayerOnly command should not execute from console")
        assertTrue(trackingProvider.sentMessages.contains("CUSTOM_PLAYER_ONLY"),
            "Custom playerOnly message should be triggered: ${trackingProvider.sentMessages}")
    }

    @Test
    fun `custom message provider is used for out of range`() {
        val cmd = GiveCommand()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("Giver")
        trackingProvider.sentMessages.clear()
        server.dispatchCommand(player, "give 0")

        assertNull(cmd.lastAmount, "give 0 should be rejected")
        assertTrue(trackingProvider.sentMessages.contains("outOfRange"),
            "Custom outOfRange message should be triggered: ${trackingProvider.sentMessages}")
    }

    @Test
    fun `custom message provider is used for unknown subcommand`() {
        val cmd = AdminCommands()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("Admin")
        player.addAttachment(plugin, "admin.base", true)
        trackingProvider.sentMessages.clear()
        server.dispatchCommand(player, "admin nonexistent")

        assertTrue(trackingProvider.sentMessages.contains("unknownSubcmd"),
            "Custom unknownSubcmd message should be triggered: ${trackingProvider.sentMessages}")
    }

    @Test
    fun `custom message provider is used for usage help when no handler matches`() {
        val cmd = GiveCommand()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("Giver")
        trackingProvider.sentMessages.clear()
        server.dispatchCommand(player, "give")

        assertNull(cmd.lastAmount, "give with no args should fail")
        assertTrue(trackingProvider.sentMessages.contains("usage"),
            "Custom usage message should be triggered when no handler matches: ${trackingProvider.sentMessages}")
    }

    @Test
    fun `setup with custom messages data class works`() {
        KCommands.unregisterAll()
        KCommands.setup(plugin) {
            brigadier(false)
            messages {
                copy(noPermission = "Denied by admin")
            }
        }

        val cmd = HealCommand()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("Blocked")
        server.dispatchCommand(player, "heal")

        assertNull(cmd.lastHealed, "heal should not execute without permission")
    }

    @Test
    fun `message provider interface contract is satisfied`() {
        val provider: IMessageProvider = trackingProvider
        assertNotNull(provider.noPermission)
        assertNotNull(provider.playerOnly)
        assertNotNull(provider.consoleOnly)
        assertNotNull(provider.invalidArg)
        assertNotNull(provider.missingArg)
        assertNotNull(provider.usage)
        assertNotNull(provider.outOfRange)
        assertNotNull(provider.cooldown)
        assertNotNull(provider.unknownSubcmd)
        assertNotNull(provider.confirm)
        assertNotNull(provider.confirmExpired)
        assertNotNull(provider.commandError)
        assertNotNull(provider.helpHeader)
        assertNotNull(provider.helpEntry)
        assertNotNull(provider.helpFooter)
    }

    @Test
    fun `multiple commands share same message provider`() {
        val healCmd = HealCommand()
        val giveCmd = GiveCommand()
        KCommands.register(plugin, healCmd, giveCmd)

        val player = server.addPlayer("Multi")
        trackingProvider.sentMessages.clear()

        server.dispatchCommand(player, "heal")
        assertTrue(trackingProvider.sentMessages.contains("CUSTOM_NO_PERM"),
            "Should use custom no perm for heal")

        trackingProvider.sentMessages.clear()

        server.dispatchCommand(player, "give 0")
        assertTrue(trackingProvider.sentMessages.contains("outOfRange"),
            "Should use custom out of range for give")
    }
}
