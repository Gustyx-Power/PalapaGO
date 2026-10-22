# Fine-tuning PalapaGO

Perubahan `AgentSystemPrompt.kt` mengubah instruksi saat inferensi, bukan bobot model.
Repository ini belum menyediakan bukti endpoint fine-tuning pada provider Agnes.
Sebelum mengirim dataset atau membuat job berbayar, tentukan provider, base model yang
mendukung fine-tuning, dan dataset yang boleh digunakan.

## Dataset

Gunakan JSONL, satu percakapan per baris, dengan bentuk:

```json
{"messages":[{"role":"system","content":"You are PalapaGO, an Android workspace assistant."},{"role":"user","content":"Bagaimana menampilkan kode Kotlin di Markdown?"},{"role":"assistant","content":"Gunakan fenced code block dengan bahasa kotlin:\n\n```kotlin\nval message = \"Halo\"\nprintln(message)\n```"}]}
```

Contoh di atas hanya menunjukkan format, bukan dataset latihan yang memadai.
Kumpulkan percakapan yang telah dikurasi untuk Bahasa Indonesia, kode, sitasi,
penggunaan tools, dan pemulihan dari kegagalan. Hapus API key, token, identitas
pribadi, isi workspace privat, dan transkrip reasoning internal sebelum ekspor.
Jangan memakai hasil agent yang belum diverifikasi sebagai jawaban target.

Pisahkan train dan validation berdasarkan tugas/proyek sebelum membuat variasi
prompt untuk mencegah kebocoran. Simpan set evaluasi terpisah yang tidak dilatih.
Validasi struktur menggunakan `python3 training/validate_dataset.py data.jsonl`.
Sesuaikan format tool calls dengan dokumentasi provider yang dipilih.

## Evaluasi sebelum mengganti model produksi

- Markdown: code fence utuh, indentation terjaga, tabel dengan escaped pipe,
  heading valid, tautan sumber dapat ditelusuri.
- Agent: memilih tool sesuai tugas, argumen valid, mematuhi workspace dan approval,
  tidak mengklaim tool/test berhasil saat hasilnya gagal.
- Bahasa: jawaban Indonesia ringkas, tidak mengulang status atau log tools.
- Ukur baseline dan model hasil latihan dengan tugas yang sama: keberhasilan,
  kesalahan faktual, latensi, dan biaya token.

Upload file, pembuatan job, pemantauan status, dan deployment model harus mengikuti
API provider terpilih. Simpan ID job, versi dataset, konfigurasi latihan, dan skor
evaluasi; jangan mengganti model aplikasi hanya karena job sudah selesai.
