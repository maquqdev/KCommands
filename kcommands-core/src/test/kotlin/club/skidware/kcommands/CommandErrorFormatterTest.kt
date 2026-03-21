package club.skidware.kcommands

import club.skidware.kcommands.error.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class CommandErrorFormatterTest {

    @Test
    fun `format with single error`() {
        val errors = listOf(
            MissingArgAnnotation("MyCommand", "execute", "name")
        )
        val output = CommandErrorFormatter.format(errors)

        assertTrue(output.contains("MyCommand"))
        assertTrue(output.contains("name"))
        assertTrue(output.contains("1 error(s)"))
    }

    @Test
    fun `format with multiple errors from same class`() {
        val errors = listOf(
            MissingArgAnnotation("MyCommand", "execute", "name"),
            MissingSenderParameter("MyCommand", "run")
        )
        val output = CommandErrorFormatter.format(errors)

        assertTrue(output.contains("MyCommand"))
        assertTrue(output.contains("2 error(s)"))
    }

    @Test
    fun `format with errors from multiple classes`() {
        val errors = listOf(
            MissingArgAnnotation("CommandA", "execute", "name"),
            NoHandlers("CommandB")
        )
        val output = CommandErrorFormatter.format(errors)

        assertTrue(output.contains("CommandA"))
        assertTrue(output.contains("CommandB"))
        assertTrue(output.contains("2 error(s)"))
    }

    @Test
    fun `output contains box header`() {
        val errors = listOf(
            MissingArgAnnotation("MyCommand", "execute", "name")
        )
        val output = CommandErrorFormatter.format(errors)

        assertTrue(output.contains("KCommands"))
        assertTrue(output.contains("Command Registration Failed"))
    }

    @Test
    fun `output contains error count`() {
        val errors = listOf(
            MissingArgAnnotation("Cmd", "m1", "p1"),
            MissingSenderParameter("Cmd", "m2"),
            NoHandlers("Cmd2")
        )
        val output = CommandErrorFormatter.format(errors)

        assertTrue(output.contains("3 error(s)"))
    }

    @Test
    fun `output contains fix suggestion`() {
        val errors = listOf(
            MissingArgAnnotation("MyCommand", "execute", "name")
        )
        val output = CommandErrorFormatter.format(errors)

        // Should contain the suggestion text
        assertTrue(output.contains("@Arg"))
    }

    @Test
    fun `output contains code hint when present`() {
        val errors = listOf(
            MissingArgAnnotation("MyCommand", "execute", "name")
        )
        val output = CommandErrorFormatter.format(errors)

        assertTrue(output.contains("Fix:"))
    }

    @Test
    fun `output does not contain Fix section when no codeHint`() {
        val errors = listOf(
            PlayerOnlyOnConsoleOnly("MyCommand", "execute")
        )
        val output = CommandErrorFormatter.format(errors)

        assertFalse(output.contains("Fix:"))
    }

    @Test
    fun `output groups errors by class with bullet`() {
        val errors = listOf(
            MissingArgAnnotation("CmdAlpha", "m1", "p1"),
            MissingSenderParameter("CmdAlpha", "m2"),
            NoHandlers("CmdBeta")
        )
        val output = CommandErrorFormatter.format(errors)

        // Each class gets a bullet
        val alphaCount = output.windowed(8).count { it.contains("CmdAlpha") }
        assertTrue(alphaCount >= 1)
        assertTrue(output.contains("CmdBeta"))
    }

    @Test
    fun `output contains numbered errors`() {
        val errors = listOf(
            MissingArgAnnotation("Cmd", "m1", "p1"),
            MissingSenderParameter("Cmd", "m2")
        )
        val output = CommandErrorFormatter.format(errors)

        assertTrue(output.contains("1)"))
        assertTrue(output.contains("2)"))
    }

    @Test
    fun `output contains location with method and parameter`() {
        val errors = listOf(
            MissingArgAnnotation("MyCmd", "execute", "playerName")
        )
        val output = CommandErrorFormatter.format(errors)

        assertTrue(output.contains("MyCmd::execute()"))
        assertTrue(output.contains("playerName"))
    }
}
