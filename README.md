# 🎮 Game Multiplayer: Collect Item

## 📌 Introduction
Game Multiplayer Collect Item adalah sebuah permainan berbasis Java Swing yang memungkinkan beberapa pemain (minimal 3 pemain) untuk terhubung dalam satu jaringan dan saling berkompetisi mengumpulkan item sebanyak-banyaknya dalam batas waktu tertentu. Game ini menekankan aspek konektivitas client-server dan komunikasi real-time antar pemain.

---

## 🕹️ Deskripsi Game
Dalam game ini, setiap pemain akan mengontrol karakter mereka sendiri dalam sebuah arena permainan. Adapun tujuannya meliputi:
- Menggerakkan karakter ke berbagai arah (atas, bawah, kiri, kanan) menggunakan keyboard.
- Mengumpulkan item (kotak kuning) yang muncul secara acak.
- Meningkatkan skor setiap kali pemain berhasil menyentuh item.
- Memperebutkan skor tertinggi sebelum waktu permainan habis.

Game ini mendukung multiplayer LAN (Local Area Network) dengan komunikasi menggunakan socket TCP.

---

## 👥 Team Members  
Proyek ini dikerjakan oleh tim yang terdiri dari:  
1. **Suko Dwi Atmojo** - 2231740034
2. **Aryuda Firmansah** - 2231740004
3. **Fatikha Hudi Aryani** - 2231740029
4. **Luluk Musyarrofah** - 2231740038

### Pembagian Tugas  
- **Suko Dwi Atmojo**:  
  - Merancang dan mengimplementasikan logika client-side untuk menghubungkan client ke server, mengirim posisi pemain, dan menerima leaderboard.  
  - Merancang jendela permainan dengan panel game di dalamnya.  
- **Aryuda Firmansah**:
  - Menyediakan GUI permainan dan logika client untuk gameplay.
  - Menrancang Panel GUI ruang tunggu sebelum game dimulai
- **Fatikha Hudi Aryani**:
  - Merancang dan mengimplementasikan logika server-side untuk menangani koneksi client, sinkronisasi data, dan koordinasi gameplay multiplayer.
  - Menangani tiap client yang terhubung ke server (1 thread per client).
  - Mengatur socket server dan menerima client baru.
  - Menyusun dokumentasi project dalam bentuk README.md.
- **Luluk Musyarrofah**:
  - Merancang dan mengimplementasikan sistem autentikasi.
  - Mengelola autentikasi pemain dan manajemen ruang (room/lobby multiplayer).
  - Mengelola daftar pemain dan status ruang tunggu (waiting room).

---

## 🛠️ Tools & Tech Stack  
Game ini dikembangkan menggunakan:  
- **Bahasa Pemrograman**: Java  
- **Version Control**: Git & GitHub  

---

## 🎮 How to Play

1. Buka folder project dan jalankan program.
2. Jika sebagai **server**:
   - Jalankan program GameServer.java pada folder server maka secara otomatis akan menampilkan waiting room (ruang tunggu pemain).
     ![Screenshot_20250626_203138_Gallery 1](https://github.com/user-attachments/assets/10fc1e2b-1856-42b9-ac9d-ef0a4623e0e6)
   - Tunggu hingga semua pemain terhubung (minimal 3 pemain).
   - Tekan button **Mulai Permainan** ketika semua pemain sudah terhubung.
4. Jika sebagai **client**:
   - Jalankan program GameWindow.java atau GameClient.java (pilih salah satu) yang terdapat pada folder client.
   - Pilih mode **client**, lalu klik button **OK**.
   - Masukkan IP Server, dan nama pemain. Lalu klik button **Login**.
   - Apabila login berhasil, maka akan ditampilkan pesan **Login berhasil sebagai nama_pemain!**
   - Lalu klik button **OK** maka akan otomatis diarahkan ke halaman waiting room.
   - Tunggu di halaman waiting room hingga server memulai permainan.
5. Setelah server menekan button **Mulai Permainan**, maka semua pemain akan diarahkan pada GUI permainan.
6. Setiap pemain dapat mengontrol karakter mereka sendiri dalam sebuah arena permainan dalam batas waktu tertentu seperti menggerakkan karakter ke berbagai arah (atas, bawah, kiri, kanan) menggunakan keyboard, dan mengumpulkan item (kotak kuning) yang muncul secara acak sebanyak-banyaknya untuk meningkatkan skor.
7. Kontrol karakter menggunakan tombol:
   - `W` / `↑` untuk atas
   - `S` / `↓` untuk bawah
   - `A` / `←` untuk kiri
   - `D` / `→` untuk kanan
9. Skor akan ditampilkan secara real-time.
10. Setelah waktu habis, leaderboard ditampilkan berdasarkan perolehan skor pemain.

---

## 📁 Struktur & Fungsi Folder

### 🔐 `auth/`
- **AuthHandler.java**: Menangani login dan autentikasi pemain.
- **RoomManager.java**: Mengatur status ruang tunggu dan daftar pemain aktif.

### 🖥️ `server/`
- **GameServer.java**: Kelas utama untuk menjalankan server socket.
- **ClientHandler.java**: Menangani koneksi setiap client secara terpisah.
- **SimpleServer.java**: Server versi ringan untuk testing/debugging.

### 🎮 `client/`
- **GameClient.java**: Koneksi ke server, komunikasi, dan inisiasi game.
- **GamePanel.java**: Panel utama permainan (tampilan, kontrol, skor).
- **GameWindow.java**: Frame utama GUI client.
- **WaitingRoomPanel.java**: Antarmuka ruang tunggu sebelum game dimulai.
- **LoginUtil.java**: Fungsi bantu validasi login.

---

## ▶️ Cara Menjalankan Game

1. Pastikan semua perangkat berada dalam satu jaringan LAN.
2. **Kompilasi semua file Java**:
   ```bash
   javac auth/*.java client/*.java server/*.java
3. Pada sisi **server**:
   - Jalankan program GameServer.java yang terdapat pada folder server maka secara otomatis akan menampilkan waiting room sebagai server-side.
     
   - Tunggu hingga semua pemain terhubung (minimal 3 pemain).
     
   - Apabila semua pemain sudah terhubung, tekan button **Mulai Permainan**.
     
5. Pada sisi **client**:
   - Jalankan program GameClient.java atau GameWindow.java  (pilih salah satu) yang terdapat pada folder client.
   - Pilih mode **client**, lalu klik button **OK**.
   - Masukkan IP Server, dan nama pemain. Lalu klik button **Login**.
   - Apabila login berhasil, maka akan ditampilkan pesan **Login berhasil sebagai nama_pemain!**
   - Lalu klik button **OK** maka akan otomatis diarahkan ke halaman waiting room.
   - Tunggu di halaman waiting room hingga server memulai permainan.
7. Setelah server menekan button **Mulai Permainan**, maka semua pemain akan diarahkan pada GUI permainan.
8. Setelah waktu habis, leaderboard ditampilkan berdasarkan perolehan skor pemain. 
   
---

## 🌐 Link Demo
Berikut ini video demo game yang telah kami buat melalui tautan berikut:
Demo Select Item Game []

Catatan: Pastikan jaringan Anda mendukung koneksi ke server.
