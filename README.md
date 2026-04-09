# NFC Studio Ultra

A modular Android starter project for reading and writing NFC tags with a dark neon visual language inspired by the supplied screenshots.

## What is included

- Jetpack Compose UI
- Reader mode based NFC discovery
- NDEF text and URI writing
- Basic NDEF parsing for reads
- In-memory session history
- Modular package layout so each major concern stays in its own file

## Current structure

```text
nfc-studio-ultra/
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
├── README.md
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/cyphershadowbourne/nfcstudioultra/
        │   ├── MainActivity.kt
        │   ├── data/repository/InMemoryHistoryRepository.kt
        │   ├── domain/model/
        │   ├── domain/repository/HistoryRepository.kt
        │   ├── nfc/
        │   ├── ui/
        │   │   ├── NfcStudioUltraApp.kt
        │   │   ├── components/
        │   │   ├── screens/
        │   │   ├── theme/
        │   │   └── viewmodel/
        │   └── util/
        └── res/
```

## Build notes

1. Open the project in a current Android Studio build.
2. Let Android Studio install the matching Android SDK and Gradle components.
3. Sync the project.
4. Build a debug APK from the IDE.

## Important constraints

- This starter project uses an in-memory history store. If you want persistence next, replace that repository with Room or DataStore.
- The project assumes NDEF-oriented tag workflows. Raw tech-specific operations such as Mifare Classic sector handling are not included yet.
- I could not build the APK in this environment because the Android SDK and Gradle wrapper binaries are not available here.

## Suggested next production steps

- Add Room-backed history and saved presets.
- Add input validation for URL and URI modes.
- Add export/share for tag read history.
- Add a dedicated diagnostics screen for tech capabilities and writable capacity.
- Add UI tests and NFC parser unit tests.
