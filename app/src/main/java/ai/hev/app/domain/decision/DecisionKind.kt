package ai.hev.app.domain.decision

/** The three decision shapes hev asks for. Wire names belong to the protocol adapters. */
enum class DecisionKind {
    Choice,
    Score,
    YesNo,
}
