# AGENTS.md

Instructions for coding agents working on **HEV**, an Android and Linux desktop client for decision models (TypeSafe System One and OpenAI Decisions protocols).

## Build and test

Requires JDK 21 and Android SDK (`sdk.dir` in `local.properties`; see `local.properties.example`).

```bash
export JAVA_HOME=/path/to/jdk-21
./gradlew :shared:jvmTest :app:assembleDebug :app:lintDebug
```

All three must pass before every commit; also run `:desktop:packageAppImage` when touching `desktop/`. The version lives in `gradle.properties` and stays **1.0.0** unless the human asks for a release bump.

## Project layout

- Kotlin Multiplatform, Compose Multiplatform, Material 3; package `ai.hev.app`
- Modules: `shared` (everything below), `app` (Android shell), `desktop` (Linux shell and AppImage packaging)
- `shared` source sets: `commonMain` for all logic and UI, `androidMain` and `jvmMain` (desktop) only for platform storage and the `expect`/`actual` pieces
- `domain/`: plain Kotlin, no Android or wire types
  - `decision/`: `DecisionKind` (Choice, Score, YesNo), `Question`, `DecisionRequest`, `DecisionOutcome`, `DecisionError`, `ModelCapabilities`, `DecisionDraft`
  - `provider/`: `DecisionProtocol`, `ProviderConfig`, `VendorLimits`, `Endpoints`
  - `history/`: `HistoryEntry`
- `data/remote/`: `DecisionClient`, one codec per protocol, `ModelCatalog`, `http/` (transport, retry, error bodies)
- `data/local/`: Room (`db/`), provider and theme storage (`prefs/`; encrypted prefs on Android, JSON files on desktop), `StorageKeys`, desktop `secrets/` (Secret Service keyring, else a 0600 file)
- `ui/`: screens and ViewModels; thin ViewModels over pure state classes; Navigation 3 with list-detail panes from 840 dp wide
- `AppGraph` wires the app; each platform has a factory (`AppGraph(context)`, `AppGraph(dirs)`)
- Tests in `shared/src/jvmTest/`; fixtures in `shared/src/jvmTest/resources/fixtures/<vendor>/`

## Languages

- Code, comments, commit messages, this file, and README: English
- English is the only locale: strings live in `shared/src/commonMain/composeResources/values/` only; do not add translations or a language setting
- The human may converse in another language; that does not change in-repo rules

## Product UI tone

- Modern and clean
- Copy only serves the current action: labels, field names, short errors
- No help text, explainers, or onboarding copy anywhere in the app
- Range errors use placeholders (`Need %1$d–%2$d options`), never hard-coded limits
- Phones keep the single-pane flow; tooltips, shortcuts, selectable text, scrollbars, and context menus are desktop only, behind `desktopUi` in `ui/common/Desktop.kt`

## Decisions providers

- A provider is a `ProviderConfig`: name, protocol, endpoint, API key, model; the user fills them in
- No presets or vendor lists in the UI or README; the edit form is name, protocol, endpoint, API key, model
- Store the endpoint exactly as entered; never stitch path segments onto it
- Model lists are looked up next to the endpoint (`…/v1/models`), then at `/api/tags`, each resolved like a link; they accept `{"models": [{"name"}]}` and `{"data": [{"id"}]}`; no list falls back to manual entry without a message
- Stored providers from older versions may carry `presetId` or `modelsUrl`; `ProviderCodec` ignores unknown keys, so keep it tolerant
- API keys stay in encrypted prefs on Android and in the keyring or the 0600 secrets file on desktop; send `Authorization: Bearer` only when a key is set
- A keyring the user will not unlock is an error, never a silent fallback to the file

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
- `VendorLimits` maps an endpoint's host (or Ollama's port) to the limits that vendor documents; every other endpoint uses `ModelCapabilities.Lenient`
- Limits that are documented per vendor stay keyed by host; key by model only if a vendor documents per-model limits
- The server is the final authority; show its error rather than guessing
- No UI for editing limits

### Adding a vendor

- Add its documented limits to `VendorLimits` (if it has any) with a test in `CapabilitiesTest`
- Add it to `testing/Vendors` with its protocol, documented endpoint, and a model
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
- Do not commit secrets; keys stay in encrypted prefs, the keyring, or local-only config
- Do not add repository mirrors or other machine-specific build settings to the repo
- Do not push tags or create releases unless the human asks
- Do not add destructive migrations or `fallbackToDestructiveMigration`
- Keep one shared decide path; no per-kind or per-vendor run functions
