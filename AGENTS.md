# AGENTS.md

Instructions for coding agents working on **HEV**, an Android client for decision models (TypeSafe System One and OpenAI Decisions protocols).

## Build and test

Requires JDK 17 and Android SDK (`sdk.dir` in `local.properties`; see `local.properties.example`).

```bash
export JAVA_HOME=/path/to/jdk-17
./gradlew testDebugUnitTest assembleDebug
```

Both must pass before every commit. App version stays **1.0.0** unless the human asks for a release bump.

## Project layout

- Kotlin, Jetpack Compose, Material 3; package `ai.hev.app`
- `domain/`: plain Kotlin, no Android or wire types
  - `decision/`: `DecisionKind` (Choice, Score, YesNo), `Question`, `DecisionRequest`, `DecisionOutcome`, `DecisionError`, `ModelCapabilities`, `DecisionDraft`
  - `provider/`: `DecisionProtocol`, `ProviderConfig`, `ProviderPreset`, `ProviderPresets`, `Endpoints`
  - `history/`: `HistoryEntry`
- `data/remote/`: `DecisionClient`, one codec per protocol, `ModelCatalog`, `http/` (transport, retry, error bodies)
- `data/local/`: Room (`db/`), encrypted provider storage (`prefs/`), `StorageKeys`
- `ui/`: screens and ViewModels; thin ViewModels over pure state classes
- Tests in `shared/src/jvmTest/`; fixtures in `shared/src/jvmTest/resources/fixtures/<vendor>/`

## Languages

- Code, comments, commit messages, this file, and README: English
- English is the only locale: strings live in `values/` only; do not add translations or a language setting
- The human may converse in another language; that does not change in-repo rules

## Product UI tone

- Modern and clean
- Copy only serves the current action: labels, field names, short errors
- No help text, explainers, or onboarding copy anywhere in the app
- Range errors use placeholders (`Need %1$d–%2$d options`), never hard-coded limits

## Decisions providers

- A provider is a `ProviderConfig`: protocol, endpoint, API key, model, optional preset and models URL
- Presets in `ProviderPresets` only prefill the form; the saved provider owns its fields
- The protocol selector and models endpoint field are shown only for Custom
- Store the endpoint exactly as entered; never stitch path segments onto it
- A models URL may be absolute or relative to the endpoint (resolved like a link)
- Model lists accept `{"models": [{"name"}]}` and `{"data": [{"id"}]}`; any failure falls back to manual entry without a message
- API keys stay in encrypted prefs; send `Authorization: Bearer` only when a key is set

### Adapter and protocol rules

- The domain never sees wire names. Each `DecisionCodec` owns the full mapping for its protocol:
  - System One: `noul` / `choice` / `score`, `state`, `criteria` (map for choice, ordered array for score), answers keyed by question name
  - OpenAI Decisions: `predicate` / `choice` / `score`, `input`, `choices[{value, description}]`, `levels[{label}]`, answers array, `refusal`
- Codecs are pure; `DecisionClient` picks one with an exhaustive `when` over `DecisionProtocol`
- `HttpTransport` handles auth, status mapping to `DecisionError`, error body extraction, and one retry for 429 / 503 / 529 honouring `Retry-After` (capped at 10 s)
- New error body shapes go into `ErrorBody` with a test
- A new protocol means a new `DecisionProtocol` entry, a codec, golden request tests, and decode tests

### Capability-driven UI

- `ModelCapabilities` decides which kinds the home screen offers, item count limits, import caps, and validation
- Presets carry documented vendor limits only; unknown vendors and custom providers use `ModelCapabilities.Lenient`
- The server is the final authority; show its error rather than guessing
- No UI for editing limits

### Adding a vendor

- Add a `ProviderPreset` with its endpoint, models, optional models URL, and documented limits
- Ship fixtures copied from the vendor's official docs under `fixtures/<vendor>/`, and say so in the test when a body is shaped from a schema instead
- Add decode, error body, and vendor matrix cases for every fixture

## History

- Room database with exported schemas in `shared/schemas/`; bump the version only with an explicit `Migration` in `HevMigrations` and a `MigrationTestHelper` test
- Never use destructive migrations; history stays until the user deletes it
- Stored keys come from `StorageKeys`; never rename an existing key
- Entries record provider id, protocol, refusal, and input tokens

## Agent rules

- Do not add help walls, feature essays, or conceptual explainers to the app UI
- Do not stitch path segments onto a full endpoint URL the user already set
- Do not bump `versionCode` / `versionName` without an explicit ask
- Do not commit secrets; keys stay in encrypted prefs or local-only config
- Do not add destructive migrations or `fallbackToDestructiveMigration`
- Keep one shared decide path; no per-kind or per-vendor run functions
