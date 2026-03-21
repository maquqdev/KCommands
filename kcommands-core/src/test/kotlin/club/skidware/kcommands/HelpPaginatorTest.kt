package club.skidware.kcommands

import club.skidware.kcommands.help.HelpPaginator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class HelpPaginatorTest {

    @Test
    fun `single page with fewer items than pageSize`() {
        val paginator = HelpPaginator(listOf("a", "b", "c"), pageSize = 10)
        assertEquals(1, paginator.totalPages)
        assertEquals(listOf("a", "b", "c"), paginator.getPage(1))
    }

    @Test
    fun `single page with exact pageSize items`() {
        val items = (1..5).map { "item$it" }
        val paginator = HelpPaginator(items, pageSize = 5)
        assertEquals(1, paginator.totalPages)
        assertEquals(items, paginator.getPage(1))
    }

    @Test
    fun `multiple pages`() {
        val items = (1..25).map { "item$it" }
        val paginator = HelpPaginator(items, pageSize = 10)
        assertEquals(3, paginator.totalPages)
    }

    @Test
    fun `getPage returns correct items for first page`() {
        val items = (1..15).map { "item$it" }
        val paginator = HelpPaginator(items, pageSize = 5)

        val page1 = paginator.getPage(1)
        assertEquals(5, page1.size)
        assertEquals("item1", page1[0])
        assertEquals("item5", page1[4])
    }

    @Test
    fun `getPage returns correct items for middle page`() {
        val items = (1..15).map { "item$it" }
        val paginator = HelpPaginator(items, pageSize = 5)

        val page2 = paginator.getPage(2)
        assertEquals(5, page2.size)
        assertEquals("item6", page2[0])
        assertEquals("item10", page2[4])
    }

    @Test
    fun `getPage returns correct items for last page`() {
        val items = (1..12).map { "item$it" }
        val paginator = HelpPaginator(items, pageSize = 5)

        val page3 = paginator.getPage(3)
        assertEquals(2, page3.size)
        assertEquals("item11", page3[0])
        assertEquals("item12", page3[1])
    }

    @Test
    fun `totalPages calculation with exact division`() {
        val items = (1..20).map { "item$it" }
        val paginator = HelpPaginator(items, pageSize = 10)
        assertEquals(2, paginator.totalPages)
    }

    @Test
    fun `totalPages calculation with remainder`() {
        val items = (1..21).map { "item$it" }
        val paginator = HelpPaginator(items, pageSize = 10)
        assertEquals(3, paginator.totalPages)
    }

    @Test
    fun `totalPages is 1 for empty list`() {
        val paginator = HelpPaginator(emptyList<String>(), pageSize = 10)
        assertEquals(1, paginator.totalPages)
    }

    @Test
    fun `out-of-range page below 1 clamps to first page`() {
        val items = (1..10).map { "item$it" }
        val paginator = HelpPaginator(items, pageSize = 5)

        val page = paginator.getPage(0)
        assertEquals(paginator.getPage(1), page)
    }

    @Test
    fun `out-of-range page above max clamps to last page`() {
        val items = (1..10).map { "item$it" }
        val paginator = HelpPaginator(items, pageSize = 5)

        val page = paginator.getPage(99)
        assertEquals(paginator.getPage(2), page)
    }

    @Test
    fun `hasPage returns true for valid pages`() {
        val items = (1..15).map { "item$it" }
        val paginator = HelpPaginator(items, pageSize = 5)

        assertTrue(paginator.hasPage(1))
        assertTrue(paginator.hasPage(2))
        assertTrue(paginator.hasPage(3))
    }

    @Test
    fun `hasPage returns false for invalid pages`() {
        val items = (1..15).map { "item$it" }
        val paginator = HelpPaginator(items, pageSize = 5)

        assertFalse(paginator.hasPage(0))
        assertFalse(paginator.hasPage(4))
        assertFalse(paginator.hasPage(-1))
    }

    @Test
    fun `getPage on empty list returns empty`() {
        val paginator = HelpPaginator(emptyList<String>(), pageSize = 10)
        val page = paginator.getPage(1)
        assertTrue(page.isEmpty())
    }

    @Test
    fun `single item single page`() {
        val paginator = HelpPaginator(listOf("only"), pageSize = 10)
        assertEquals(1, paginator.totalPages)
        assertEquals(listOf("only"), paginator.getPage(1))
    }

    @Test
    fun `pageSize of 1`() {
        val items = listOf("a", "b", "c")
        val paginator = HelpPaginator(items, pageSize = 1)

        assertEquals(3, paginator.totalPages)
        assertEquals(listOf("a"), paginator.getPage(1))
        assertEquals(listOf("b"), paginator.getPage(2))
        assertEquals(listOf("c"), paginator.getPage(3))
    }
}
