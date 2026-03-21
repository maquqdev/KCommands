package club.skidware.kcommands

import club.skidware.kcommands.commands.*
import org.mockbukkit.mockbukkit.MockBukkit
import org.mockbukkit.mockbukkit.ServerMock
import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class TabCompletionTest {
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
    fun `admin command suggests subcommands`() {
        val cmd = AdminCommands()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("Admin")
        player.addAttachment(plugin, "admin.base", true)
        player.addAttachment(plugin, "admin.reload", true)

        val commandMap = Bukkit.getServer().commandMap
        val bukkitCmd = commandMap.getCommand("admin")
        assertNotNull(bukkitCmd, "admin command should be registered in command map")

        val completions = bukkitCmd!!.tabComplete(player, "admin", arrayOf(""))
        assertTrue(completions.contains("reload"), "Should suggest 'reload': $completions")
        assertTrue(completions.contains("debug"), "Should suggest 'debug': $completions")
    }

    @Test
    fun `admin command filters subcommand suggestions by partial input`() {
        val cmd = AdminCommands()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("Admin")
        player.addAttachment(plugin, "admin.base", true)
        player.addAttachment(plugin, "admin.reload", true)

        val commandMap = Bukkit.getServer().commandMap
        val bukkitCmd = commandMap.getCommand("admin")!!

        val completions = bukkitCmd.tabComplete(player, "admin", arrayOf("re"))
        assertTrue(completions.contains("reload"), "Should suggest 'reload' for prefix 're': $completions")
        assertFalse(completions.contains("debug"), "Should NOT suggest 'debug' for prefix 're': $completions")
    }

    @Test
    fun `admin command shows no subcommand suggestions for non-matching input`() {
        val cmd = AdminCommands()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("Admin")
        player.addAttachment(plugin, "admin.base", true)
        player.addAttachment(plugin, "admin.reload", true)

        val commandMap = Bukkit.getServer().commandMap
        val bukkitCmd = commandMap.getCommand("admin")!!

        val completions = bukkitCmd.tabComplete(player, "admin", arrayOf("xyz"))
        assertTrue(completions.isEmpty(), "No subcommand should match 'xyz': $completions")
    }

    @Test
    fun `tp command suggests online player names`() {
        val cmd = TpCommand()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("Mover")
        val target1 = server.addPlayer("Alice")
        val target2 = server.addPlayer("Bob")

        val commandMap = Bukkit.getServer().commandMap
        val bukkitCmd = commandMap.getCommand("tp")!!

        val completions = bukkitCmd.tabComplete(player, "tp", arrayOf(""))
        assertTrue(completions.contains("Alice"), "Should suggest online player 'Alice': $completions")
        assertTrue(completions.contains("Bob"), "Should suggest online player 'Bob': $completions")
    }

    @Test
    fun `tp command filters player suggestions by prefix`() {
        val cmd = TpCommand()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("Mover")
        val alice = server.addPlayer("Alice")
        val bob = server.addPlayer("Bob")

        val commandMap = Bukkit.getServer().commandMap
        val bukkitCmd = commandMap.getCommand("tp")!!

        val completions = bukkitCmd.tabComplete(player, "tp", arrayOf("A"))
        assertTrue(completions.contains("Alice"), "Should suggest 'Alice' for prefix 'A': $completions")
        assertFalse(completions.contains("Bob"), "Should NOT suggest 'Bob' for prefix 'A': $completions")
    }

    @Test
    fun `admin debug suggests boolean values`() {
        val cmd = AdminCommands()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("Admin")
        player.addAttachment(plugin, "admin.base", true)

        val commandMap = Bukkit.getServer().commandMap
        val bukkitCmd = commandMap.getCommand("admin")!!

        val completions = bukkitCmd.tabComplete(player, "admin", arrayOf("debug", ""))
        assertTrue(completions.contains("true"), "Should suggest 'true': $completions")
        assertTrue(completions.contains("false"), "Should suggest 'false': $completions")
    }

    @Test
    fun `admin debug filters boolean suggestions by prefix`() {
        val cmd = AdminCommands()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("Admin")
        player.addAttachment(plugin, "admin.base", true)

        val commandMap = Bukkit.getServer().commandMap
        val bukkitCmd = commandMap.getCommand("admin")!!

        val completions = bukkitCmd.tabComplete(player, "admin", arrayOf("debug", "t"))
        assertTrue(completions.contains("true"), "Should suggest 'true' for prefix 't': $completions")
        assertFalse(completions.contains("false"), "Should NOT suggest 'false' for prefix 't': $completions")
    }

    @Test
    fun `greet command has no specific completions for string arg`() {
        val cmd = GreetCommand()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("Greeter")

        val commandMap = Bukkit.getServer().commandMap
        val bukkitCmd = commandMap.getCommand("greet")!!

        val completions = bukkitCmd.tabComplete(player, "greet", arrayOf(""))
        assertTrue(completions.isEmpty(), "String arg should have no completions: $completions")
    }

    @Test
    fun `flag suggestions when typing dash`() {
        val cmd = FlagTestCommand()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("Flagger")

        val commandMap = Bukkit.getServer().commandMap
        val bukkitCmd = commandMap.getCommand("flagtest")!!

        val completions = bukkitCmd.tabComplete(player, "flagtest", arrayOf("target", "-"))
        assertTrue(completions.contains("--silent") || completions.contains("-s"),
            "Should suggest flag options when typing '-': $completions")
    }

    @Test
    fun `subcommand suggestions hide permission-restricted subcommands`() {
        val cmd = AdminCommands()
        KCommands.register(plugin, cmd)

        val player = server.addPlayer("LimitedAdmin")
        player.addAttachment(plugin, "admin.base", true)

        val commandMap = Bukkit.getServer().commandMap
        val bukkitCmd = commandMap.getCommand("admin")!!

        val completions = bukkitCmd.tabComplete(player, "admin", arrayOf(""))
        assertFalse(completions.contains("reload"),
            "Should NOT suggest 'reload' without admin.reload permission: $completions")
        assertTrue(completions.contains("debug"),
            "Should still suggest 'debug' (no specific permission): $completions")
    }
}
