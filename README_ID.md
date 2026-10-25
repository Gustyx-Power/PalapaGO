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

Setelah membuka aplikasi, masuk ke menu Pengaturan untuk mengonfigurasi layanan model bahasa:
1. **API Base URL**: Tentukan alamat endpoint API target (misal penyedia resmi atau server proxy lokal).
2. **API Key**: Masukkan kunci API pribadi Anda.
3. **Model**: Masukkan pengenal model yang dituju (contoh: `gemini-2.0-flash`, `deepseek-chat`, atau `gpt-4o`).
4. **Max Retries**: Atur batas pengulangan panggilan jaringan saat koneksi tidak stabil.

---

## Lisensi

Proyek ini dilisensikan di bawah Lisensi MIT. Rincian ketentuan lisensi dapat dibaca pada berkas [LICENSE](LICENSE).
