package ai.hev.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ai.hev.app.data.repository.HevRepository
import ai.hev.app.domain.decision.DecisionError
import ai.hev.app.domain.provider.DecisionProtocol
import ai.hev.app.domain.provider.ProviderPreset
import ai.hev.app.domain.provider.ProviderPresets
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

class ProviderEditViewModel(
    private val repo: HevRepository,
    providerId: String?,
) : ViewModel() {
    private val existing = providerId?.let { id -> repo.providers.value.firstOrNull { it.id == id } }

    private val _state = MutableStateFlow(
        ProviderEditState.of(
            config = existing ?: ProviderPresets.TypeSafe.newProvider(Uuid.random().toString()),
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

    private var modelsRequest: List<String>? = null
    private var modelsJob: Job? = null

    /** Loads the vendor's model list once per URL and key; failures leave manual entry. */
    fun loadModels() {
        val current = _state.value
        if (current.modelsUrl.isBlank()) return
        val request = listOf(current.modelsUrl.trim(), current.endpoint.trim(), current.apiKey.trim())
        if (request == modelsRequest) return
        modelsRequest = request
        modelsJob?.cancel()
        modelsJob = viewModelScope.launch {
            val (modelsUrl, endpoint, apiKey) = request
            try {
                val models = repo.listModels(modelsUrl, endpoint, apiKey)
                _state.update { it.copy(fetchedModels = models) }
            } catch (_: DecisionError) {
                modelsRequest = null
            }
        }
    }

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
