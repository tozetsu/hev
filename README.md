# HEV

<p align="center">
  <img src="docs/icon.png" alt="HEV" width="128" height="128">
</p>

License: [GPL-3.0](LICENSE).

Android client for decision models. Ask choice, score, or yes/no questions and get back probabilities and confidence from any vendor that speaks the TypeSafe System One API or the OpenAI Decisions API.

Stack: Kotlin, Jetpack Compose, Material 3. Package `ai.hev.app` (minSdk 26, targetSdk 35).

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

## Build

Requires JDK 17 and an Android SDK (`sdk.dir` in `local.properties`; see `local.properties.example`).

```bash
export JAVA_HOME=/path/to/jdk-17
./gradlew testDebugUnitTest assembleDebug
```

## Data

History is stored on the device with Room and kept across upgrades. API keys are stored in EncryptedSharedPreferences. Requests go only to the endpoint you configure.

## Disclaimer

Unofficial client, not affiliated with any of the vendors listed. You supply your own endpoint and API key and are responsible for usage, costs, and each provider's terms. Provided as-is, without warranty. Model output is yours to interpret and act on.
