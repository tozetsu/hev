package ai.hev.app.ui.settings

import ai.hev.app.data.local.prefs.SecretStorageException
import ai.hev.app.data.repository.HevRepository
import ai.hev.app.domain.provider.DecisionProtocol
import ai.hev.app.domain.provider.ProviderIssue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProviderEditViewModel(
    private val repo: HevRepository,
    providerId: String?,
) : ViewModel() {
    private val existing = providerId?.let { id -> repo.providers.value.firstOrNull { it.id == id } }

    private val _state = MutableStateFlow(
        existing?.let(ProviderEditState::of) ?: ProviderEditState(id = Uuid.random().toString(), isNew = true),
    )
    val state: StateFlow<ProviderEditState> = _state.asStateFlow()

    fun setName(value: String) = edit { it.copy(name = value) }
    fun setProtocol(value: DecisionProtocol) = edit { it.copy(protocol = value) }
    fun setEndpoint(value: String) = edit { it.copy(endpoint = value, fetchedModels = emptyList()) }
    fun setApiKey(value: String) = edit { it.copy(apiKey = value) }
    fun setModel(value: String) = edit { it.copy(model = value) }

    private var modelsRequest: Pair<String, String>? = null
    private var modelsJob: Job? = null

    /** Loads the vendor's model list once per endpoint and key; without one, the model is typed in. */
    fun loadModels() {
        val current = _state.value
        val request = current.endpoint.trim() to current.apiKey.trim()
        if (request == modelsRequest) return
        modelsRequest = request
        modelsJob?.cancel()
        modelsJob = viewModelScope.launch {
            val models = repo.listModels(request.first, request.second)
            _state.update { it.copy(fetchedModels = models) }
            if (models.isEmpty()) modelsRequest = null
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
        return persist {
            repo.upsertProvider(config)
            if (current.isNew || repo.activeProvider() == null) repo.setActiveProvider(config.id)
        }
    }

    /** Deletes and returns true, or shows why the keys could not be updated and returns false. */
    fun delete(): Boolean = persist { existing?.let { repo.deleteProvider(it.id) } }

    private inline fun persist(write: () -> Unit): Boolean = try {
        write()
        true
    } catch (_: SecretStorageException) {
        _state.update { it.copy(error = ProviderIssue.KeyringLocked) }
        false
    }

    private fun edit(transform: (ProviderEditState) -> ProviderEditState) =
        _state.update { transform(it).copy(error = null) }
}
