# Palapa GO

**English** | [Bahasa Indonesia](README_ID.md)

Palapa GO is an autonomous mobile AI workspace, academic research engine, and coding agent developed for the Android platform. Built with Kotlin 2.0 and Jetpack Compose (Material 3), the application bridges foundational language models with local storage access, multi-database academic search engines, and sandboxed file execution tools directly on mobile devices.

---

## Key Features and Operating Modes

### 1. Chat Mode
- Low-latency Server-Sent Events (SSE) streaming for real-time response generation.
- Full offline message and session persistence powered by Android Room Database.
- Markdown rendering support including tables, code blocks, and copy actions.

### 2. Pelajar Mode (Academic Research)
- Multi-API aggregation querying OpenAlex, Semantic Scholar, and Crossref in parallel.
- Direct DOI (Digital Object Identifier) verification to eliminate citation hallucination.
- Automated bibliography formatting supporting APA, IEEE, and BibTeX styles.
- Open-access full-text PDF discovery and download capabilities.

### 3. Agent Mode (Autonomous Workspace)
- File system manipulation restricted to an explicitly designated workspace directory.
- Human-in-the-Loop diff approval system providing visual side-by-side inspections before modifications are written to disk.
- Sandboxed shell command execution with output truncation to prevent context exhaustion.
- Protection against directory traversal attacks.

### 4. Canvas Mode
- Side-by-side document and visual layout workspace.
- Live web rendering supporting HTML, CSS, and interactive JavaScript.
- Markdown document drafting and export.

### 5. Image Generation Mode
- Prompt-based visual asset generation for presentations, promotional copy, and creative projects.
- Configurable aspect ratios and direct gallery export.

---

## Security and System Resilience

- **Human-in-the-Loop Governance**: File write operations, diff patches, and shell commands require explicit user consent via approval dialogs unless autonomous execution is intentionally toggled.
- **Smart Retry Interceptor**: Exponential backoff with full jitter to automatically handle upstream HTTP 429 (rate limits) and 5xx server errors.
- **BYOK (Bring Your Own Key)**: Decentralized credential management allowing users to supply custom API keys, base URLs, and model identifiers (OpenAI-compatible, Google Gemini, DeepSeek). Credentials remain stored strictly on the local device via Encrypted DataStore.

---

## Technical Specifications

- **Operating System**: Android 8.0 (API level 26) or higher
- **Programming Language**: Kotlin 2.0+
- **UI Toolkit**: Jetpack Compose with Material 3 Expressive tokens
- **Architecture**: Clean Architecture with Unidirectional Data Flow (UDF) and MVVM
- **Local Persistence**: Room Database (SQLite) and Jetpack DataStore Preferences
- **Networking**: OkHttp 4.12 with custom SSE stream parser and retry interceptors
- **Serialization**: kotlinx.serialization

---

## Building and Installation

### Prerequisites
- Java Development Kit (JDK) 17 or higher
- Android SDK (API level 34 or 35 recommended)
- Android Debug Bridge (ADB)

### Build Commands

To compile the debug build:
```bash
./gradlew assembleDebug
```

On Windows Command Prompt or PowerShell:
```powershell
.\gradlew.bat assembleDebug
```

The resulting APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Installing on Device

With USB debugging enabled on your Android device:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Configuration

Upon launching the application, open the Settings screen to configure your inference backend:
1. **API Base URL**: Specify the target endpoint (for example, official providers or local proxy endpoints).
2. **API Key**: Enter your personal API credential (BYOK).
3. **Model Identifier**: Define the target model name (e.g., `gemini-2.0-flash`, `deepseek-chat`, or `gpt-4o`).
4. **Max Retries**: Configure network retry thresholds for high-latency connections.

---

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
