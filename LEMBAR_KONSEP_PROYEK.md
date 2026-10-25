# LEMBAR KONSEP PROYEK AWAL

**Mata Kuliah:** TIF708 — Proyek Sistem Informasi (3 SKS)  
**Program Studi:** S1 Teknik Informatika, Fakultas Teknik  
**Perguruan Tinggi:** Universitas Pelita Bangsa  
**Dosen Pengampu:** Andri Firmansyah, S.Kom., M.Kom.  
**Kelas:** I233C  
**Kelompok:** Kelompok 9  
**Tanggal Pengajuan:** 25 Oktober 2026  

---

### 1. Judul atau Nama Proyek

> **PALAPA GO: Rancang Bangun Sistem Asisten Cerdas Berbasis Agen Otonom Multi-Mode dengan Verifikasi Sumber Ilmiah Terbuka dan Workspace Interaktif pada Perangkat Bergerak Android**

- **Nama Aplikasi:** PALAPA GO
- **Kategori Sistem:** *Autonomous Mobile AI Workspace & Cross-Domain Information Retrieval System*
- **Target Sasaran:** Masyarakat umum, pelajar/guru lintas disiplin, pelaku usaha mandiri (UMKM), dan praktisi teknis.

---

### 2. Latar Belakang Masalah

Kemunculan *Large Language Models* (LLM) telah mengubah cara masyarakat mengakses informasi. Namun, pemanfaatan kecerdasan buatan generik pada perangkat ponsel pintar (*mobile*) saat ini menghadapi empat kendala mendasar:

1. **Tingginya Tingkat Halusinasi Rujukan Ilmiah bagi Masyarakat & Pelajar:**  
   Masyarakat awam, siswa sekolah, maupun mahasiswa non-IT kerap menerima jawaban AI yang memuat kutipan, nama peneliti, atau nomor DOI (*Digital Object Identifier*) palsu (*hallucinated citations*). Hal ini berisiko fatal pada karya tulis, tugas akhir, maupun pengambilan keputusan berbasis fakta karena ketiadaan validasi silang ke basis data jurnal resmi.

2. **Friksi Akses Pustaka Ilmiah Internasional:**  
   Basis data riset dunia seperti Semantic Scholar, OpenAlex, dan Crossref menyediakan ratusan juta publikasi ilmiah terbuka (*open access*). Namun, antarmuka pencarian standar menuntut pemahaman sintaks query akademis yang rumit, menyulitkan masyarakat non-akademisi untuk menemukan rujukan ilmiah yang sahih secara langsung lewat ponsel.

3. **Fragmentasi Alat Produktivitas & Kreativitas Digital:**  
   Pengguna ponsel (seperti pelaku UMKM, guru, dan penulis) terpaksa berpindah-pindah (*app-switching*) antara aplikasi tanya-jawab teks, aplikasi generator gambar, dan aplikasi editor dokumen. Tidak tersedia sistem terintegrasi yang memungkinkan pembuatan teks, peninjauan dokumen (*live canvas*), dan sintesis materi promosi visual dalam satu alur kerja terpadu.

4. **Ketiadaan Mekanisme Keamanan *Human-in-the-Loop* pada Manipulasi Berkas:**  
   Aplikasi asisten yang ada di pasaran umumnya beroperasi sebagai kotak hitam (*black box*) atau hanya dapat menjawab dalam bentuk gelembung teks obrolan, tanpa kemampuan menyunting berkas kerja nyata di media penyimpanan lokal dengan transparansi perubahan (*diff inspection*) sebelum disetujui oleh pengguna.

---

### 3. Tujuan dan Manfaat Proyek

#### A. Tujuan Proyek (SMART)
- Mengembangkan aplikasi Android berbasis Kotlin dan Jetpack Compose yang mengintegrasikan mesin inferensi bahasa dengan empat mode kerja spesifik: **Obrolan Umum**, **Riset Pelajar**, **Agen Otonom**, dan **Kreasi Gambar**.
- Mengintegrasikan tiga API repositori ilmiah global (**OpenAlex**, **Semantic Scholar**, dan **Crossref**) secara *real-time* untuk menjamin seluruh kutipan akademis tervalidasi nomor DOI resminya dan dokumen PDF terbuka dapat diunduh langsung.
- Menerapkan arsitektur keamanan *Human-in-the-Loop* melalui sistem persetujuan selisih kode/teks (*Diff Approval Dialog*) dan pembatasan direktori kerja (*sandbox isolation*).
- Menyediakan kanvas dokumen interaktif (*Canvas Mode*) yang mendukung pratinjau rendering Markdown dan Web/HTML tanpa ketergantungan pada editor pihak ketiga.

#### B. Manfaat Proyek

| Sasaran Penerima Manfaat | Bentuk Manfaat Konkret |
| :--- | :--- |
| **Masyarakat Umum & Siswa Non-IT** | Memperoleh jawaban terpercaya yang didukung bukti literatur sahih; terhindar dari disinformasi ilmiah; kemudahan mengekspor format sitasi standar (APA, IEEE, BibTeX) secara instan. |
| **Pelaku Usaha (UMKM) & Penulis** | Menghasilkan materi promosi (teks penawaran + gambar produk) serta menyusun draf dokumen proposal/kontrak kerja langsung dari ponsel via *Canvas Mode*. |
| **Pengembang & Peneliti Akademis** | Menyediakan lingkungan kerja bergerak (*mobile workspace*) untuk inspeksi kode, manipulasi berkas lokal terisolasi, dan otomasi tugas komputasi berskala kecil. |

---

### 4. Calon Pengguna / Organisasi Sasaran (Stakeholder Awal)

| No | Kelompok Pengguna | Karakteristik & Perilaku Penggunaan | Masalah Utama yang Diselesaikan |
| :---: | :--- | :--- | :--- |
| 1 | **Pelajar, Mahasiswa Non-IT, & Guru** | Membutuhkan referensi artikel ilmiah terpercaya untuk tugas, makalah, atau bahan ajar sekolah. | Menghilangkan referensi palsu buatan AI; menyediakan metadata terverifikasi (judul, penulis, tahun, DOI) dan tautan unduh PDF resmi. |
| 2 | **Masyarakat Awam & Konsumen Informasi** | Menggunakan AI untuk konsultasi pengetahuan umum, penerjemahan, dan perangkuman topik sains populer. | Menghadirkan antarmuka percakapan berbahasa Indonesia yang intuitif dengan kutipan sumber yang dapat diuji keasliannya. |
| 3 | **Pelaku UMKM & Kreator Konten** | Memerlukan pembuatan materi promosi, deskripsi katalog, dan aset gambar visual dalam mobilitas tinggi. | Integrasi fungsi *text-to-image*, penyuntingan tata letak draf kanvas, dan ekspor instan tanpa berlangganan banyak aplikasi. |
| 4 | **Pengembang Perangkat Lunak** | Menguji skrip, merapikan struktur direktori berkas proyek, dan mengeksekusi perintah shell lokal. | Eksekusi aman dalam *workspace sandboxing* dengan konfirmasi visual (diff hijau/merah) sebelum perubahan berkas disimpan permanen. |

---

### 5. Ruang Lingkup Awal (Batasan Proyek)

| Di Dalam Ruang Lingkup (*In-Scope*) | Di Luar Ruang Lingkup (*Out-of-Scope*) |
| :--- | :--- |
| 1. Aplikasi berjalan pada sistem operasi Android (min. SDK 26 / Android 8.0 Oreo hingga Android 14/15). | 1. Tidak melatih (*training/fine-tuning*) fondasi model bahasa dari awal; memanfaatkan protokol REST/SSE API model fondasi (OpenAI-compatible / Gemini / DeepSeek via BYOK - *Bring Your Own Key*). |
| 2. Fitur **Mode Pelajar** dengan integrasi langsung ke Semantic Scholar API, OpenAlex API, dan Crossref DOI Verification Tool. | 2. Tidak melayani pengunduhan artikel berbayar (*paywalled*); pengunduhan dibatasi pada berkas berlisensi *Open Access*. |
| 3. Fitur **Mode Agen Otonom** dengan sistem persetujuan berkas *Human-in-the-Loop* (Diff Dialog), pembacaan direktori lokal, dan eksekusi skrip terminal terisolasi. | 3. Tidak memerlukan hak akses *root* sistem pada perangkat Android pengguna. |
| 4. Fitur **Mode Kreasi Gambar** untuk generasi visual berbasis teks dan **Mode Canvas** untuk manipulasi dokumen interaktif (HTML & Markdown). | 4. Tidak menyediakan layanan komputasi awan mandiri untuk penyimpanan data pengguna (seluruh data percakapan tersimpan secara lokal via Room Database di perangkat). |
| 5. Mekanisme ketahanan jaringan: *Smart Exponential Backoff Retry* untuk menangani batas laju permintaan (*Rate Limit* HTTP 429). | |

---

### 6. Anggota Tim dan Pembagian Peran Sementara

| No | Peran Proyek (*Project Role*) | Nama Anggota & NIM | Tanggung Jawab Utama dalam SDLC |
| :---: | :--- | :--- | :--- |
| 1 | **Lead System Architect & Core Developer** | **[Gusti Aditya Muzaky]**<br>NIM: [312310193] | - Merancang arsitektur perangkat lunak (Clean Architecture + MVVM).<br>- Mengimplementasikan orkestrasi agen, integrasi 3 API ilmiah (Semantic Scholar, OpenAlex, Crossref), SSE Parser, dan Room DB.<br>- Memimpin manajemen *codebase* dan *repository* Git. |
| 2 | **System Analyst & Business Requirements** | **[M Faizal Akbar]**<br>NIM: [312310666] | - Menyusun dokumen spesifikasi kebutuhan perangkat lunak (SRS).<br>- Memodelkan sistem menggunakan diagram UML (Use Case, Activity Diagram, Sequence Diagram) dan DFD (*Data Flow Diagram*).<br>- Melakukan analisis kelayakan fungsional fitur Mode Pelajar & Workspace. |
| 3 | **UI/UX Designer & Information Architect** | **[Rizky Fadil Hanif]**<br>NIM: [312310205] | - Merancang *wireframe*, tata letak Bento Card, dan prototipe interaktif di Figma.<br>- Menetapkan hierarki visual, kontras warna Material 3 Expressive, serta panduan tipografi.<br>- Melakukan evaluasi *Usability Testing* menggunakan kuesioner SUS (*System Usability Scale*). |
| 4 | **Quality Assurance (QA) & Security Tester** | **[Arizal Hardiansyah]**<br>NIM: [312310201] | - Menyusun matriks pengujian fungsional (*Black Box Testing* & *Boundary Value Analysis*).<br>- Menguji protokol keamanan direktori (*Path Traversal*) dan validasi dialog persetujuan *diff*.<br>- Mendokumentasikan catatan temuan *bug* (*Defect Log*) dan verifikasi uji laju batas (HTTP 429). |
| 5 | **Technical Writer & Project Coordinator** | **[Siti Aisyah]**<br>NIM: [3123?????] | - Mengelola linimasa proyek (*Gantt Chart*) dan matriks rincian kerja (*Work Breakdown Structure*).<br>- Menyusun dokumen Panduan Pengguna (*User Manual Guide*) untuk masyarakat awam & UMKM.<br>- Mengompilasi dan mengedit laporan akhir proyek sesuai pedoman akademik UPB. |

---

### Panduan Pembagian Penjelasan Saat Presentasi Kelompok

- **Lead Developer & Architect:** Menjelaskan fondasi teknis, arsitektur Clean Architecture + MVVM, integrasi multi-API (Semantic Scholar, OpenAlex, Crossref), SSE stream, dan demonstrasi aplikasi langsung pada perangkat Android.
- **System Analyst:** Menjelaskan diagram sistem (Use Case, Activity Diagram, DFD), batas ruang lingkup, dan relasi kebutuhan pengguna lintas domain (masyarakat, pelajar, UMKM, developer) terhadap fitur aplikasi.
- **UI/UX Designer:** Menjelaskan desain antarmuka, prinsip Material 3 Expressive, pemilihan Bento Card, Bottom Sheet navigasi cepat, dan umpan balik visual (*ambient edge shimmer*).
- **QA Tester:** Menjelaskan skenario pengujian fungsional (*black box*), pengujian keamanan *path traversal*, persetujuan *diff* dokumen, dan ketahanan *retry* saat terjadi batas laju (HTTP 429).
- **Technical Writer / Coordinator:** Menjelaskan linimasa pengerjaan proyek (*Gantt Chart*), panduan pengguna untuk masyarakat umum non-IT, serta struktur kompilasi laporan akhir.
