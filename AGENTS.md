# AGENTS.md — AI Agent Guidance & Coding Standards
# Project: GG Agent (id.gustyxganteng.agent)

Selamat datang di codebase **GG Agent** — Android AI Workspace & Coding Agent berbasis Kotlin 2.0+, Jetpack Compose Material 3, dan Clean Architecture + MVVM/MVI.

Dokumen ini menjadi rujukan utama bagi AI Coding Agent (Antigravity, Claude Code, Cursor, Copilot, dsb.) saat membangun, memelihara, dan mengembangkan fitur-fitur dalam proyek ini.

---

## 1. Arsitektur & Prinsip Desain

1. **Clean Architecture + Unidirectional Data Flow (UDF)**:
   - **UI Layer** (`feature/*`, `core:designsystem`): Murni menampilkan state (`StateFlow<UiState>`) dan mengirim event/intent pengguna (`UiIntent` / `UiEvent`) ke ViewModel.
   - **Domain Layer** (`domain/*`): Berisi UseCase / Orchestrator, Tool implementations, dan Model Domain murni tanpa ketergantungan pada Android UI framework.
   - **Data Layer** (`core:network`, `core:database`): Implementasi repository, Room DAO, OkHttp SSE client, DataStore settings, dan mapper DTO ke domain model.
2. **Kemandirian Modul / Paket**:
   - `core:model` tidak boleh bergantung pada modul lain (foundation).
   - Domain layer tidak boleh mengimpor package Compose UI.
   - Modul `feature/*` hanya berkomunikasi dengan domain layer atau core data lewat dependency injection.
3. **Dependency Injection**:
   - Menggunakan Koin (ringan, ramah kompilasi cepat di Kotlin 2.0) atau Hilt.
   - Seluruh Network Client, Database, Tools, dan Repository di-inject sebagai singleton/factory.

---

## 2. Aturan Gaya Penulisan Kode (Coding Style)

- **Bahasa**: Kotlin 2.0+ dengan Idiomatic Kotlin (misal data objects, inline value classes, functional operators).
- **Asinkron & Streaming**:
  - Wajib menggunakan Kotlin Coroutines (`Dispatchers.IO`, `Dispatchers.Default`, `Dispatchers.Main.immediate`).
  - Untuk streaming SSE (reasoning, teks, tool calls), gunakan `Flow<StreamChunk>` yang reactive dan thread-safe.
- **Serialization**: Gunakan `kotlinx.serialization` (hindari Gson/Jackson demi performa dan kompatibilitas Kotlin multiplatform).
- **Jetpack Compose UI**:
  - Gunakan `androidx.compose.material3`.
  - Pisahkan komponen Composable menjadi fungsi modular berukuran kecil (< 150 baris per fungsi).
  - Gunakan `@Immutable` atau `@Stable` pada data class UI State untuk optimasi recomposition.
  - Komponen Markdown harus mendukung scroll horizontal untuk blok kode dan tabel, tidak boleh memotong teks kode secara paksa (`overflow-x: auto`).

---

## 3. Protokol Keamanan & Resiliensi Tool Execution

1. **Batas Direktori (Workspace Sandboxing)**:
   - Tool `FileSystemTool` dan `ShellTool` **WAJIB** mengecek path relatif terhadap active workspace folder (`activeWorkspacePath`).
   - Mencegah traversal path di luar workspace (misal blokir `../../` yang keluar dari root workspace kecuali jika user memberikan izin eksplisit).
2. **Approval System (YOLO Mode vs Human-in-the-Loop)**:
   - Jika `yoloMode == false` (default):
     - Panggilan `write_file`, `apply_diff`, dan `execute_shell` **HARUS** di-pause di `AgentOrchestrator`.
     - Tampilkan UI `DiffApprovalDialog` (menampilkan diff hijau/merah) atau `ShellApprovalDialog`.
     - Lanjutkan eksekusi hanya setelah user menekan tombol **Approve**.
   - Jika `yoloMode == true`:
     - Tool dieksekusi otomatis, **KECUALI** perintah destruktif fatal (misal `rm -rf /`, `mkfs`, format storage).
3. **Resilience & Rate Limit (HTTP 429)**:
   - Implementasikan exponential backoff dengan full jitter pada retry interceptor.
   - Simpan state agent loop ke local storage saat retry berjalan agar konteks tidak hilang jika aplikasi ditutup sementara.
4. **Context Window Safety**:
   - Shell output harus dipotong maksimal 10.000 karakter terakhir agar context window tidak meledak.
   - Pantau `usage.total_tokens` dan beri peringatan jika mendekati batas kapasitas model.

---

## 4. Status Fase Pengerjaan (5-Phase Roadmap)

- [x] **Fase 1: Pondasi & Core Network**
  - Setup Gradle (`build.gradle.kts`, `settings.gradle.kts`, `libs.versions.toml`)
  - Modul Data Models (`core:model`: `Message`, `ToolCall`, `TokenUsage`, `CanvasArtifact`, dll.)
  - Modul Database (`core:database`: Room DB untuk Chat & Sesi, DataStore Preferences untuk BYOK & Settings)
  - Modul Jaringan (`core:network`: OkHttp 4.12, SSE Stream Parser, Smart Exponential Backoff Retry Interceptor untuk HTTP 429/5xx, Agnes API Service)
- [x] **Fase 2: UI Gemini & Rich Markdown**
  - Modul Design System (`core:designsystem`: Material 3 Dynamic Theme, Gemini Typography, Palet Warna)
  - Modul Markdown (`core:markdown`: Syntax Highlighting, Horizontal Scrollable Table, Copy Action)
  - Modul Chat UI (`feature:chat`: TopBar Dinamis, Context Meter Pill, Collapsible Thinking Accordion, Full-width Response, Floating Input Capsule, Vision Base64 Compressor)
- [x] **Fase 3: Engine Agent, File & Shell Tools**
  - Modul Tools (`domain:tools`: `FileSystemTool`, `ShellTool` via `ProcessBuilder`)
  - Modul Agent (`domain:agent`: `AgentOrchestrator` Multi-Turn Execution Loop & State Machine)
  - Modul Workspace UI (`feature:workspace`: Diff Approval Dialog Hijau/Merah, Terminal Log Viewer, Directory Picker)
- [x] **Fase 4: Web Search & Mode Canvas**
  - Modul Web Tools (`domain:tools/WebTool`: DuckDuckGo Lite Scraper & Jsoup Markdown Extractor)
  - Modul Canvas (`feature:canvas`: 3-Tab BottomSheet / Fullscreen: Code Editor, WebView Live Preview, Local Sync)
- [x] **Fase 5: Settings BYOK & Pemolesan Akhir**
  - Modul Settings (`feature:settings`: BYOK API Key, Base URL, Model Selection, Max Retries, YOLO Toggle)
  - Context Compactor (`domain:context/ContextMeterManager`: Otomatisasi ringkasan percakapan)
  - Foreground Service: Menjaga Agent Loop tetap hidup saat aplikasi diminimalkan

---

## 5. Instruksi Eksekusi Fase 1 (Pondasi & Core Network)

Saat mengeksekusi Fase 1, AI Agent harus memastikan:
1. Konfigurasi Gradle modular yang rapi menggunakan Version Catalog (`libs.versions.toml`).
2. Data class di `core:model` lengkap merefleksikan format OpenAI Chat Completions (termasuk delta stream, tool calls, reasoning_content, dan token usage).
3. SSE parser di `core:network` mampu menangkap event stream secara realtime tanpa memory leak.
4. Smart Retry Interceptor menangani HTTP 429, 502, 503, dan 504 dengan membaca header `Retry-After` atau menerapkan exponential backoff.
5. Room database & DataStore tersambung dengan entitas sesi percakapan, pesan, dan konfigurasi API Key.

---

## 6. Standar Anti-AI Slop (Writing, UI/UX Design, & Clean Code)

Seluruh kontribusi kode, desain antarmuka, dan teks WAJIB mematuhi skill `no-ai-slop`:
1. **Frontend & Jetpack Compose**:
   - Dilarang keras glassmorphism berlebihan, gradasi neon ungu/cyan/indigo, dan text gradient.
   - Gunakan intentional surface layering (Canvas `#0B0C0E` -> Card `#131417` -> Elevated `#1B1D22`) dengan border hairline tipis (`1px`).
   - Jangan gunakan emoji teks sebagai icon di kartu atau tombol; gunakan Vector Icon Material.
   - Hindari layout klise landing page AI (hero pill glowing, 3 kartu simetris seragam, fake testimonial/pricing).
2. **Rekayasa Perangkat Lunak & Kode**:
   - Zero narrator comments (`// Initialize state`, `// Handle click`, `<!-- Footer -->`). Tulis komentar hanya untuk alasan non-obvious *WHY*.
   - Larangan keras placeholder malas (`// ... rest of code`, `// TODO`). Tulis implementasi lengkap dan fungsional.
   - Hindari over-engineering, defensive coding berlebihan, dan abstraksi kosong (interface-implementation 1-to-1 yang tidak perlu).
3. **Komunikasi & Tulisan (EN & ID)**:
   - Mulai langsung ke inti jawaban/tindakan tanpa pembuka penjilat (*"Tentu!"*, *"Siap!"*, *"Pertanyaan bagus!"*).
   - Larang kata klise AI: *delve, foster, leverage, utilize, seamless, cutting-edge, inovasi terdepan, solusi komprehensif, di era digital yang terus berkembang*.
   - Tidak ada rekap 10 poin membanggakan diri setelah menulis kode. Cukup jelaskan 1-2 kalimat esensial secara profesional.

