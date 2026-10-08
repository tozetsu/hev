package ai.hev.app.data.repository

import ai.hev.app.data.local.db.HistoryDao
import ai.hev.app.data.local.db.HistoryMapper
import ai.hev.app.data.local.prefs.ProviderStore
import ai.hev.app.data.remote.DecisionClient
import ai.hev.app.domain.decision.ChoiceOption
import ai.hev.app.domain.decision.DecisionRequest
import ai.hev.app.domain.decision.Question
import ai.hev.app.domain.history.HistoryEntry
import ai.hev.app.domain.provider.ProviderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class HevRepository(
    private val client: DecisionClient,
    private val historyDao: HistoryDao,
    private val providerStore: ProviderStore,
) {
    val providers = providerStore.providers
    val activeProviderId = providerStore.activeId
    val history: Flow<List<HistoryEntry>> =
        historyDao.observeAll().map { rows -> rows.map(HistoryMapper::toDomain) }

    fun activeProvider(): ProviderConfig? = providerStore.activeProvider

    fun upsertProvider(config: ProviderConfig) = providerStore.upsert(config)
    fun deleteProvider(id: String) = providerStore.delete(id)
    fun setActiveProvider(id: String) = providerStore.setActive(id)
    fun ensureDefaultProvider() = providerStore.createDefaultIfEmpty()

    /** Sends [request] to [provider], stores the answer, and returns the new history id. */
    suspend fun decide(provider: ProviderConfig, request: DecisionRequest): Long {
        val result = client.decide(provider.protocol, provider.endpoint, provider.apiKey, request)
        val entry = HistoryEntry(
            createdAt = System.currentTimeMillis(),
            kind = request.kind,
            instructions = request.instructions,
            context = request.context,
            items = request.question.items(),
            outcome = result.outcome,
            model = result.model ?: request.model,
            providerName = provider.name,
            rawJson = result.rawJson,
            providerId = provider.id,
            protocol = provider.protocol,
            inputTokens = result.usage.inputTokens,
        )
        return historyDao.insert(HistoryMapper.toEntity(entry))
    }

    suspend fun getHistory(id: Long): HistoryEntry? =
        withContext(Dispatchers.IO) { historyDao.getById(id)?.let(HistoryMapper::toDomain) }

    suspend fun deleteHistory(id: Long) =
        withContext(Dispatchers.IO) { historyDao.deleteById(id) }

    suspend fun clearHistory() =
        withContext(Dispatchers.IO) { historyDao.clearAll() }

    private fun Question.items(): List<ChoiceOption> = when (this) {
        is Question.Choice -> options
        is Question.Score -> levels.mapIndexed { index, label -> ChoiceOption(index.toString(), label) }
        Question.YesNo -> emptyList()
    }
}
