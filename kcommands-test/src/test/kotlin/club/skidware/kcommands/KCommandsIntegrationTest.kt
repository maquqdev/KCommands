package club.skidware.kcommands

import club.skidware.kcommands.commands.*
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.bukkit.plugin.Plugin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class KCommandsIntegrationTest {
    private lateinit var server: ServerMock
    private lateinit var plugin: Plugin

    @BeforeEach
    fun setup() {
        server = MockBukkit.mock()
        plugin = MockBukkit.createMockPlugin()
        KCommands.setup(plugin) { brigadier(false) }
    }

    @AfterEach
    fun teardown() {
        KCommands.unregisterAll()
        MockBukkit.unmock()
    }

    @Test
    fun `register simple command and verify it appears in registeredNodes`() {
        val cmd = HealCommand()
        KCommands.register(plugin, cmd)

        val nodes = KCommands.getRegisteredNodes()
        assertTrue(nodes.any { it.name == "heal" }, "heal command should be registered")
    }

    @Test
    fun `register command with aliases`() {
        val cmd = TpCommand()
        KCommands.register(plugin, cmd)

        val nodes = KCommands.getRegisteredNodes()
        val tpNode = nodes.first { it.name == "tp" }
        assertTrue(tpNode.aliases.contains("teleport"), "tp should have 'teleport' alias")
    }

    @Test
    fun `register command with subcommands`() {
        val cmd = AdminCommands()
        KCommands.register(plugin, cmd)

        val nodes = KCommands.getRegisteredNodes()
        val adminNode = nodes.first { it.name == "admin" }
        assertTrue(adminNode.children.containsKey("reload"), "admin should have 'reload' subcommand")
        assertTrue(adminNode.children.containsKey("debug"), "admin should have 'debug' subcommand")
    }

    @Test
    fun `register multiple commands`() {
        KCommands.register(plugin, HealCommand(), TpCommand(), GreetCommand())

        val nodes = KCommands.getRegisteredNodes()
        assertEquals(3, nodes.size, "Should have 3 registered commands")
        assertTrue(nodes.any { it.name == "heal" })
        assertTrue(nodes.any { it.name == "tp" })
        assertTrue(nodes.any { it.name == "greet" })
    }

    @Test
    fun `execute simple command via dispatchCommand`() {
        val cmd = HealCommand()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("Healer")
        player.addAttachment(plugin, "test.heal", true)
        server.dispatchCommand(player, "heal")

        assertNotNull(cmd.lastHealed, "heal command should have been executed")
        assertEquals("Healer", cmd.lastHealed?.name)
    }

    @Test
    fun `command sets player health`() {
        val cmd = HealCommand()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("Wounded")
        player.addAttachment(plugin, "test.heal", true)
        player.health = 5.0
        server.dispatchCommand(player, "heal")

        assertEquals(20.0, player.health, 0.01, "Player health should be 20 after heal")
    }

    @Test
    fun `command denied without permission`() {
        val cmd = HealCommand()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("NoPerm")
        server.dispatchCommand(player, "heal")

        assertNull(cmd.lastHealed, "heal should NOT execute without permission")
    }

    @Test
    fun `command executes with permission`() {
        val cmd = HealCommand()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("HasPerm")
        player.addAttachment(plugin, "test.heal", true)
        server.dispatchCommand(player, "heal")

        assertNotNull(cmd.lastHealed, "heal should execute with permission")
    }

    @Test
    fun `subcommand reload dispatches correctly`() {
        val cmd = AdminCommands()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("Admin")
        player.addAttachment(plugin, "admin.base", true)
        player.addAttachment(plugin, "admin.reload", true)
        server.dispatchCommand(player, "admin reload")

        assertTrue(cmd.reloaded, "admin reload should set reloaded flag")
    }

    @Test
    fun `subcommand debug with boolean arg`() {
        val cmd = AdminCommands()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("Admin")
        player.addAttachment(plugin, "admin.base", true)
        server.dispatchCommand(player, "admin debug true")

        assertEquals(true, cmd.lastDebugState, "debug should be set to true")
    }

    @Test
    fun `subcommand permission denied`() {
        val cmd = AdminCommands()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("LowAdmin")
        player.addAttachment(plugin, "admin.base", true)
        server.dispatchCommand(player, "admin reload")

        assertFalse(cmd.reloaded, "reload should NOT execute without admin.reload permission")
    }

    @Test
    fun `tp command with one arg uses teleportSelf`() {
        val cmd = TpCommand()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("Mover")
        val target = server.addPlayer("Target")
        server.dispatchCommand(player, "tp Target")

        assertTrue(cmd.teleportedSelf, "tp with 1 arg should call teleportSelf")
        assertFalse(cmd.teleportedOther, "teleportOther should not be called")
    }

    @Test
    fun `tp command with two args uses teleportOther`() {
        val cmd = TpCommand()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("Mover")
        val target = server.addPlayer("Target")
        val dest = server.addPlayer("Dest")
        server.dispatchCommand(player, "tp Target Dest")

        assertTrue(cmd.teleportedOther, "tp with 2 args should call teleportOther")
    }

    @Test
    fun `greedy argument consumes all remaining words`() {
        val cmd = GreetCommand()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("Greeter")
        server.dispatchCommand(player, "greet hello world how are you")

        assertEquals("hello world how are you", cmd.lastMessage, "Greedy arg should capture entire message")
    }

    @Test
    fun `greedy argument with single word`() {
        val cmd = GreetCommand()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("Greeter")
        server.dispatchCommand(player, "greet hello")

        assertEquals("hello", cmd.lastMessage, "Greedy arg should work with single word")
    }

    @Test
    fun `range valid value succeeds`() {
        val cmd = GiveCommand()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("Giver")
        server.dispatchCommand(player, "give 32")

        assertEquals(32, cmd.lastAmount, "give 32 should succeed")
    }

    @Test
    fun `range below minimum is rejected`() {
        val cmd = GiveCommand()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("Giver")
        server.dispatchCommand(player, "give 0")

        assertNull(cmd.lastAmount, "give 0 should be rejected (below min 1)")
    }

    @Test
    fun `range above maximum is rejected`() {
        val cmd = GiveCommand()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("Giver")
        server.dispatchCommand(player, "give 100")

        assertNull(cmd.lastAmount, "give 100 should be rejected (above max 64)")
    }

    @Test
    fun `range at exact boundaries succeeds`() {
        val cmd = GiveCommand()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("Giver")

        server.dispatchCommand(player, "give 1")
        assertEquals(1, cmd.lastAmount, "give 1 (min boundary) should succeed")

        cmd.lastAmount = null
        server.dispatchCommand(player, "give 64")
        assertEquals(64, cmd.lastAmount, "give 64 (max boundary) should succeed")
    }

    @Test
    fun `playeronly command executes for player`() {
        val cmd = PlayerOnlyCommand()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("Tester")
        server.dispatchCommand(player, "playeronly")

        assertTrue(cmd.executed, "PlayerOnly command should execute for a player")
    }

    @Test
    fun `playeronly command denied for console`() {
        val cmd = PlayerOnlyCommand()
        KCommands.register(plugin, cmd)

        val consoleSender = server.consoleSender
        server.dispatchCommand(consoleSender, "playeronly")

        assertFalse(cmd.executed, "PlayerOnly command should NOT execute from console")
    }

    @Test
    fun `unknown subcommand does not execute any handler`() {
        val cmd = AdminCommands()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("Admin")
        player.addAttachment(plugin, "admin.base", true)
        server.dispatchCommand(player, "admin nonexistent")

        assertFalse(cmd.reloaded, "No handler should execute for unknown subcommand")
        assertNull(cmd.lastDebugState, "No handler should execute for unknown subcommand")
    }

    @Test
    fun `info command with no args calls noArgs handler`() {
        val cmd = InfoCommand()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("InfoUser")
        server.dispatchCommand(player, "info")

        assertTrue(cmd.noArgsExecuted, "info with no args should call noArgs handler")
        assertNull(cmd.lastNameArg, "name arg should be null for no-arg call")
    }

    @Test
    fun `info command with name arg calls withName handler`() {
        val cmd = InfoCommand()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("InfoUser")
        server.dispatchCommand(player, "info Alice")

        assertEquals("Alice", cmd.lastNameArg, "info with arg should call withName handler")
    }

    @Test
    fun `flag command with boolean flag`() {
        val cmd = FlagTestCommand()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("Flagger")
        server.dispatchCommand(player, "flagtest MyTarget --silent")

        assertTrue(cmd.silentFlag, "silent flag should be true")
    }

    @Test
    fun `flag command with short flag`() {
        val cmd = FlagTestCommand()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("Flagger")
        server.dispatchCommand(player, "flagtest MyTarget -s")

        assertTrue(cmd.silentFlag, "silent flag via -s should be true")
    }

    @Test
    fun `flag command without flags uses defaults`() {
        val cmd = FlagTestCommand()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("Flagger")
        server.dispatchCommand(player, "flagtest MyTarget")

        assertFalse(cmd.silentFlag, "silent flag should default to false")
        assertEquals(1, cmd.countFlag, "count flag should default to 1")
    }

    @Test
    fun `unregisterAll clears all registered nodes`() {
        KCommands.register(plugin, HealCommand(), TpCommand())
        assertEquals(2, KCommands.getRegisteredNodes().size)

        KCommands.unregisterAll()
        assertEquals(0, KCommands.getRegisteredNodes().size, "All nodes should be cleared after unregisterAll")
    }
}
