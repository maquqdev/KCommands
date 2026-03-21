package club.skidware.kcommands

import club.skidware.kcommands.argument.resolvers.StringResolver
import club.skidware.kcommands.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class CommandNodeTest {

    private fun makeExecutorInfo(vararg args: ArgInfo): ExecutorInfo {
        return ExecutorInfo(
            function = ::dummyFunction,
            instance = null,
            parameters = emptyList(),
            args = args.toList()
        )
    }

    private fun makeArg(name: String, required: Boolean = true, isGreedy: Boolean = false): ArgInfo {
        return ArgInfo(
            name = name,
            type = String::class,
            required = required,
            isGreedy = isGreedy,
            resolver = StringResolver
        )
    }

    @Test
    fun `CommandNode default values`() {
        val node = CommandNode(name = "test")
        assertEquals("test", node.name)
        assertNull(node.permission)
        assertNull(node.description)
        assertTrue(node.aliases.isEmpty())
        assertTrue(node.children.isEmpty())
        assertTrue(node.handlers.isEmpty())
    }

    @Test
    fun `CommandNode with all properties`() {
        val node = CommandNode(
            name = "test",
            aliases = listOf("t", "tst"),
            permission = "test.use",
            permissionMessage = "No access",
            description = "A test command"
        )
        assertEquals("test", node.name)
        assertEquals(listOf("t", "tst"), node.aliases)
        assertEquals("test.use", node.permission)
        assertEquals("No access", node.permissionMessage)
        assertEquals("A test command", node.description)
    }

    @Test
    fun `CommandNode children map operations`() {
        val parent = CommandNode(name = "parent")
        val child1 = CommandNode(name = "child1")
        val child2 = CommandNode(name = "child2")

        parent.children["child1"] = child1
        parent.children["child2"] = child2

        assertEquals(2, parent.children.size)
        assertEquals(child1, parent.children["child1"])
        assertEquals(child2, parent.children["child2"])
        assertNull(parent.children["nonexistent"])
    }

    @Test
    fun `CommandNode handlers can be populated`() {
        val node = CommandNode(name = "test")
        assertTrue(node.handlers.isEmpty())

        val executor = makeExecutorInfo(makeArg("arg1"))
        node.handlers.add(executor)

        assertTrue(node.handlers.isNotEmpty())
        assertEquals(1, node.handlers.all().size)
    }

    @Test
    fun `HandlerGroup add and all`() {
        val group = HandlerGroup()
        val exec1 = makeExecutorInfo(makeArg("a"))
        val exec2 = makeExecutorInfo(makeArg("b"), makeArg("c"))

        group.add(exec1)
        group.add(exec2)

        val all = group.all()
        assertEquals(2, all.size)
        assertTrue(all.contains(exec1))
        assertTrue(all.contains(exec2))
    }

    @Test
    fun `HandlerGroup isEmpty and isNotEmpty`() {
        val group = HandlerGroup()
        assertTrue(group.isEmpty())
        assertFalse(group.isNotEmpty())

        group.add(makeExecutorInfo())
        assertFalse(group.isEmpty())
        assertTrue(group.isNotEmpty())
    }

    @Test
    fun `HandlerGroup match selects handler by arg count`() {
        val group = HandlerGroup()
        val exec0 = makeExecutorInfo()
        val exec1 = makeExecutorInfo(makeArg("a"))
        val exec2 = makeExecutorInfo(makeArg("a"), makeArg("b"))

        group.add(exec0)
        group.add(exec1)
        group.add(exec2)

        assertEquals(exec0, group.match(0))
        assertEquals(exec1, group.match(1))
        assertEquals(exec2, group.match(2))
    }

    @Test
    fun `HandlerGroup match returns null when no handler matches`() {
        val group = HandlerGroup()
        val exec1 = makeExecutorInfo(makeArg("a"))

        group.add(exec1)

        assertNull(group.match(0))
        assertNull(group.match(2))
    }

    @Test
    fun `HandlerGroup match with optional args`() {
        val group = HandlerGroup()
        // handler with 1 required, 1 optional -> matches 1 or 2 args
        val exec = makeExecutorInfo(
            makeArg("required", required = true),
            makeArg("optional", required = false)
        )
        group.add(exec)

        assertEquals(exec, group.match(1))
        assertEquals(exec, group.match(2))
        assertNull(group.match(0))
        assertNull(group.match(3))
    }

    @Test
    fun `HandlerGroup match prefers handler with more args`() {
        val group = HandlerGroup()
        // handler1: 0 required, 1 optional -> matches 0 or 1
        val exec1 = makeExecutorInfo(makeArg("a", required = false))
        // handler2: 1 required, 1 optional -> matches 1 or 2
        val exec2 = makeExecutorInfo(makeArg("a", required = true), makeArg("b", required = false))

        group.add(exec1)
        group.add(exec2)

        // for 1 arg, handler2 should be preferred (sorted by descending args.size)
        assertEquals(exec2, group.match(1))
        // for 0 args, only handler1 matches
        assertEquals(exec1, group.match(0))
    }

    @Test
    fun `HandlerGroup all returns copy not reference`() {
        val group = HandlerGroup()
        val exec = makeExecutorInfo()
        group.add(exec)

        val list1 = group.all()
        val list2 = group.all()
        assertEquals(list1, list2)
    }

    @Test
    fun `HandlerGroup usageVariants returns arg lists`() {
        val group = HandlerGroup()
        val arg1 = makeArg("a")
        val arg2 = makeArg("b")
        val exec1 = makeExecutorInfo(arg1)
        val exec2 = makeExecutorInfo(arg1, arg2)

        group.add(exec1)
        group.add(exec2)

        val variants = group.usageVariants()
        assertEquals(2, variants.size)
        assertEquals(1, variants[0].size)
        assertEquals(2, variants[1].size)
    }

    companion object {
        @JvmStatic
        fun dummyFunction() {}
    }
}
