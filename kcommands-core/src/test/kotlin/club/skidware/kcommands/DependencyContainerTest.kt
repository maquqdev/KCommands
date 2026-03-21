package club.skidware.kcommands

import club.skidware.kcommands.di.DependencyContainer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows

class DependencyContainerTest {

    private lateinit var container: DependencyContainer

    @BeforeEach
    fun setup() {
        container = DependencyContainer()
    }

    // Test helpers
    class SimpleService(val value: String = "default")
    class DependentService(val service: SimpleService)
    class MultiDepService(val service: SimpleService, val name: String)
    class NoConstructorArgs

    @Test
    fun `bind and get`() {
        val service = SimpleService("hello")
        container.bind(SimpleService::class, service)

        val retrieved = container.get(SimpleService::class)
        assertNotNull(retrieved)
        assertSame(service, retrieved)
        assertEquals("hello", retrieved!!.value)
    }

    @Test
    fun `get returns null for unbound type`() {
        val result = container.get(SimpleService::class)
        assertNull(result)
    }

    @Test
    fun `has returns true for bound type`() {
        container.bind(SimpleService::class, SimpleService())
        assertTrue(container.has(SimpleService::class))
    }

    @Test
    fun `has returns false for unbound type`() {
        assertFalse(container.has(SimpleService::class))
    }

    @Test
    fun `bind overwrites previous binding`() {
        val first = SimpleService("first")
        val second = SimpleService("second")

        container.bind(SimpleService::class, first)
        container.bind(SimpleService::class, second)

        val retrieved = container.get(SimpleService::class)
        assertSame(second, retrieved)
        assertEquals("second", retrieved!!.value)
    }

    @Test
    fun `createInstance with bound dependencies`() {
        val service = SimpleService("injected")
        container.bind(SimpleService::class, service)

        val dependent = container.createInstance(DependentService::class)
        assertNotNull(dependent)
        assertSame(service, dependent.service)
    }

    @Test
    fun `createInstance throws for missing binding`() {
        val ex = assertThrows<IllegalArgumentException> {
            container.createInstance(DependentService::class)
        }
        assertTrue(ex.message!!.contains("No binding"))
    }

    @Test
    fun `createInstance with multiple dependencies`() {
        container.bind(SimpleService::class, SimpleService("svc"))
        container.bind(String::class, "test-name")

        val instance = container.createInstance(MultiDepService::class)
        assertEquals("svc", instance.service.value)
        assertEquals("test-name", instance.name)
    }

    @Test
    fun `createInstance with no-arg constructor`() {
        val instance = container.createInstance(NoConstructorArgs::class)
        assertNotNull(instance)
    }

    @Test
    fun `multiple bindings of different types`() {
        container.bind(SimpleService::class, SimpleService("svc"))
        container.bind(String::class, "text")

        assertTrue(container.has(SimpleService::class))
        assertTrue(container.has(String::class))
        assertFalse(container.has(Int::class))

        assertEquals("text", container.get(String::class))
    }

    @Test
    fun `bind with interface type`() {
        container.bind(CharSequence::class, "hello" as CharSequence)
        val result = container.get(CharSequence::class)
        assertNotNull(result)
        assertEquals("hello", result.toString())
    }
}
