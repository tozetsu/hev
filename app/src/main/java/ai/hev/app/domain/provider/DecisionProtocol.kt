package ai.hev.app.domain.provider

/** Wire protocol a provider speaks. */
enum class DecisionProtocol {
    /** TypeSafe System One: `state` + keyed `questions` (noul / choice / score). */
    SystemOne,

    /** OpenAI Decisions: `input` + named `questions` (predicate / choice / score). */
    OpenAiDecisions,
}
