# AGENTS.md

Instructions for coding agents working on **HEV** (Android client for TypeSafe Jev System One).

Choice, score, and noul are all shipped on `main` as equal first-class types.

## Build and test

Requires JDK 17 and Android SDK (`sdk.dir` in `local.properties`; see `local.properties.example`).

```bash
export JAVA_HOME=/path/to/jdk-17
./gradlew assembleDebug
```

Use `assembleDebug` for local checks. App version stays **1.0.0** unless the human asks for a release bump.

## Project layout

- Kotlin, Jetpack Compose, Material 3
- Package: `ai.hev.app`
- Layers: `domain` / `data` / `ui`
- App code under `app/src/main/java/ai/hev/app/`

## Languages

- Code, comments, this file, and README: English
- UI string resources: English in `values/` only (no Chinese locale)
- Conversation with the human may be Chinese; that does not change in-repo language rules

## Product UI tone

How the **app** should read to end users (string resources and on-screen labels):

- Modern and clean
- Copy only serves the current action
- Short labels; meaning comes from layout and controls, not paragraphs

## Jev API

- Store the provider Endpoint URL exactly as entered (no path stitching)
- Empty `state` falls back to the question text
- Show HTTP errors using the server response body when present
- API keys in encrypted prefs; history in Room until the user deletes it
- Default base: `https://api.typesafe.ai` — `POST /v1/systemone`

## Question types

Shared decide path; types differ in `type`, `criteria`, and result fields.

### Choice

- API `type`: `choice`
- `criteria`: map of option id → label
- UI: 2–255 options (ids `a`, `b`, … Excel-style); import from structured text replaces the list
- Result: confidence chip + probability bars labeled `a` / `b` / `c` …; model · provider under results

### Score

- API `type`: `score`
- `criteria`: ordered JSON array of 2–10 level description strings (low → high)
- Returns `score` (may be fractional), `probabilities`, `confidence`
- Home: levels list (same row UI as options; labels are level texts; min 2, max 10)
- Result: numeric score prominent + confidence + optional per-level probability bars (probability keys are often `"0"`, `"1"`, … — map to level text by index)

### Noul

- API `type`: `noul`
- Request uses `instructions` (and `state`) only; no `criteria`
- Returns `noul` 0–1 (yes probability)
- Home: no options / levels section
- Result: probability as the primary readout; do not invent a choice-style winner; skip the confidence chip when the API omits confidence

## Home UX

- Type control: Choice / Score / Noul (segmented control)
- Switching type keeps question and state; resets lists to:
  - choice: `a` + `b` empty
  - score: two empty levels
  - noul: none

## History

- Fields: `questionType` (`choice` | `score` | `noul`), `score`, `noul`
- Room schema v2
- Row subtitle: choice shows winner-style text; score shows the score; noul shows formatted probability
- Missing `questionType` on old rows means choice

## API DTOs

- Choice: map criteria; score: list criteria; noul: omit criteria
- Criteria as `JsonObject` / `JsonElement`, or built per type in `JevApiClient`
- `AnswerDto` has optional `score` and `noul`

## Domain

- Shared `DecideResult` (and related types) plus `HistoryEntry` covering all three question types
- Thin ViewModels; decide entry points stay typed (`runChoice` / `runScore` / `runNoul`) or a single `runDecide` — avoid unstructured branching

## Agent rules

Rules for **you** when editing this repo (not end-user UI copy):

- Do not add help walls, feature essays, or conceptual explainers into the app UI (including “what is confidence”)
- Do not stitch path segments onto a full Endpoint URL the user already set
- Do not bump `versionCode` / `versionName` without an explicit ask
- Do not commit secrets; keys stay in encrypted prefs / local-only config
- Do not write docs or code that treat score or noul as unfinished relative to choice
