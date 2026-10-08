# HEV

<p align="center">
  <img src="docs/icon.png" alt="HEV" width="128" height="128">
</p>

License: [GPL-3.0](LICENSE).

Android and Linux desktop client for decision models. Ask choice, score, or yes/no questions and get back probabilities and confidence from any vendor that speaks the TypeSafe System One API or the OpenAI Decisions API.

Stack: Kotlin Multiplatform, Compose Multiplatform, Material 3. Package `ai.hev.app`.

- `shared/`: domain, data, and the whole UI
- `app/`: the Android app (minSdk 26, targetSdk 35); tablets and foldables get a navigation rail and side-by-side panes
- `desktop/`: the Linux desktop app, shipped as an AppImage

## Providers

Pick a preset or add a custom endpoint under Settings → Providers. Presets only prefill the form; every field stays editable.

| Preset | Protocol | Endpoint |
| --- | --- | --- |
| TypeSafe | System One | `https://api.typesafe.ai/v1/systemone` |
| Perplexity | System One | `https://api.perplexity.ai/v1/decisions` |
| Alibaba Beijing | System One | `https://{WorkspaceId}.cn-beijing.maas.aliyuncs.com/compatible-mode/v1/systemone` |
| Alibaba Singapore | System One | `https://{WorkspaceId}.ap-southeast-1.maas.aliyuncs.com/compatible-mode/v1/systemone` |
| OpenRouter | System One | `https://openrouter.ai/api/v1/systemone` |
| Vercel System One | System One | `https://ai-gateway.vercel.sh/typesafe/v1/systemone` |
| DeepInfra | System One | `https://api.deepinfra.com/v1/decisions` |
| Liquid | System One | `https://api.liquid.ai/decisions/v1/systemone` |
| Ollama | System One | `http://localhost:11434/v1/systemone` |
| OpenAI | OpenAI Decisions | `https://api.openai.com/v1/decisions` |
| Vercel Decisions | OpenAI Decisions | `https://ai-gateway.vercel.sh/v1/decisions` |

For Alibaba, replace `{WorkspaceId}` with your workspace ID. Ollama needs no API key. Where the vendor offers a model list, the model field loads it.

## Desktop

Download `hev-<version>-x86_64.AppImage` from the releases, make it executable, and run it. It needs no libfuse2; without FUSE at all, run it with `--appimage-extract-and-run`.

| Shortcut | Action |
| --- | --- |
| Ctrl+Enter | Submit |
| Ctrl+N | New decision |
| Ctrl+, | Settings |
| Esc | Back |
| Ctrl+Q | Quit |

## Build

Requires JDK 21 and an Android SDK (`sdk.dir` in `local.properties`; see `local.properties.example`).

```bash
export JAVA_HOME=/path/to/jdk-21
./gradlew :shared:jvmTest :app:assembleDebug :app:lintDebug
./gradlew :desktop:run
./gradlew :desktop:packageAppImage   # needs the file command; writes desktop/build/appimage/
```

`packageAppImage` downloads appimagetool and the AppImage runtime and checks their SHA-256 sums; pass `-Pappimagetool=<path>` and `-PappimageRuntime=<path>` to use local copies.

CI builds every push to `main`. Pushing a tag `v<version>` that matches `version` in `gradle.properties` drafts a GitHub release with the AppImage and its SHA-256 sum.

## Data

History is stored with Room and kept across upgrades. Requests go only to the endpoint you configure.

- Android: API keys are stored in EncryptedSharedPreferences.
- Desktop: history lives in `~/.local/share/hev/`, settings in `~/.config/hev/` (or the XDG directories you set). API keys go to the Secret Service keyring (GNOME Keyring, KWallet) when the session has one, otherwise to `~/.local/share/hev/secrets.json`, readable only by you; keys move into the keyring once one is available. Settings → About shows which is in use.

## Disclaimer

Unofficial client, not affiliated with any of the vendors listed. You supply your own endpoint and API key and are responsible for usage, costs, and each provider's terms. Provided as-is, without warranty. Model output is yours to interpret and act on.
