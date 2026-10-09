package ai.hev.app.data.local.prefs

import ai.hev.app.domain.provider.ProviderConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The configured providers as observable state, written through to [storage]. */
class ProviderStore(private val storage: ProviderStorage) {
    private val _providers = MutableStateFlow(storage.loadProviders())
    val providers: StateFlow<List<ProviderConfig>> = _providers.asStateFlow()

    private val _activeId = MutableStateFlow(storage.loadActiveId())
    val activeId: StateFlow<String?> = _activeId.asStateFlow()

    val activeProvider: ProviderConfig?
        get() {
            val id = _activeId.value
            return _providers.value.firstOrNull { it.id == id } ?: _providers.value.firstOrNull()
        }

    fun upsert(config: ProviderConfig) {
        val list = _providers.value.toMutableList()
        val idx = list.indexOfFirst { it.id == config.id }
        if (idx >= 0) list[idx] = config else list.add(config)
        persist(list)
        if (_activeId.value == null) setActive(config.id)
    }

    fun delete(id: String) {
        val list = _providers.value.filterNot { it.id == id }
        persist(list)
        if (_activeId.value == id) {
            setActive(list.firstOrNull()?.id)
        }
    }

    fun setActive(id: String?) {
        storage.saveActiveId(id)
        _activeId.value = id
    }

    private fun persist(list: List<ProviderConfig>) {
        storage.saveProviders(list)
        _providers.value = list
    }
}
