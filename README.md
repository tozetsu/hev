# HEV

<p align="center">
  <img src="docs/icon.png" alt="HEV" width="128" height="128">
</p>

License: [GPL-3.0](LICENSE).

Android client for the TypeSafe **Jev** System One API — choice, score, and noul questions with probability and confidence results.

Stack: Kotlin, Jetpack Compose, Material 3. Package `ai.hev.app` (minSdk 26, targetSdk 35).

## Build

Requires JDK 17 and an Android SDK (`sdk.dir` in `local.properties`; see `local.properties.example`).

```bash
export JAVA_HOME=/path/to/jdk-17
./gradlew assembleDebug
```

## Usage

Configure a provider (endpoint, API key, model) under Settings, then submit a choice, score, or noul question on the home screen. Results and history are stored locally (Room; API keys in EncryptedSharedPreferences).

Default API base: `https://api.typesafe.ai` — `POST /v1/systemone`.

## Disclaimer

Unofficial client. Not affiliated with TypeSafe AI. You supply your own endpoint and API key and are responsible for API use, costs, and compliance with the provider’s terms. Provided as-is, without warranty. Decisions from the API are yours to interpret and act on.
