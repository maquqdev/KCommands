package club.skidware.kcommands

import club.skidware.kcommands.argument.ArgumentException
import club.skidware.kcommands.argument.resolvers.*
import club.skidware.kcommands.execution.CommandContext
import org.bukkit.command.CommandSender
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.util.UUID

class ResolverTest {

    private fun mockContext(): CommandContext {
        val sender = java.lang.reflect.Proxy.newProxyInstance(
            Thread.currentThread().contextClassLoader,
            arrayOf(CommandSender::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "toString" -> "MockSender"
                "hashCode" -> 0
                "equals" -> false
                else -> null
            }
        } as CommandSender
        return CommandContext(sender, "test", arrayOf())
    }

    // --- StringResolver ---

    @Test
    fun `StringResolver resolve returns input as-is`() {
        val result = StringResolver.resolve("hello", mockContext())
        assertEquals("hello", result)
    }

    @Test
    fun `StringResolver resolve returns empty string`() {
        val result = StringResolver.resolve("", mockContext())
        assertEquals("", result)
    }

    // --- IntResolver ---

    @Test
    fun `IntResolver resolves valid integer`() {
        val result = IntResolver.resolve("42", mockContext())
        assertEquals(42, result)
    }

    @Test
    fun `IntResolver resolves negative integer`() {
        val result = IntResolver.resolve("-10", mockContext())
        assertEquals(-10, result)
    }

    @Test
    fun `IntResolver throws for invalid integer`() {
        val ex = assertThrows<ArgumentException> {
            IntResolver.resolve("abc", mockContext())
        }
        assertTrue(ex.message!!.contains("abc"))
    }

    @Test
    fun `IntResolver throws for decimal`() {
        assertThrows<ArgumentException> {
            IntResolver.resolve("3.14", mockContext())
        }
    }

    // --- LongResolver ---

    @Test
    fun `LongResolver resolves valid long`() {
        val result = LongResolver.resolve("9999999999", mockContext())
        assertEquals(9999999999L, result)
    }

    @Test
    fun `LongResolver throws for invalid long`() {
        val ex = assertThrows<ArgumentException> {
            LongResolver.resolve("xyz", mockContext())
        }
        assertTrue(ex.message!!.contains("xyz"))
    }

    // --- DoubleResolver ---

    @Test
    fun `DoubleResolver resolves valid double`() {
        val result = DoubleResolver.resolve("3.14", mockContext())
        assertEquals(3.14, result, 0.001)
    }

    @Test
    fun `DoubleResolver resolves integer as double`() {
        val result = DoubleResolver.resolve("42", mockContext())
        assertEquals(42.0, result, 0.001)
    }

    @Test
    fun `DoubleResolver throws for invalid double`() {
        val ex = assertThrows<ArgumentException> {
            DoubleResolver.resolve("notanumber", mockContext())
        }
        assertTrue(ex.message!!.contains("notanumber"))
    }

    // --- FloatResolver ---

    @Test
    fun `FloatResolver resolves valid float`() {
        val result = FloatResolver.resolve("1.5", mockContext())
        assertEquals(1.5f, result, 0.001f)
    }

    @Test
    fun `FloatResolver throws for invalid float`() {
        val ex = assertThrows<ArgumentException> {
            FloatResolver.resolve("abc", mockContext())
        }
        assertTrue(ex.message!!.contains("abc"))
    }

    // --- BooleanResolver ---

    @Test
    fun `BooleanResolver resolves true variants`() {
        val ctx = mockContext()
        assertTrue(BooleanResolver.resolve("true", ctx))
        assertTrue(BooleanResolver.resolve("yes", ctx))
        assertTrue(BooleanResolver.resolve("on", ctx))
        assertTrue(BooleanResolver.resolve("1", ctx))
    }

    @Test
    fun `BooleanResolver resolves false variants`() {
        val ctx = mockContext()
        assertFalse(BooleanResolver.resolve("false", ctx))
        assertFalse(BooleanResolver.resolve("no", ctx))
        assertFalse(BooleanResolver.resolve("off", ctx))
        assertFalse(BooleanResolver.resolve("0", ctx))
    }

    @Test
    fun `BooleanResolver is case insensitive`() {
        val ctx = mockContext()
        assertTrue(BooleanResolver.resolve("TRUE", ctx))
        assertTrue(BooleanResolver.resolve("Yes", ctx))
        assertFalse(BooleanResolver.resolve("FALSE", ctx))
        assertFalse(BooleanResolver.resolve("No", ctx))
    }

    @Test
    fun `BooleanResolver throws for invalid input`() {
        assertThrows<ArgumentException> {
            BooleanResolver.resolve("maybe", mockContext())
        }
    }

    @Test
    fun `BooleanResolver complete filters by prefix`() {
        val ctx = mockContext()
        val result = BooleanResolver.complete("t", ctx)
        assertEquals(listOf("true"), result)
    }

    @Test
    fun `BooleanResolver complete returns both for empty input`() {
        val ctx = mockContext()
        val result = BooleanResolver.complete("", ctx)
        assertEquals(listOf("true", "false"), result)
    }

    // --- DurationResolver ---

    @Test
    fun `DurationResolver resolves seconds`() {
        val result = DurationResolver.resolve("30s", mockContext())
        assertEquals(Duration.ofSeconds(30), result)
    }

    @Test
    fun `DurationResolver resolves minutes`() {
        val result = DurationResolver.resolve("5m", mockContext())
        assertEquals(Duration.ofMinutes(5), result)
    }

    @Test
    fun `DurationResolver resolves hours`() {
        val result = DurationResolver.resolve("2h", mockContext())
        assertEquals(Duration.ofHours(2), result)
    }

    @Test
    fun `DurationResolver resolves days`() {
        val result = DurationResolver.resolve("1d", mockContext())
        assertEquals(Duration.ofDays(1), result)
    }

    @Test
    fun `DurationResolver resolves compound duration`() {
        val result = DurationResolver.resolve("2h30m", mockContext())
        assertEquals(Duration.ofHours(2).plusMinutes(30), result)
    }

    @Test
    fun `DurationResolver throws for invalid duration`() {
        assertThrows<ArgumentException> {
            DurationResolver.resolve("invalid", mockContext())
        }
    }

    @Test
    fun `DurationResolver complete returns suggestions`() {
        val ctx = mockContext()
        val result = DurationResolver.complete("3", ctx)
        assertTrue(result.contains("30s"))
        assertTrue(result.contains("30m"))
        assertTrue(result.contains("30d"))
    }

    @Test
    fun `DurationResolver complete returns all for empty input`() {
        val ctx = mockContext()
        val result = DurationResolver.complete("", ctx)
        assertTrue(result.isNotEmpty())
    }

    // --- UUIDResolver ---

    @Test
    fun `UUIDResolver resolves valid UUID`() {
        val uuid = UUID.randomUUID()
        val result = UUIDResolver.resolve(uuid.toString(), mockContext())
        assertEquals(uuid, result)
    }

    @Test
    fun `UUIDResolver throws for invalid UUID`() {
        assertThrows<ArgumentException> {
            UUIDResolver.resolve("not-a-uuid", mockContext())
        }
    }

    // --- EnumResolver ---

    enum class Color { RED, GREEN, BLUE }

    @Test
    fun `EnumResolver resolves valid enum value`() {
        val resolver = EnumResolver(Color::class.java)
        val result = resolver.resolve("red", mockContext())
        assertEquals(Color.RED, result)
    }

    @Test
    fun `EnumResolver is case insensitive`() {
        val resolver = EnumResolver(Color::class.java)
        assertEquals(Color.GREEN, resolver.resolve("Green", mockContext()))
        assertEquals(Color.BLUE, resolver.resolve("BLUE", mockContext()))
    }

    @Test
    fun `EnumResolver throws for invalid enum value`() {
        val resolver = EnumResolver(Color::class.java)
        val ex = assertThrows<ArgumentException> {
            resolver.resolve("purple", mockContext())
        }
        assertTrue(ex.message!!.contains("purple"))
        assertTrue(ex.message!!.contains("Color"))
    }

    @Test
    fun `EnumResolver complete filters by prefix`() {
        val resolver = EnumResolver(Color::class.java)
        val result = resolver.complete("r", mockContext())
        assertEquals(listOf("red"), result)
    }

    @Test
    fun `EnumResolver complete returns all for empty input`() {
        val resolver = EnumResolver(Color::class.java)
        val result = resolver.complete("", mockContext())
        assertEquals(listOf("red", "green", "blue"), result)
    }
}
