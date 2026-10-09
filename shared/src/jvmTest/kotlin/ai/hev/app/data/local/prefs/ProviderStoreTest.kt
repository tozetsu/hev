package ai.hev.app.data.local.prefs

import ai.hev.app.domain.provider.DecisionProtocol
import ai.hev.app.domain.provider.ProviderConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProviderStoreTest {
    private class MemoryStorage : ProviderStorage {
        var providers = emptyList<ProviderConfig>()
        var activeId: String? = null
        override fun loadProviders() = providers
        override fun saveProviders(providers: List<ProviderConfig>) { this.providers = providers }
        override fun loadActiveId() = activeId
        override fun saveActiveId(id: String?) { activeId = id }
    }

    private val first = ProviderConfig("first", "First", DecisionProtocol.SystemOne, "https://x.example/v1/systemone", "", "m")

    @Test
    fun `starts empty and the first provider becomes active`() {
        val storage = MemoryStorage()
        val store = ProviderStore(storage)
        assertNull(store.activeProvider)

        store.upsert(first)

        assertEquals("first", storage.activeId)
        assertEquals(storage.providers, ProviderStore(storage).providers.value)
    }

    @Test
    fun `deleting the active provider activates the next one`() {
        val storage = MemoryStorage()
        val store = ProviderStore(storage)
        store.upsert(first)
        val second = first.copy(id = "second", name = "Second")
        store.upsert(second)

        store.delete(first.id)
        assertEquals("second", storage.activeId)
        store.delete("second")
        assertNull(store.activeProvider)
        assertNull(storage.activeId)
    }
}
