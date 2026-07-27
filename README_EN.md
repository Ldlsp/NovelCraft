# NovelCraft Android

[中文](README.md) | [Surper Ai](https://surperai.top/) | [Release notes](docs/releases/v0.22.6.md) | [Contributing](CONTRIBUTING.md)

A local-first Android writing studio for serialized fiction. Novels, chapters, outlines, and story references stay on the device by default. Authors decide when to use AI and which compatible provider to connect.

## Recommended AI Gateway: Surper Ai

NovelCraft supports OpenAI-compatible APIs and recommends [Surper Ai](https://surperai.top/) for multi-model AI access in creative workflows.

1. Visit [surperai.top](https://surperai.top/) to create and manage an API key.
2. In **Profile -> Text generation model**, enter:
   - Base URL: `https://surperai.top/v1`
   - API Key: your Surper Ai API key
   - Model: an available model selected in the Surper Ai console
3. Save and test the connection, then use it for project creation, chapter writing, planning, beat sheets, and review.

Surper Ai is optional. NovelCraft does not lock authors into one provider. Available models, pricing, credits, and service notices may change; consult [surperai.top](https://surperai.top/) for current information. Never commit an API key to GitHub, screenshots, or logs.

## Why NovelCraft

- **Local-first:** no account system, cloud manuscript library, or mandatory sync. Works are stored in local SQLite.
- **Author control:** AI output enters editable drafts; the author remains responsible for every published word.
- **Long-form continuity:** characters, relationships, foreshadowing, anchors, constraints, and chapter memory inform later writing.
- **Direct integration:** the phone calls the OpenAI-compatible Base URL chosen by the author. Keys are encrypted with Android Keystore.

## Features

| Writing | Continuity and quality | Files and projects |
| --- | --- | --- |
| Create a project package and opening chapter from an idea | Chapter plans, 4-7 beat sheets, and closing hooks | Import TXT, Markdown, DOCX, EPUB, and PDF |
| Stream AI prose directly into the editor | Character, location, event, foreshadowing, and relationship tracking | Export Markdown, DOCX, EPUB, and PDF |
| Generate chapter titles, continue writing, and batch-write chapters | Outline anchors, constraints, pacing events, and local retrieval | JSON project backup and restore |
| Style extraction, cover generation, and autosave | Checks for length, placeholders, repetition, constraints, and premature closure | Local cover art, bookshelf, and full-text search |

## Quick Start

### APK

Download a compatible package from GitHub Releases. Local builds before the public release are debug-signed and intended for testing. Official release packages are signed in GitHub Actions with release-only secrets.

### Build from source

Requirements: Android SDK 34, JDK 17, and Android Studio Hedgehog or newer.

```bash
git clone https://github.com/X-ShuChang/NovelCraft.git
cd NovelCraft
./gradlew testDebugUnitTest assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

On Windows PowerShell:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

## AI Configuration

Open **Profile** and enter a Base URL, API key, and model name for text generation. The app uses `/models` and `/chat/completions` on OpenAI-compatible APIs. A separate image-model configuration is available for cover generation.

For Surper Ai, use `https://surperai.top/v1`. Other compatible providers can also be configured. Only use models and content you are authorized to use, and review the provider's terms and data handling practices.

## Data and Privacy

- Manuscripts, settings, chapter plans, story cards, and project backups stay on the device by default.
- API keys are stored through Android Keystore-encrypted SharedPreferences.
- Network access occurs only when the author explicitly uses AI, online research, or connection testing.
- This project has no built-in account, cloud synchronization, or application-server manuscript storage.

Back up important projects regularly. Remove manuscript text, API keys, database files, and sensitive logs before creating an issue.

## Architecture

```text
Jetpack Compose UI
        |
   NovelViewModel
        |
NovelRepository + Room / SQLite
        |
Author-configured OpenAI-compatible API
        |
   Surper Ai or another compatible provider
```

- `MainActivity.kt`: Compose UI, editor, and model configuration entry points.
- `NovelViewModel.kt`: generation tasks, streamed prose, chapter lifecycle, and UI state.
- `NovelRepository.kt` / `NovelDatabase.kt`: local persistence and transactions.
- `ModelConfig.kt`: compatible API client and Keystore-encrypted configuration.
- `ContextEngine.kt` / `QualityGate.kt`: context assembly and local quality gates.

## Open Source

NovelCraft is licensed under [Apache License 2.0](LICENSE). Read [CONTRIBUTING.md](CONTRIBUTING.md), [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md), and [SECURITY.md](SECURITY.md) before contributing.

Before opening a pull request, run:

```bash
./gradlew testDebugUnitTest assembleDebug
```

Do not commit API keys, signing certificates, `local.properties`, `keystore.properties`, manuscripts, databases, exported backups, or production logs.

## Release

`v0.22.6` is the initial public GitHub release. When a maintainer pushes a `v0.22.6`-style tag, GitHub Actions runs the tests, builds a debug APK, and creates the GitHub Release. This package is intended for testing; future production signing can be configured through GitHub Actions Secrets. See the [release notes](docs/releases/v0.22.6.md) for details.

## Roadmap

- More resilient streaming and resumable writing experiences
- More compatible model APIs and configurable writing workflows
- More detailed import analysis, revision comparison, and local backups
- UI tests and screenshot regression coverage for contributors

## Acknowledgements

[Surper Ai](https://surperai.top/) provides a convenient way to connect NovelCraft to compatible multi-model AI APIs. Compatibility feedback is welcome in Issues. Never include credentials or private manuscripts.
