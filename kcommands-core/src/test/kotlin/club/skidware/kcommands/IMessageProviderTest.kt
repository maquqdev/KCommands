package club.skidware.kcommands

import club.skidware.kcommands.message.CommandMessages
import club.skidware.kcommands.message.DefaultMessageProvider
import club.skidware.kcommands.message.IMessageProvider
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class IMessageProviderTest {

    @Test
    fun `DefaultMessageProvider returns correct defaults`() {
        val provider = DefaultMessageProvider()

        assertTrue(provider.noPermission.contains("No permission"))
        assertTrue(provider.playerOnly.contains("player-only"))
        assertTrue(provider.consoleOnly.contains("console-only"))
        assertTrue(provider.invalidArg.contains("{ARG}"))
        assertTrue(provider.missingArg.contains("{ARG}"))
        assertTrue(provider.usage.contains("{USAGE}"))
        assertTrue(provider.outOfRange.contains("{MIN}"))
        assertTrue(provider.outOfRange.contains("{MAX}"))
        assertTrue(provider.cooldown.contains("{TIME}"))
        assertTrue(provider.unknownSubcmd.contains("{INPUT}"))
        assertTrue(provider.confirm.contains("confirm"))
        assertTrue(provider.confirmExpired.contains("expired"))
        assertTrue(provider.commandError.contains("error"))
        assertTrue(provider.helpHeader.contains("{COMMAND}"))
        assertTrue(provider.helpEntry.contains("{USAGE}"))
        assertTrue(provider.helpFooter.contains("{PAGE}"))
    }

    @Test
    fun `DefaultMessageProvider with custom CommandMessages`() {
        val messages = CommandMessages(
            noPermission = "Custom no perm",
            playerOnly = "Custom player only",
            consoleOnly = "Custom console only",
            invalidArg = "Custom invalid {ARG}",
            missingArg = "Custom missing {ARG}",
            usage = "Custom usage {USAGE}",
            outOfRange = "Custom range {MIN} {MAX}",
            cooldown = "Custom cooldown {TIME}",
            unknownSubcmd = "Custom unknown {INPUT}",
            confirm = "Custom confirm {TIME}",
            confirmExpired = "Custom expired",
            commandError = "Custom error",
            helpHeader = "Custom header {COMMAND}",
            helpEntry = "Custom entry {USAGE}",
            helpFooter = "Custom footer {PAGE}"
        )
        val provider = DefaultMessageProvider(messages)

        assertEquals("Custom no perm", provider.noPermission)
        assertEquals("Custom player only", provider.playerOnly)
        assertEquals("Custom console only", provider.consoleOnly)
        assertEquals("Custom invalid {ARG}", provider.invalidArg)
        assertEquals("Custom missing {ARG}", provider.missingArg)
        assertEquals("Custom usage {USAGE}", provider.usage)
        assertEquals("Custom range {MIN} {MAX}", provider.outOfRange)
        assertEquals("Custom cooldown {TIME}", provider.cooldown)
        assertEquals("Custom unknown {INPUT}", provider.unknownSubcmd)
        assertEquals("Custom confirm {TIME}", provider.confirm)
        assertEquals("Custom expired", provider.confirmExpired)
        assertEquals("Custom error", provider.commandError)
        assertEquals("Custom header {COMMAND}", provider.helpHeader)
        assertEquals("Custom entry {USAGE}", provider.helpEntry)
        assertEquals("Custom footer {PAGE}", provider.helpFooter)
    }

    @Test
    fun `custom IMessageProvider implementation works`() {
        val custom = object : IMessageProvider {
            override val noPermission = "Nope"
            override val playerOnly = "Players only"
            override val consoleOnly = "Console only"
            override val invalidArg = "Bad arg"
            override val missingArg = "Need arg"
            override val usage = "Use it"
            override val outOfRange = "Out of range"
            override val cooldown = "Wait"
            override val unknownSubcmd = "Unknown"
            override val confirm = "Confirm?"
            override val confirmExpired = "Too late"
            override val commandError = "Oops"
            override val helpHeader = "Help"
            override val helpEntry = "Entry"
            override val helpFooter = "Footer"
        }

        assertEquals("Nope", custom.noPermission)
        assertEquals("Players only", custom.playerOnly)
        assertEquals("Console only", custom.consoleOnly)
        assertEquals("Bad arg", custom.invalidArg)
        assertEquals("Need arg", custom.missingArg)
        assertEquals("Use it", custom.usage)
        assertEquals("Out of range", custom.outOfRange)
        assertEquals("Wait", custom.cooldown)
        assertEquals("Unknown", custom.unknownSubcmd)
        assertEquals("Confirm?", custom.confirm)
        assertEquals("Too late", custom.confirmExpired)
        assertEquals("Oops", custom.commandError)
        assertEquals("Help", custom.helpHeader)
        assertEquals("Entry", custom.helpEntry)
        assertEquals("Footer", custom.helpFooter)
    }

    @Test
    fun `IMessageProvider can be assigned from DefaultMessageProvider`() {
        val provider: IMessageProvider = DefaultMessageProvider()
        assertNotNull(provider.noPermission)
        assertNotNull(provider.playerOnly)
    }

    @Test
    fun `DefaultMessageProvider with default constructor matches CommandMessages defaults`() {
        val provider = DefaultMessageProvider()
        val defaults = CommandMessages()

        assertEquals(defaults.noPermission, provider.noPermission)
        assertEquals(defaults.playerOnly, provider.playerOnly)
        assertEquals(defaults.consoleOnly, provider.consoleOnly)
        assertEquals(defaults.invalidArg, provider.invalidArg)
        assertEquals(defaults.missingArg, provider.missingArg)
        assertEquals(defaults.usage, provider.usage)
        assertEquals(defaults.outOfRange, provider.outOfRange)
        assertEquals(defaults.cooldown, provider.cooldown)
        assertEquals(defaults.unknownSubcmd, provider.unknownSubcmd)
        assertEquals(defaults.confirm, provider.confirm)
        assertEquals(defaults.confirmExpired, provider.confirmExpired)
        assertEquals(defaults.commandError, provider.commandError)
        assertEquals(defaults.helpHeader, provider.helpHeader)
        assertEquals(defaults.helpEntry, provider.helpEntry)
        assertEquals(defaults.helpFooter, provider.helpFooter)
    }

    @Test
    fun `DefaultMessageProvider with partially customized CommandMessages`() {
        val messages = CommandMessages(
            noPermission = "Access denied",
            cooldown = "Slow down!"
        )
        val provider = DefaultMessageProvider(messages)

        // Custom values
        assertEquals("Access denied", provider.noPermission)
        assertEquals("Slow down!", provider.cooldown)

        // Remaining defaults
        val defaults = CommandMessages()
        assertEquals(defaults.playerOnly, provider.playerOnly)
        assertEquals(defaults.consoleOnly, provider.consoleOnly)
        assertEquals(defaults.invalidArg, provider.invalidArg)
    }
}
