package ai.hev.app.domain.provider

/** Why a provider configuration cannot be saved or used. */
enum class ProviderIssue {
    MissingName,
    InvalidEndpoint,
    MissingApiKey,
    MissingModel,

    /** The keyring holding API keys is locked. */
    KeyringLocked,
}
