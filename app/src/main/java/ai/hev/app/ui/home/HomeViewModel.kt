package ai.hev.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ai.hev.app.R
import ai.hev.app.data.remote.ApiException
import ai.hev.app.data.repository.HevRepository
import ai.hev.app.domain.choice.MAX_CHOICE_OPTIONS
import ai.hev.app.domain.choice.MAX_SCORE_LEVELS
import ai.hev.app.domain.choice.MIN_CHOICE_OPTIONS
import ai.hev.app.domain.choice.MIN_SCORE_LEVELS
import ai.hev.app.domain.choice.nextChoiceOptionId
import ai.hev.app.domain.choice.optionsFromLabels
import ai.hev.app.domain.choice.parseStructuredOptionLines
import ai.hev.app.domain.model.ChoiceOption
import ai.hev.app.domain.model.ProviderConfig
import ai.hev.app.domain.model.QuestionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val questionType: QuestionType = QuestionType.Choice,
    val stateText: String = "",
    val question: String = "",
    val options: List<ChoiceOption> = defaultChoiceOptions(),
    val providers: List<ProviderConfig> = emptyList(),
    val activeProvider: ProviderConfig? = null,
    val loading: Boolean = false,
    val error: String? = null,
)

private fun defaultChoiceOptions() = listOf(
    ChoiceOption(id = "a", label = ""),
    ChoiceOption(id = "b", label = ""),
)

private fun defaultScoreLevels() = listOf(
    ChoiceOption(id = "0", label = ""),
    ChoiceOption(id = "1", label = ""),
)

class HomeViewModel(
    application: Application,
    private val repo: HevRepository,
) : AndroidViewModel(application) {
    private val form = MutableStateFlow(HomeUiState())

    val uiState: StateFlow<HomeUiState> = combine(
        form,
        repo.providers,
        repo.activeProviderId,
    ) { f, providers, activeId ->
        f.copy(
            providers = providers,
            activeProvider = providers.firstOrNull { it.id == activeId } ?: providers.firstOrNull(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun str(resId: Int): String = getApplication<Application>().getString(resId)
    private fun str(resId: Int, vararg args: Any): String =
        getApplication<Application>().getString(resId, *args)

    fun setQuestionType(type: QuestionType) = form.update { s ->
        if (s.questionType == type) return@update s
        s.copy(
            questionType = type,
            options = when (type) {
                QuestionType.Choice -> defaultChoiceOptions()
                QuestionType.Score -> defaultScoreLevels()
                QuestionType.Noul -> emptyList()
            },
            error = null,
        )
    }

    fun setStateText(v: String) = form.update { it.copy(stateText = v, error = null) }
    fun setQuestion(v: String) = form.update { it.copy(question = v, error = null) }

    fun setOptionLabel(id: String, label: String) = form.update { s ->
        s.copy(options = s.options.map { if (it.id == id) it.copy(label = label) else it }, error = null)
    }

    fun addOption() = form.update { s ->
        when (s.questionType) {
            QuestionType.Choice -> {
                if (s.options.size >= MAX_CHOICE_OPTIONS) return@update s
                val next = nextChoiceOptionId(s.options.map { it.id }.toSet())
                s.copy(options = s.options + ChoiceOption(id = next, label = ""))
            }
            QuestionType.Score -> {
                if (s.options.size >= MAX_SCORE_LEVELS) return@update s
                val nextId = s.options.size.toString()
                s.copy(options = s.options + ChoiceOption(id = nextId, label = ""))
            }
            QuestionType.Noul -> s
        }
    }

    fun removeOption(id: String) = form.update { s ->
        val minSize = when (s.questionType) {
            QuestionType.Choice -> MIN_CHOICE_OPTIONS
            QuestionType.Score -> MIN_SCORE_LEVELS
            QuestionType.Noul -> 0
        }
        if (s.options.size <= minSize) return@update s
        val filtered = s.options.filterNot { it.id == id }
        val renumbered = if (s.questionType == QuestionType.Score) {
            filtered.mapIndexed { index, opt -> opt.copy(id = index.toString()) }
        } else {
            filtered
        }
        s.copy(options = renumbered)
    }

    /** Replace current choice options from structured text (clear + fill). */
    fun importChoiceOptions(text: String): Boolean {
        if (form.value.questionType != QuestionType.Choice) return false
        val labels = parseStructuredOptionLines(text, MAX_CHOICE_OPTIONS)
        if (labels.size < MIN_CHOICE_OPTIONS) {
            form.update { it.copy(error = str(R.string.error_import_min)) }
            return false
        }
        form.update {
            it.copy(options = optionsFromLabels(labels), error = null)
        }
        return true
    }

    fun selectProvider(id: String) {
        repo.setActiveProvider(id)
    }

    fun submit(onSuccess: (Long) -> Unit) {
        val s = uiState.value
        val provider = s.activeProvider ?: repo.activeProvider()
        val baseError = when {
            provider == null -> str(R.string.error_configure_provider)
            provider.apiKey.isBlank() -> str(R.string.error_api_key)
            provider.baseUrl.isBlank() -> str(R.string.error_endpoint)
            s.question.isBlank() -> str(R.string.error_question)
            else -> null
        }
        if (baseError != null) {
            form.update { it.copy(error = baseError) }
            return
        }
        val typeError = when (s.questionType) {
            QuestionType.Choice -> when {
                s.options.size !in MIN_CHOICE_OPTIONS..MAX_CHOICE_OPTIONS ->
                    str(R.string.error_options_range)
                s.options.any { it.label.isBlank() } -> str(R.string.error_options_empty)
                else -> null
            }
            QuestionType.Score -> when {
                s.options.size !in MIN_SCORE_LEVELS..MAX_SCORE_LEVELS ->
                    str(R.string.error_levels_range)
                s.options.any { it.label.isBlank() } -> str(R.string.error_levels_empty)
                else -> null
            }
            QuestionType.Noul -> null
        }
        if (typeError != null) {
            form.update { it.copy(error = typeError) }
            return
        }

        val p = provider!!
        form.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val (_, id) = when (s.questionType) {
                    QuestionType.Choice -> repo.runChoice(
                        question = s.question.trim(),
                        options = s.options,
                        state = s.stateText.trim().ifBlank { null },
                        provider = p,
                    )
                    QuestionType.Score -> repo.runScore(
                        question = s.question.trim(),
                        levels = s.options.map { it.label },
                        state = s.stateText.trim().ifBlank { null },
                        provider = p,
                    )
                    QuestionType.Noul -> repo.runNoul(
                        question = s.question.trim(),
                        state = s.stateText.trim().ifBlank { null },
                        provider = p,
                    )
                }
                form.update { it.copy(loading = false) }
                onSuccess(id)
            } catch (e: ApiException) {
                form.update {
                    it.copy(
                        loading = false,
                        error = str(R.string.error_request_failed, e.code, e.message ?: ""),
                    )
                }
            } catch (e: Exception) {
                form.update {
                    it.copy(loading = false, error = e.message ?: str(R.string.error_network))
                }
            }
        }
    }

    companion object {
        fun factory(app: Application, repo: HevRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HomeViewModel(app, repo) as T
        }
    }
}
