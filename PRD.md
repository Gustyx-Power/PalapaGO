# Product Requirements Document (PRD) & Technical Blueprint
## GG Agent — Android AI Workspace & Coding Agent

### 1. Identitas & Spesifikasi Proyek
- **Nama Aplikasi**: GG Agent
- **Package ID**: `id.gustyxganteng.agent`
- **Platform Target**: Android 9.0 (API 28) hingga Android 15 (API 35)
- **Bahasa & UI Framework**: Kotlin 2.0+, Jetpack Compose (Material 3 / Material You)
- **Arsitektur Aplikasi**: Clean Architecture + MVVM/MVI (Unidirectional Data Flow)
- **Format Upstream API**: OpenAI Chat Completions (SSE Streaming, Tool Calling, Reasoning/Thinking, Vision Base64)
- **Default Endpoint & Model**:
  - Base URL: `https://apihub.agnes-ai.com/v1`
  - Model Utama: `agnes-3.0-flash` (cepat/ringan), `agnes-3.0-pro` (kemampuan penalaran tinggi, konteks s.d. 1M token)

---

### 2. Struktur Modul & Paket Proyek
```
id.gustyxganteng.agent/
├── core/
│   ├── model/          # Shared data models: Message, ToolCall, TokenUsage, WorkspaceConfig, CanvasArtifact
│   ├── network/        # OkHttp + SSE Stream Parser, Smart Retry Interceptor (Rate Limit Handler), Agnes API Service
│   ├── database/       # Room Database (ChatSessionDao, MessageDao, WorkspaceDao) + DataStore (API Key & Settings)
│   ├── markdown/       # Compose Markdown Engine (Syntax Highlighting, Scrollable Table, Copy/Canvas Action)
│   └── designsystem/   # Gemini Theme (Material 3 Dynamic Color, Typography, Collapsible Components)
├── domain/
│   ├── agent/          # AgentOrchestrator (Multi-turn Tool Calling Execution Loop & State Machine)
│   ├── tools/          # Implementasi Tool Lokal:
│   │   ├── FileSystemTool.kt   # list_dir, read_file, write_file, apply_diff, search_files
│   │   ├── ShellTool.kt        # execute_shell (ProcessBuilder /system/bin/sh & jembatan Termux opsional)
│   │   ├── WebTool.kt          # web_search (DuckDuckGo Lite) & read_webpage (Jsoup Markdown Extractor)
│   │   └── CanvasTool.kt       # create_or_update_canvas
│   └── context/        # ContextMeterManager (Kalkulasi token & Auto-Compact / Session Summarizer)
└── feature/
    ├── chat/           # Layar Utama ala Gemini, Floating Input Capsule, Thinking Accordion, Vision Picker
    ├── canvas/         # BottomSheet / Fullscreen Canvas (Code Editor, WebView Live Preview, Local File Sync)
    ├── workspace/      # Directory Picker, Diff Approval Dialog (Hijau/Merah), Terminal Output Log Viewer
    └── settings/       # BYOK (API Key, Base URL), Model Selector, Max Retry Config, YOLO Mode Toggle
```

---

### 3. Tech Stack & Dependencies
| Komponen | Library / Teknologi | Kegunaan Spesifik |
|---|---|---|
| UI & Navigasi | `androidx.compose.material3`, `androidx.navigation:navigation-compose` | Material 3 Dynamic Color, Navigation graph |
| Dependency Injection | Koin / Hilt | Injeksi Network, DB, Repositories, dan Agent Tools |
| Networking & SSE | OkHttp 4.12+ / `okhttp-sse` + Kotlinx Serialization | SSE streaming parser real-time: thinking, delta text, tool_calls |
| Markdown & Syntax | Multiplatform Markdown / custom Compose Canvas | Highlighting kode, tabel scrollable, inline chips |
| Web Scraper Lokal | Jsoup 1.18+ | Ekstraksi web search & konversi HTML ke Markdown bersih |
| Penyimpanan Lokal | Room 2.6+ & Jetpack DataStore Preferences | Riwayat sesi, cache percakapan, API key & setelan terenkripsi |
| Manajemen Gambar | Coil 3 + Bitmap Compressor | Pratinjau lampiran chat & konversi ke Base64 (max 1280px, 80% JPEG) |

---

### 4. Desain UI/UX (Kiblat Gemini Apps)
1. **Top App Bar Dinamis**:
   - Kiri: Drawer menu (Riwayat chat, Workspace terdaftar, Settings).
   - Tengah: Pill selector model (`agnes-3.0-flash`, `agnes-3.0-pro`, custom).
   - Kanan: Pill Context Meter (contoh: `14.2K / 1M (1.4%)` dengan warna Hijau/Kuning/Merah).
2. **Area Pesan (Full-width edge-to-edge)**:
   - User message: Kapsul rata kanan.
   - Assistant message: Full-width tanpa bubble sempit.
   - Collapsible Thinking Accordion: "Berpikir selama 3.4 dtk" (collapsible reasoning content).
   - Tool Execution Cards: Status kartu ringkas (contoh: `⚡ Menjalankan: ls -la` atau `🔍 Mencari di web: ...`).
   - Response Footer: Token details (`In: 1.2K | Think: 450 | Out: 820`) + Copy, Regenerate, Open in Canvas.
3. **Floating Input Capsule**:
   - Baris atas: Toggle Chips (`📁 Workspace`, `🌐 Web Search`, `🎨 Canvas`, `⚡ YOLO Mode`).
   - Baris utama: Tombol `+` (Kamera/Galeri/Folder), text field auto-expand, tombol Kirim / Stop.

---

### 5. Spesifikasi Fitur Inti & Tool Calling
1. **Workspace & File System**:
   - Izin `MANAGE_EXTERNAL_STORAGE`.
   - Tool: `list_directory`, `read_file`, `write_file`, `apply_diff`, `search_files`.
2. **Shell Execution**:
   - Native `ProcessBuilder` (`/system/bin/sh -c "<command>"`) dengan cwd ke workspace aktif.
   - Jembatan Termux opsional via direct binary path atau intent.
   - Safety approval: Diff hijau/merah & konfirmasi perintah sebelum dijalankan jika YOLO mode non-aktif.
3. **Interactive Canvas**:
   - Tab 1: Code / Markdown Editor (penomoran baris, targeted modification).
   - Tab 2: Live Preview (WebView terisolasi untuk HTML/CSS/JS dan Markdown).
   - Tab 3: Local Sync (simpan ke workspace file).
4. **Web Search & Reader**:
   - `web_search`: DuckDuckGo Lite HTML scraper atau API key kustom.
   - `read_webpage`: Jsoup parser sanitasi HTML ke Markdown bersih.
5. **Resilience & Rate Limit**:
   - HTTP 429 / 5xx Smart Retry dengan Exponential Backoff + Jitter & header `Retry-After`.
   - Rate limit banner di UI dengan countdown timer dan tombol manual retry/cancel.
   - State preservation agar agent loop tidak hilang jika terkena throttle.
6. **Context Meter & Auto-Compact**:
   - Hitung persentase kapasitas token model.
   - Fitur Compact: Ringkas riwayat percakapan lama ke dalam System Summary ringkas dan arsipkan pesan lama.
