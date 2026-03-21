package club.skidware.kcommands

import club.skidware.kcommands.argument.ArgumentResolverRegistry
import club.skidware.kcommands.argument.resolvers.StringResolver
import club.skidware.kcommands.argument.resolvers.IntResolver
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class ArgumentResolverRegistryTest {
    private lateinit var registry: ArgumentResolverRegistry

    @BeforeEach
    fun setup() {
        registry = ArgumentResolverRegistry()
    }

    @Test
    fun `register and get resolver`() {
        registry.register(String::class, StringResolver)
        val resolver = registry.get(String::class)
        assertNotNull(resolver)
        assertEquals(StringResolver, resolver)
    }

    @Test
    fun `get returns null for unregistered type`() {
        val resolver = registry.get(String::class)
        assertNull(resolver)
    }

    @Test
    fun `has returns true for registered type`() {
        registry.register(Int::class, IntResolver)
        assertTrue(registry.has(Int::class))
    }

    @Test
    fun `has returns false for unregistered type`() {
        assertFalse(registry.has(Int::class))
    }

    @Test
    fun `getOrCreateEnum returns null for non-enum`() {
        assertNull(registry.getOrCreateEnum(String::class))
    }

    enum class TestEnum { A, B, C }

    @Test
    fun `getOrCreateEnum creates resolver for enum type`() {
        val resolver = registry.getOrCreateEnum(TestEnum::class)
        assertNotNull(resolver)
    }

    @Test
    fun `has returns true for enum type`() {
        assertTrue(registry.has(TestEnum::class))
    }

    @Test
    fun `register overwrites previous resolver for same type`() {
        registry.register(String::class, StringResolver)
        val resolver1 = registry.get(String::class)
        registry.register(String::class, StringResolver)
        val resolver2 = registry.get(String::class)
        assertNotNull(resolver1)
        assertNotNull(resolver2)
    }

    @Test
    fun `getOrCreateEnum caches resolver on second call`() {
        val resolver1 = registry.getOrCreateEnum(TestEnum::class)
        val resolver2 = registry.getOrCreateEnum(TestEnum::class)
        assertSame(resolver1, resolver2)
    }
}
