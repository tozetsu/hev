package ai.hev.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ai.hev.app.R
import ai.hev.app.data.repository.HevRepository
import ai.hev.app.domain.decision.DecisionDraft
import ai.hev.app.domain.decision.DecisionError
import ai.hev.app.domain.decision.DecisionItems
import ai.hev.app.domain.decision.DecisionKind
import ai.hev.app.domain.decision.ModelCapabilities
import ai.hev.app.domain.decision.parseStructuredOptionLines
import ai.hev.app.domain.provider.Endpoints
import ai.hev.app.domain.provider.ProviderConfig
import ai.hev.app.ui.common.message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val draft: DecisionDraft = DecisionDraft(
        kind = DecisionKind.Choice,
        instructions = "",
        context = "",
        items = DecisionItems.initial(DecisionKind.Choice),
    ),
    val providers: List<ProviderConfig> = emptyList(),
    val activeProvider: ProviderConfig? = null,
    val capabilities: ModelCapabilities = ModelCapabilities.Lenient,
    val loading: Boolean = false,
    val error: String? = null,
) {
    val itemRange: IntRange? get() = capabilities.itemRange(draft.kind)
    val canAddItem: Boolean get() = itemRange?.let { draft.items.size < it.last } ?: false
    val canRemoveItem: Boolean get() = itemRange?.let { draft.items.size > it.first } ?: false
}

private data class FormState(
    val draft: DecisionDraft,
    val loading: Boolean = false,
    val error: String? = null,
)

class HomeViewModel(
    application: Application,
    private val repo: HevRepository,
) : AndroidViewModel(application) {
    private val form = MutableStateFlow(FormState(HomeUiState().draft))

    val uiState: StateFlow<HomeUiState> = combine(
        form,
        repo.providers,
        repo.activeProviderId,
    ) { f, providers, activeId ->
        val active = providers.firstOrNull { it.id == activeId } ?: providers.firstOrNull()
        val capabilities = active?.capabilities ?: ModelCapabilities.Lenient
        HomeUiState(
            draft = f.draft.fittedTo(capabilities),
            providers = providers,
            activeProvider = active,
            capabilities = capabilities,
            loading = f.loading,
            error = f.error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private val context get() = getApplication<Application>()

    fun setKind(kind: DecisionKind) = editDraft { it.withKind(kind) }

    fun setContext(value: String) = editDraft { it.copy(context = value) }
    fun setInstructions(value: String) = editDraft { it.copy(instructions = value) }

    fun setItemLabel(id: String, label: String) = editDraft { draft ->
        draft.copy(items = draft.items.map { if (it.id == id) it.copy(label = label) else it })
    }

    fun addItem() {
        if (!uiState.value.canAddItem) return
        editDraft { it.copy(items = DecisionItems.added(it.kind, it.items)) }
    }

    fun removeItem(id: String) {
        if (!uiState.value.canRemoveItem) return
        editDraft { it.copy(items = DecisionItems.removed(it.kind, it.items, id)) }
    }

    /** Replaces the choice options with one option per line of [text]. */
    fun importChoiceOptions(text: String): Boolean {
        val range = uiState.value.capabilities.choiceOptions
        val labels = parseStructuredOptionLines(text, max = range.last)
        if (labels.size < range.first) {
            form.update { it.copy(error = context.getString(R.string.error_import_min, range.first)) }
            return false
        }
        editDraft { it.copy(items = DecisionItems.choiceOptions(labels)) }
        return true
    }

    fun selectProvider(id: String) = repo.setActiveProvider(id)

    fun submit(onSuccess: (Long) -> Unit) {
        val state = uiState.value
        val provider = state.activeProvider
        val error = when {
            provider == null -> context.getString(R.string.error_configure_provider)
            !Endpoints.isValid(provider.endpoint) -> context.getString(R.string.error_endpoint_invalid)
            provider.requiresApiKey && provider.apiKey.isBlank() -> context.getString(R.string.error_api_key)
            else -> state.draft.validate(state.capabilities)?.message(context)
        }
        if (error != null || provider == null) {
            form.update { it.copy(error = error) }
            return
        }
        val request = state.draft.toRequest(provider.model)
        form.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val failure = try {
                onSuccess(repo.decide(provider, request))
                null
            } catch (e: DecisionError) {
                e.message(context)
            }
            form.update { it.copy(loading = false, error = failure) }
        }
    }

    /** Edits the draft as shown, i.e. already fitted to the active model. */
    private fun editDraft(transform: (DecisionDraft) -> DecisionDraft) {
        val capabilities = uiState.value.capabilities
        form.update { it.copy(draft = transform(it.draft.fittedTo(capabilities)), error = null) }
    }

    companion object {
        fun factory(app: Application, repo: HevRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HomeViewModel(app, repo) as T
        }
    }
}

