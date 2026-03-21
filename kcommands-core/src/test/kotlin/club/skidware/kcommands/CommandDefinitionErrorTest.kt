package club.skidware.kcommands

import club.skidware.kcommands.error.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class CommandDefinitionErrorTest {

    @Test
    fun `MissingArgAnnotation has correct message and suggestion`() {
        val error = MissingArgAnnotation("MyCmd", "execute", "name")
        assertTrue(error.message.contains("name"))
        assertTrue(error.message.contains("@Arg"))
        assertTrue(error.suggestion.isNotEmpty())
        assertNotNull(error.codeHint)
        assertEquals("MyCmd", error.commandClass)
        assertEquals("execute", error.method)
        assertEquals("name", error.parameter)
    }

    @Test
    fun `MissingSenderParameter has correct message and suggestion`() {
        val error = MissingSenderParameter("MyCmd", "execute")
        assertTrue(error.message.contains("@Sender"))
        assertTrue(error.suggestion.isNotEmpty())
        assertNotNull(error.codeHint)
        assertEquals("MyCmd", error.commandClass)
        assertEquals("execute", error.method)
        assertNull(error.parameter)
    }

    @Test
    fun `MultipleSenderParameters has correct message and suggestion`() {
        val error = MultipleSenderParameters("MyCmd", "execute", listOf("s1", "s2"))
        assertTrue(error.message.contains("s1"))
        assertTrue(error.message.contains("s2"))
        assertTrue(error.suggestion.contains("one"))
        assertNull(error.codeHint)
    }

    @Test
    fun `InvalidSenderType has correct message and suggestion`() {
        val error = InvalidSenderType("MyCmd", "execute", "sender", "String")
        assertTrue(error.message.contains("String"))
        assertTrue(error.suggestion.contains("Player"))
        assertNotNull(error.codeHint)
    }

    @Test
    fun `RequiredAfterOptional has correct message and suggestion`() {
        val error = RequiredAfterOptional("MyCmd", "execute", "reqArg", "optArg")
        assertTrue(error.message.contains("reqArg"))
        assertTrue(error.message.contains("optArg"))
        assertTrue(error.suggestion.isNotEmpty())
        assertNotNull(error.codeHint)
    }

    @Test
    fun `GreedyNotLast has correct message and suggestion`() {
        val error = GreedyNotLast("MyCmd", "execute", "msg", "extra")
        assertTrue(error.message.contains("msg"))
        assertTrue(error.message.contains("extra"))
        assertTrue(error.suggestion.contains("last"))
        assertNotNull(error.codeHint)
    }

    @Test
    fun `GreedyNotString has correct message and suggestion`() {
        val error = GreedyNotString("MyCmd", "execute", "msg", "Int")
        assertTrue(error.message.contains("msg"))
        assertTrue(error.message.contains("Int"))
        assertTrue(error.suggestion.contains("String"))
        assertNotNull(error.codeHint)
    }

    @Test
    fun `NoResolver has correct message and suggestion`() {
        val error = NoResolver("MyCmd", "execute", "target", "CustomType")
        assertTrue(error.message.contains("CustomType"))
        assertTrue(error.suggestion.isNotEmpty())
        assertNotNull(error.codeHint)
    }

    @Test
    fun `AmbiguousOverload has correct message and suggestion`() {
        val error = AmbiguousOverload("MyCmd", listOf("handler1", "handler2"), 1)
        assertTrue(error.message.contains("2"))
        assertTrue(error.message.contains("handler1"))
        assertTrue(error.suggestion.isNotEmpty())
        assertNotNull(error.codeHint)
        assertNull(error.method)
        assertNull(error.parameter)
    }

    @Test
    fun `MissingCommandAnnotation has correct message and suggestion`() {
        val error = MissingCommandAnnotation("MyCmd")
        assertTrue(error.message.contains("MyCmd"))
        assertTrue(error.message.contains("@Command"))
        assertTrue(error.suggestion.isNotEmpty())
        assertNotNull(error.codeHint)
    }

    @Test
    fun `NoHandlers has correct message and suggestion`() {
        val error = NoHandlers("MyCmd")
        assertTrue(error.message.contains("@DefaultHandler"))
        assertTrue(error.message.contains("@SubCommand"))
        assertTrue(error.suggestion.isNotEmpty())
        assertNotNull(error.codeHint)
    }

    @Test
    fun `DuplicateSubCommand has correct message and suggestion`() {
        val error = DuplicateSubCommand("MyCmd", "help", listOf("help1", "help2"))
        assertTrue(error.message.contains("help"))
        assertTrue(error.message.contains("help1"))
        assertTrue(error.suggestion.isNotEmpty())
        assertNotNull(error.codeHint)
    }

    @Test
    fun `RangeOnNonNumeric has correct message and suggestion`() {
        val error = RangeOnNonNumeric("MyCmd", "execute", "name", "String")
        assertTrue(error.message.contains("name"))
        assertTrue(error.message.contains("String"))
        assertTrue(error.suggestion.contains("Int"))
        assertNotNull(error.codeHint)
    }

    @Test
    fun `RangeInverted has correct message and suggestion`() {
        val error = RangeInverted("MyCmd", "execute", "count", 100.0, 0.0)
        assertTrue(error.message.contains("100.0"))
        assertTrue(error.message.contains("0.0"))
        assertTrue(error.suggestion.contains("Swap"))
        assertNotNull(error.codeHint)
    }

    @Test
    fun `FlagDuplicateShort has correct message and suggestion`() {
        val error = FlagDuplicateShort("MyCmd", "execute", 'v', listOf("verbose", "version"))
        assertTrue(error.message.contains("-v"))
        assertTrue(error.message.contains("verbose"))
        assertTrue(error.suggestion.isNotEmpty())
        assertNull(error.codeHint)
    }

    @Test
    fun `FlagDuplicateName has correct message and suggestion`() {
        val error = FlagDuplicateName("MyCmd", "execute", "force", listOf("f1", "f2"))
        assertTrue(error.message.contains("--force"))
        assertTrue(error.message.contains("f1"))
        assertTrue(error.suggestion.isNotEmpty())
        assertNull(error.codeHint)
    }

    @Test
    fun `PlayerOnlyOnConsoleOnly has correct message and suggestion`() {
        val error = PlayerOnlyOnConsoleOnly("MyCmd", "execute")
        assertTrue(error.message.contains("@PlayerOnly"))
        assertTrue(error.message.contains("@ConsoleOnly"))
        assertTrue(error.suggestion.isNotEmpty())
        assertNull(error.codeHint)
    }

    @Test
    fun `AsyncConfirmConflict has correct message and suggestion`() {
        val error = AsyncConfirmConflict("MyCmd", "execute")
        assertTrue(error.message.contains("@Async"))
        assertTrue(error.message.contains("@Confirm"))
        assertTrue(error.suggestion.isNotEmpty())
        assertNull(error.codeHint)
    }

    @Test
    fun `CooldownNonPositive has correct message and suggestion`() {
        val error = CooldownNonPositive("MyCmd", "execute", -5)
        assertTrue(error.message.contains("-5"))
        assertTrue(error.suggestion.contains("positive"))
        assertNotNull(error.codeHint)
    }

    @Test
    fun `CompleterNotFound has correct message and suggestion`() {
        val error = CompleterNotFound("MyCmd", "execute", "target", "MyCompleter")
        assertTrue(error.message.contains("MyCompleter"))
        assertTrue(error.suggestion.isNotEmpty())
        assertNotNull(error.codeHint)
    }

    @Test
    fun `all errors have non-empty message and suggestion`() {
        val errors: List<CommandDefinitionError> = listOf(
            MissingArgAnnotation("C", "m", "p"),
            MissingSenderParameter("C", "m"),
            MultipleSenderParameters("C", "m", listOf("a", "b")),
            InvalidSenderType("C", "m", "p", "String"),
            RequiredAfterOptional("C", "m", "r", "o"),
            GreedyNotLast("C", "m", "g", "a"),
            GreedyNotString("C", "m", "g", "Int"),
            NoResolver("C", "m", "p", "T"),
            AmbiguousOverload("C", listOf("h1", "h2"), 1),
            MissingCommandAnnotation("C"),
            NoHandlers("C"),
            DuplicateSubCommand("C", "sub", listOf("m1", "m2")),
            RangeOnNonNumeric("C", "m", "p", "String"),
            RangeInverted("C", "m", "p", 10.0, 0.0),
            FlagDuplicateShort("C", "m", 'v', listOf("a", "b")),
            FlagDuplicateName("C", "m", "flag", listOf("a", "b")),
            PlayerOnlyOnConsoleOnly("C", "m"),
            AsyncConfirmConflict("C", "m"),
            CooldownNonPositive("C", "m", 0),
            CompleterNotFound("C", "m", "p", "Comp")
        )

        for (error in errors) {
            assertTrue(error.message.isNotEmpty(), "${error::class.simpleName} has empty message")
            assertTrue(error.suggestion.isNotEmpty(), "${error::class.simpleName} has empty suggestion")
        }
    }
}
