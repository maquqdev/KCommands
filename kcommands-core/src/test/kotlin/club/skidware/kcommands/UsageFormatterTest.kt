package club.skidware.kcommands

import club.skidware.kcommands.argument.resolvers.StringResolver
import club.skidware.kcommands.help.UsageFormatter
import club.skidware.kcommands.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class UsageFormatterTest {

    private fun makeArg(name: String, required: Boolean = true, isGreedy: Boolean = false): ArgInfo {
        return ArgInfo(
            name = name,
            type = String::class,
            required = required,
            isGreedy = isGreedy,
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

    private fun makeNode(name: String, vararg executors: ExecutorInfo): CommandNode {
        val node = CommandNode(name = name)
        executors.forEach { node.handlers.add(it) }
        return node
    }

    @Test
    fun `format single handler with required args`() {
        val exec = makeExecutor(makeArg("arg1"), makeArg("arg2"))
        val node = makeNode("test", exec)

        val result = UsageFormatter.format(node, exec)
        assertEquals("/test <arg1> <arg2>", result)
    }

    @Test
    fun `format handler with optional args`() {
        val exec = makeExecutor(makeArg("req"), makeArg("opt", required = false))
        val node = makeNode("test", exec)

        val result = UsageFormatter.format(node, exec)
        assertEquals("/test <req> [opt]", result)
    }

    @Test
    fun `format handler with greedy arg`() {
        val exec = makeExecutor(makeArg("msg", isGreedy = true))
        val node = makeNode("test", exec)

        val result = UsageFormatter.format(node, exec)
        assertEquals("/test <msg...>", result)
    }

    @Test
    fun `format handler with no args`() {
        val exec = makeExecutor()
        val node = makeNode("test", exec)

        val result = UsageFormatter.format(node, exec)
        assertEquals("/test", result)
    }

    @Test
    fun `format with parent path`() {
        val exec = makeExecutor(makeArg("target"))
        val node = makeNode("sub", exec)

        val result = UsageFormatter.format(node, exec, "cmd")
        assertEquals("/cmd sub <target>", result)
    }

    @Test
    fun `formatAll includes all handlers`() {
        val exec1 = makeExecutor(makeArg("a"))
        val exec2 = makeExecutor(makeArg("a"), makeArg("b"))
        val node = makeNode("test", exec1, exec2)

        val result = UsageFormatter.formatAll(node)
        val lines = result.lines()
        assertEquals(2, lines.size)
        assertTrue(lines[0].contains("<a>"))
        assertTrue(lines[1].contains("<a> <b>"))
    }

    @Test
    fun `formatAll includes children`() {
        val childExec = makeExecutor(makeArg("target"))
        val child = makeNode("sub", childExec)

        val parent = CommandNode(name = "cmd")
        parent.children["sub"] = child

        val result = UsageFormatter.formatAll(parent)
        assertTrue(result.contains("/cmd sub <target>"))
    }

    @Test
    fun `formatBrief for no handlers with children lists subcommands`() {
        val child1 = CommandNode(name = "add")
        val child2 = CommandNode(name = "remove")

        val parent = CommandNode(name = "cmd")
        parent.children["add"] = child1
        parent.children["remove"] = child2

        val result = UsageFormatter.formatBrief(parent)
        assertEquals("/cmd <add|remove>", result)
    }

    @Test
    fun `formatBrief for single handler`() {
        val exec = makeExecutor(makeArg("player"))
        val node = makeNode("ban", exec)

        val result = UsageFormatter.formatBrief(node)
        assertEquals("/ban <player>", result)
    }

    @Test
    fun `formatBrief for multiple handler variants`() {
        val exec1 = makeExecutor(makeArg("a"))
        val exec2 = makeExecutor(makeArg("a"), makeArg("b"))
        val node = makeNode("cmd", exec1, exec2)

        val result = UsageFormatter.formatBrief(node)
        assertTrue(result.contains("<a>"))
        assertTrue(result.contains("|"))
        assertTrue(result.contains("<b>"))
    }

    @Test
    fun `format optional greedy arg`() {
        val exec = makeExecutor(makeArg("msg", required = false, isGreedy = true))
        val node = makeNode("say", exec)

        val result = UsageFormatter.format(node, exec)
        assertEquals("/say [msg...]", result)
    }

    @Test
    fun `formatUsageLine delegates to format`() {
        val exec = makeExecutor(makeArg("target"))
        val node = makeNode("kick", exec)

        val result = UsageFormatter.formatUsageLine(node, exec)
        assertEquals("/kick <target>", result)
    }
}
