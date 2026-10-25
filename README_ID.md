# Palapa GO

[English](README.md) | **Bahasa Indonesia**

Palapa GO adalah sistem asisten kecerdasan buatan berbasis agen otonom, mesin riset akademis, dan ruang kerja komputasi bergerak untuk platform Android. Dibangun menggunakan Kotlin 2.0 dan Jetpack Compose (Material 3), aplikasi ini menghubungkan model bahasa fondasi dengan akses penyimpanan lokal, mesin pencarian basis data ilmiah internasional, serta perkakas eksekusi berkas terisolasi langsung dari ponsel pintar.

---

## Fitur Utama dan Mode Operasional

### 1. Mode Obrolan (Chat Mode)
- Penerimaan respons secara langsung melalui mekanisme *Server-Sent Events* (SSE) dengan latensi rendah.
- Penyimpanan riwayat obrolan dan sesi secara luring (*offline*) memanfaatkan Android Room Database.
- Dukungan tampilan Markdown lengkap mencakup tabel, penyorotan sintaks kode, dan fungsi penyalinan teks.

### 2. Mode Pelajar (Riset Akademis)
- Agregasi pencarian paralel pada tiga basis data ilmiah dunia: OpenAlex, Semantic Scholar, dan Crossref.
- Verifikasi nomor DOI (*Digital Object Identifier*) secara langsung untuk mengeliminasi halusinasi sitasi pada jawaban AI.
- Pembuatan kutipan pustaka otomatis dengan format standar internasional (APA, IEEE, dan BibTeX).
- Deteksi ketersediaan serta pengunduhan naskah lengkap artikel ilmiah berlisensi terbuka (*Open Access PDF*).

### 3. Mode Agen (Ruang Kerja Otonom)
- Manipulasi berkas lokal yang dibatasi secara ketat di dalam direktori kerja (*workspace*) aktif pengguna.
- Sistem persetujuan perubahan berkas *Human-in-the-Loop* yang menampilkan perbandingan selisih teks (*diff viewer*) secara transparan sebelum berkas disimpan.
- Eksekusi perintah terminal terisolasi dengan pembatasan panjang keluaran teks guna mencegah ledakan token konteks.
- Pencegahan otomatis terhadap upaya manipulasi direktori di luar batas izin (*path traversal attack*).

### 4. Mode Kanvas (Canvas Mode)
- Ruang kerja penyusunan dokumen dan tata letak visual secara berdampingan.
- Pratinjau langsung konten web berbasis HTML, CSS, dan skrip JavaScript interaktif.
- Penyuntingan serta ekspor dokumen format Markdown.

### 5. Mode Kreasi Gambar (Image Generation Mode)
- Pembuatan aset visual berbasis instruksi teks untuk keperluan materi presentasi, ilustrasi, atau materi promosi.
- Pilihan rasio aspek gambar yang dapat disesuaikan dan ekspor langsung ke galeri perangkat.

---

## Keamanan dan Ketahanan Sistem

- **Tata Kelola Human-in-the-Loop**: Seluruh operasi penulisan berkas, penerapan *patch*, dan eksekusi skrip terminal memerlukan konfirmasi eksplisit pengguna melalui dialog persetujuan, kecuali jika mode otonom penuh diaktifkan secara sengaja.
- **Smart Retry Interceptor**: Mekanisme penundaan eksponensial (*exponential backoff with full jitter*) untuk menangani pembatasan laju permintaan (*rate limit* HTTP 429) serta gangguan peladen upstream (HTTP 5xx).
- **BYOK (Bring Your Own Key)**: Manajemen kredensial mandiri yang memungkinkan pengguna memasukkan kunci API, URL dasar, dan nama model pilihan (OpenAI-compatible, Google Gemini, DeepSeek). Data kredensial tersimpan aman di penyimpanan lokal melalui Jetpack DataStore terenkripsi.

---

## Spesifikasi Teknis

- **Sistem Operasi**: Android 8.0 (API level 26) atau versi di atasnya
- **Bahasa Pemrograman**: Kotlin 2.0+
- **Antarmuka Pengguna**: Jetpack Compose dengan Material 3 Expressive tokens
- **Pola Arsitektur**: Clean Architecture dengan Unidirectional Data Flow (UDF) dan MVVM
- **Penyimpanan Lokal**: Room Database (SQLite) dan Jetpack DataStore Preferences
- **Jaringan**: OkHttp 4.12 dengan custom SSE parser dan retry interceptor
- **Serialisasi**: kotlinx.serialization

---

## Kompilasi dan Pemasangan

### Prasyarat
- Java Development Kit (JDK) versi 17 atau yang lebih baru
- Android SDK (API level 34 atau 35 direkomendasikan)
- Android Debug Bridge (ADB)

### Perintah Kompilasi

Untuk mengompilasi berkas APK versi pengembangan (*debug*):
```bash
./gradlew assembleDebug
```

Pada Command Prompt atau PowerShell di Windows:
```powershell
.\gradlew.bat assembleDebug
```

Berkas APK hasil kompilasi akan berada di direktori:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Pemasangan ke Perangkat

Pastikan fitur USB Debugging telah aktif di perangkat Android Anda:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Konfigurasi Aplikasi

### Kunci API Waktu Kompilasi (`local.properties`)
Untuk mengaktifkan backend inferensi bawaan dan mesin pencarian jurnal ilmiah saat proses kompilasi, buat berkas `local.properties` di direktori utama proyek berdasarkan [local.properties.example](local.properties.example):

```bash
cp local.properties.example local.properties
```

Isi kredensial yang dibutuhkan sesuai layanan yang Anda gunakan:
```properties
## Android SDK
sdk.dir=/path/to/Android/Sdk

## Backend Inferensi AI (Agnes AI / OpenAI-compatible / Gemini / Groq)
AGNES_API_KEY=your_agnes_api_key_here
AGNES_BASE_URL=https://apihub.agnes-ai.com/v1
GEMINI_API_KEY=your_gemini_api_key_here
GROQ_API_KEY=your_groq_api_key_here

## API Pustaka Ilmiah (Mode Pelajar)
SEMANTIC_SCHOLAR_API_KEY=your_semantic_scholar_api_key_here
OPENALEX_MAILTO=your_email@example.com

## Mesin Pencarian Web
TAVILY_API_KEY=your_tavily_api_key_here
```

### Pengaturan Waktu Jalan (BYOK In-App)
Pengguna juga dapat menimpa atau menyediakan kredensial API secara dinamis langsung dari dalam antarmuka aplikasi:
1. Buka layar **Pengaturan** melalui ikon di bilah atas (*Top Bar*).
2. Sesuaikan **API Base URL**, **API Key**, dan **Model** (contoh: `gemini-2.0-flash`, `deepseek-chat`, atau `gpt-4o`).
3. Atur **Max Retries** untuk menghadapi koneksi yang fluktuatif. Seluruh kredensial runtime tersimpan aman di penyimpanan lokal perangkat.

---

## Lisensi

Proyek ini dilisensikan di bawah Lisensi MIT. Rincian ketentuan lisensi dapat dibaca pada berkas [LICENSE](LICENSE).
