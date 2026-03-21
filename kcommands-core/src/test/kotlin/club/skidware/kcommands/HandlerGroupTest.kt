package club.skidware.kcommands

import club.skidware.kcommands.argument.resolvers.StringResolver
import club.skidware.kcommands.model.ArgInfo
import club.skidware.kcommands.model.ExecutorInfo
import club.skidware.kcommands.model.HandlerGroup
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class HandlerGroupTest {

    private lateinit var group: HandlerGroup

    @BeforeEach
    fun setup() {
        group = HandlerGroup()
    }

    private fun makeArg(name: String, required: Boolean = true): ArgInfo {
        return ArgInfo(
            name = name,
            type = String::class,
            required = required,
            resolver = StringResolver
        )
    }

    private fun makeExecutor(vararg args: ArgInfo): ExecutorInfo {
        return ExecutorInfo(
            function = CommandNodeTest::dummyFunction,
            instance = null,
            parameters = emptyList(),
            args = args.toList()
        )
    }

    @Test
    fun `match handler with exact arg count`() {
        val exec2 = makeExecutor(makeArg("a"), makeArg("b"))
        group.add(exec2)

        assertEquals(exec2, group.match(2))
    }

    @Test
    fun `match handler with zero args`() {
        val exec0 = makeExecutor()
        group.add(exec0)

        assertEquals(exec0, group.match(0))
        assertNull(group.match(1))
    }

    @Test
    fun `match handler with optional args fewer than total`() {
        // 2 required, 1 optional -> matches 2 or 3
        val exec = makeExecutor(
            makeArg("a", required = true),
            makeArg("b", required = true),
            makeArg("c", required = false)
        )
        group.add(exec)

        assertEquals(exec, group.match(2))
        assertEquals(exec, group.match(3))
        assertNull(group.match(1))
        assertNull(group.match(4))
    }

    @Test
    fun `match prefers handler with more args when multiple match`() {
        // exec1: 0 args (matches 0)
        val exec1 = makeExecutor()
        // exec2: 1 required, 1 optional (matches 1 or 2)
        val exec2 = makeExecutor(
            makeArg("a", required = true),
            makeArg("b", required = false)
        )
        // exec3: 2 required, 1 optional (matches 2 or 3)
        val exec3 = makeExecutor(
            makeArg("a", required = true),
            makeArg("b", required = true),
            makeArg("c", required = false)
        )

        group.add(exec1)
        group.add(exec2)
        group.add(exec3)

        // 0 args -> only exec1 matches
        assertEquals(exec1, group.match(0))
        // 1 arg -> exec2 matches
        assertEquals(exec2, group.match(1))
        // 2 args -> exec3 has more args.size and matches, so preferred over exec2
        assertEquals(exec3, group.match(2))
        // 3 args -> only exec3 matches
        assertEquals(exec3, group.match(3))
    }

    @Test
    fun `match returns null when arg count doesn't match any handler`() {
        val exec = makeExecutor(makeArg("a"))
        group.add(exec)

        assertNull(group.match(0))
        assertNull(group.match(2))
        assertNull(group.match(5))
    }

    @Test
    fun `match returns null on empty group`() {
        assertNull(group.match(0))
        assertNull(group.match(1))
    }

    @Test
    fun `match with all optional args`() {
        // 0 required, 2 optional -> matches 0, 1, or 2
        val exec = makeExecutor(
            makeArg("a", required = false),
            makeArg("b", required = false)
        )
        group.add(exec)

        assertEquals(exec, group.match(0))
        assertEquals(exec, group.match(1))
        assertEquals(exec, group.match(2))
        assertNull(group.match(3))
    }

    @Test
    fun `match with single required arg`() {
        val exec = makeExecutor(makeArg("target"))
        group.add(exec)

        assertNull(group.match(0))
        assertEquals(exec, group.match(1))
        assertNull(group.match(2))
    }

    @Test
    fun `multiple handlers with overlapping ranges prefer larger`() {
        // exec1: 1 required, 2 optional -> matches 1..3
        val exec1 = makeExecutor(
            makeArg("a", required = true),
            makeArg("b", required = false),
            makeArg("c", required = false)
        )
        // exec2: 1 required only -> matches 1
        val exec2 = makeExecutor(
            makeArg("x", required = true)
        )

        group.add(exec1)
        group.add(exec2)

        // 1 arg: exec1 is preferred because it has more args (sorted descending by size)
        assertEquals(exec1, group.match(1))
        // 2 args: only exec1 matches
        assertEquals(exec1, group.match(2))
    }
}
