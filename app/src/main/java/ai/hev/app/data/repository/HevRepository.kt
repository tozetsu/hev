package ai.hev.app.data.repository

import ai.hev.app.data.local.db.HistoryDao
import ai.hev.app.data.local.db.toDomain
import ai.hev.app.data.local.db.toEntity
import ai.hev.app.data.local.prefs.ProviderStore
import ai.hev.app.data.remote.JevApiClient
import ai.hev.app.domain.model.ChoiceOption
import ai.hev.app.domain.model.ChoiceRequest
import ai.hev.app.domain.model.DecideResult
import ai.hev.app.domain.model.HistoryEntry
import ai.hev.app.domain.model.ModelInfo
import ai.hev.app.domain.model.NoulRequest
import ai.hev.app.domain.model.ProviderConfig
import ai.hev.app.domain.model.QuestionType
import ai.hev.app.domain.model.ScoreRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class HevRepository(
    private val api: JevApiClient,
    private val historyDao: HistoryDao,
    private val providerStore: ProviderStore,
) {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    val providers = providerStore.providers
    val activeProviderId = providerStore.activeId
    val history: Flow<List<HistoryEntry>> =
        historyDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun activeProvider(): ProviderConfig? = providerStore.activeProvider

    fun upsertProvider(config: ProviderConfig) = providerStore.upsert(config)
    fun deleteProvider(id: String) = providerStore.delete(id)
    fun setActiveProvider(id: String) = providerStore.setActive(id)
    fun ensureDefaultProvider() = providerStore.createDefaultIfEmpty()

    suspend fun runChoice(
        question: String,
        options: List<ChoiceOption>,
        state: String?,
        provider: ProviderConfig,
    ): Pair<DecideResult, Long> = withContext(Dispatchers.IO) {
        val result = api.choice(
            endpointUrl = provider.baseUrl,
            apiKey = provider.apiKey,
            request = ChoiceRequest(
                question = question,
                options = options,
                state = state,
                model = provider.model,
            ),
        )
        val id = persist(
            questionType = QuestionType.Choice,
            question = question,
            state = state,
            optionsJson = json.encodeToString(options.map { mapOf("id" to it.id, "label" to it.label) }),
            result = result,
            provider = provider,
        )
        result to id
    }

    suspend fun runScore(
        question: String,
        levels: List<String>,
        state: String?,
        provider: ProviderConfig,
    ): Pair<DecideResult, Long> = withContext(Dispatchers.IO) {
        val result = api.score(
            endpointUrl = provider.baseUrl,
            apiKey = provider.apiKey,
            request = ScoreRequest(
                question = question,
                levels = levels,
                state = state,
                model = provider.model,
            ),
        )
        // Store levels with index ids so result bars can map "0","1",… → text.
        val optionsJson = json.encodeToString(
            levels.mapIndexed { index, label -> mapOf("id" to index.toString(), "label" to label) }
        )
        val id = persist(
            questionType = QuestionType.Score,
            question = question,
            state = state,
            optionsJson = optionsJson,
            result = result,
            provider = provider,
        )
        result to id
    }

    suspend fun runNoul(
        question: String,
        state: String?,
        provider: ProviderConfig,
    ): Pair<DecideResult, Long> = withContext(Dispatchers.IO) {
        val result = api.noul(
            endpointUrl = provider.baseUrl,
            apiKey = provider.apiKey,
            request = NoulRequest(
                question = question,
                state = state,
                model = provider.model,
            ),
        )
        val id = persist(
            questionType = QuestionType.Noul,
            question = question,
            state = state,
            optionsJson = "[]",
            result = result,
            provider = provider,
        )
        result to id
    }

    private suspend fun persist(
        questionType: QuestionType,
        question: String,
        state: String?,
        optionsJson: String,
        result: DecideResult,
        provider: ProviderConfig,
    ): Long {
        val entry = HistoryEntry(
            createdAt = System.currentTimeMillis(),
            questionType = questionType,
            question = question,
            state = state?.takeIf { it.isNotBlank() },
            optionsJson = optionsJson,
            probabilitiesJson = json.encodeToString(result.probabilities),
            confidence = result.confidence,
            choice = result.choice,
            score = result.score,
            noul = result.noul,
            model = result.model ?: provider.model,
            providerName = provider.name,
            rawJson = result.rawJson,
        )
        return historyDao.insert(entry.toEntity())
    }

    suspend fun listModels(provider: ProviderConfig): List<ModelInfo> =
        withContext(Dispatchers.IO) {
            api.listModels(provider.baseUrl, provider.apiKey)
        }

    suspend fun getHistory(id: Long): HistoryEntry? =
        withContext(Dispatchers.IO) { historyDao.getById(id)?.toDomain() }

    suspend fun deleteHistory(id: Long) =
        withContext(Dispatchers.IO) { historyDao.deleteById(id) }

    suspend fun clearHistory() =
        withContext(Dispatchers.IO) { historyDao.clearAll() }
}
