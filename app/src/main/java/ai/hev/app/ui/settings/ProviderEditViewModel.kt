package ai.hev.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ai.hev.app.data.repository.HevRepository
import ai.hev.app.domain.provider.DecisionProtocol
import ai.hev.app.domain.provider.ProviderPreset
import ai.hev.app.domain.provider.ProviderPresets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class ProviderEditViewModel(
    private val repo: HevRepository,
    providerId: String?,
) : ViewModel() {
    private val existing = providerId?.let { id -> repo.providers.value.firstOrNull { it.id == id } }

    private val _state = MutableStateFlow(
        ProviderEditState.of(
            config = existing ?: ProviderPresets.TypeSafe.newProvider(UUID.randomUUID().toString()),
            isNew = existing == null,
        ),
    )
    val state: StateFlow<ProviderEditState> = _state.asStateFlow()

    fun selectPreset(preset: ProviderPreset?) = edit { it.withPreset(preset) }

    fun setName(value: String) = edit { it.copy(name = value) }
    fun setProtocol(value: DecisionProtocol) = edit { it.copy(protocol = value) }
    fun setEndpoint(value: String) = edit { it.copy(endpoint = value) }
    fun setModelsUrl(value: String) = edit { it.copy(modelsUrl = value) }
    fun setApiKey(value: String) = edit { it.copy(apiKey = value) }
    fun setModel(value: String) = edit { it.copy(model = value) }

    /** Saves and returns true, or shows the first problem and returns false. */
    fun save(): Boolean {
        val current = _state.value
        val error = current.validate()
        if (error != null) {
            _state.update { it.copy(error = error) }
            return false
        }
        val config = current.toConfig()
        repo.upsertProvider(config)
        if (current.isNew || repo.activeProvider() == null) repo.setActiveProvider(config.id)
        return true
    }

    fun delete() {
        existing?.let { repo.deleteProvider(it.id) }
    }

    private fun edit(transform: (ProviderEditState) -> ProviderEditState) =
        _state.update { transform(it).copy(error = null) }

    companion object {
        fun factory(repo: HevRepository, providerId: String?) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ProviderEditViewModel(repo, providerId) as T
        }
    }
}
