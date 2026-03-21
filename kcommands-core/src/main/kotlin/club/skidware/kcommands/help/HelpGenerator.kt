package club.skidware.kcommands.help

import club.skidware.kcommands.message.DefaultMessageProvider
import club.skidware.kcommands.message.IMessageProvider
import club.skidware.kcommands.model.CommandNode
import club.skidware.kcommands.model.ExecutorInfo
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.CommandSender

/**
 * Generates paginated help output for a command tree.
 *
 * Collects all visible handlers (respecting permissions) and formats them as
 * MiniMessage [Component] lines with header, entries, and optional page footer.
 *
 * @param messageProvider the message provider for help templates
 * @param pageSize the maximum number of entries per page
 */
class HelpGenerator(
    private val messageProvider: IMessageProvider = DefaultMessageProvider(),
    private val pageSize: Int = 10
) {

    private val mm = MiniMessage.miniMessage()

    data class HelpEntry(
        val usage: String,
        val description: String,
        val permission: String?
    )

    /**
     * Generates help output for the given command [node], filtered by the [sender]'s permissions.
     *
     * @param sender the command sender (used for permission filtering)
     * @param node the root command node
     * @param page the page number to display (1-based)
     * @return a list of formatted [Component] lines ready to send to the sender
     */
    fun generate(sender: CommandSender, node: CommandNode, page: Int = 1): List<Component> {
        val entries = collectEntries(sender, node, "")
        val paginator = HelpPaginator(entries, pageSize)
        val currentPage = page.coerceIn(1, paginator.totalPages)
        val pageEntries = paginator.getPage(currentPage)

        val lines = mutableListOf<Component>()

        // Header
        lines.add(mm.deserialize(
            messageProvider.helpHeader.replace("{COMMAND}", node.name)
        ))

        // Entries
        for (entry in pageEntries) {
            val line = messageProvider.helpEntry
                .replace("{USAGE}", entry.usage)
                .replace("{DESCRIPTION}", entry.description)
            lines.add(mm.deserialize(line))
        }

        // Footer
        if (paginator.totalPages > 1) {
            lines.add(mm.deserialize(
                messageProvider.helpFooter
                    .replace("{PAGE}", currentPage.toString())
                    .replace("{PAGES}", paginator.totalPages.toString())
            ))
        }

        return lines
    }

    private fun collectEntries(
        sender: CommandSender,
        node: CommandNode,
        parentPath: String
    ): List<HelpEntry> {
        val entries = mutableListOf<HelpEntry>()

        // Check node-level permission
        if (node.permission != null && !sender.hasPermission(node.permission!!)) {
            return entries
        }

        // Handlers on this node
        for (handler in node.handlers.all()) {
            if (handler.permission != null && !sender.hasPermission(handler.permission!!)) {
                continue
            }
            entries.add(HelpEntry(
                usage = UsageFormatter.format(node, handler, parentPath),
                description = handler.description ?: node.description ?: "",
                permission = handler.permission ?: node.permission
            ))
        }

        // Child nodes
        val path = if (parentPath.isBlank()) node.name else "$parentPath ${node.name}"
        for ((_, child) in node.children.toSortedMap()) {
            entries.addAll(collectEntries(sender, child, path))
        }

        return entries
    }
}
