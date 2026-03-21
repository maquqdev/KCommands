package club.skidware.kcommands.help

/**
 * Generic paginator that splits a list of entries into fixed-size pages.
 *
 * @param T the type of entries being paginated
 * @param entries the full list of entries
 * @param pageSize the maximum number of entries per page
 */
class HelpPaginator<T>(
    private val entries: List<T>,
    private val pageSize: Int = 10
) {
    /** The total number of pages (always at least 1). */
    val totalPages: Int
        get() = if (entries.isEmpty()) 1 else (entries.size + pageSize - 1) / pageSize

    /**
     * Returns the entries for the given [page] number (1-based, clamped to valid range).
     *
     * @param page the page number to retrieve
     * @return the entries on that page
     */
    fun getPage(page: Int): List<T> {
        val clamped = page.coerceIn(1, totalPages)
        val start = (clamped - 1) * pageSize
        val end = (start + pageSize).coerceAtMost(entries.size)
        return if (start < entries.size) entries.subList(start, end) else emptyList()
    }

    /** Returns true if the given [page] number is within the valid range. */
    fun hasPage(page: Int): Boolean = page in 1..totalPages
}
