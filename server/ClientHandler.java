package server;

import java.io.*;
import java.net.*;

import javax.swing.SwingUtilities;

import client.GameWindow.GamePanel;

public class ClientHandler implements Runnable {
    private Socket socket;
    private GameServer server;
    private BufferedReader input;
    private PrintWriter output;
    private String clientID; // ID unik untuk setiap client

    public ClientHandler(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;

        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("ClientHandler berhasil diinisialisasi.");
        } catch (IOException e) {
            System.err.println("Error saat menginisialisasi ClientHandler: " + e.getMessage());
            close(); // Pastikan koneksi ditutup jika inisialisasi gagal
        }
    }

    @Override
    public void run() {
        try {
            // Membaca ID unik client
            clientID = input.readLine();
            if (clientID == null || clientID.trim().isEmpty()) {
                System.err.println("Client ID tidak valid. Menutup koneksi.");
                close();
                return;
            }
            System.out.println("Client terhubung dengan ID: " + clientID);

            // Kirim konfirmasi login
            output.println("LOGIN_SUCCESS");

            // Tambahkan client ke server dengan clientID
            server.addClient(clientID, this);

            // Tambahkan pemain ke room, hanya jika ID valid
            if (clientID != null && !clientID.trim().isEmpty()) {
                server.addPlayerToRoom(this);
            } else {
                System.err.println("Client ID tidak valid untuk ditambahkan ke room.");
            }

            // Baca dan proses pesan dari client
            String message;
            while ((message = input.readLine()) != null) {
                System.out.println("Pesan dari " + clientID + ": " + message);

                if (message.startsWith("PLAYER:")) {
                    server.updatePlayerPosition(clientID, message);
                } else if (message.startsWith("UPDATE_ITEM")) {
                    String[] parts = message.split(":");
                    int newX = Integer.parseInt(parts[1]);
                    int newY = Integer.parseInt(parts[2]);
                    server.updateItemPosition(newX, newY); 
                } else if ("LOGOUT".equals(message)) {
                    System.out.println("Client " + clientID + " keluar.");
                    break; // Keluar dari loop
                } else if (message.startsWith("GAME_RESULTS:")) {
                    processGameResultsMessage(message);
                } else {
                    server.broadcastMessage(message, this);
                }
            }
        } catch (IOException e) {
            System.err.println("Koneksi dengan client " + clientID + " terputus: " + e.getMessage());
        } finally {
            close();
        }
    }

    private void processUpdateItemMessage(String message) {
        String[] parts = message.split(":");
        if (parts.length == 3) {
            try {
                int newX = Integer.parseInt(parts[1]);
                int newY = Integer.parseInt(parts[2]);
                server.updateItemPosition(newX, newY);
            } catch (NumberFormatException e) {
                System.err.println("Format angka salah dalam pesan UPDATE_ITEM: " + message);
            }
        } else {
            System.err.println("Format pesan UPDATE_ITEM tidak valid: " + message);
        }
    }
    private void processGameResultsMessage(String message) {
        // Ambil hasil game dari pesan
        String[] results = message.substring(13).split(";");
        SwingUtilities.invokeLater(() -> {
            GamePanel panel = server.getGamePanel();
            if (panel != null) {
                try {
                    panel.showResults(results);
                } catch (Exception e) {
                    System.err.println("Gagal menampilkan hasil game: " + e.getMessage());
                }
            } else {
                System.err.println("GamePanel belum tersedia.");
            }
        });
    }

    public void sendMessage(String message) {
        if (output != null) {
            output.println(message);
        } else {
            System.err.println("Output stream tidak tersedia untuk client: " + clientID);
        }
    }

    private void close() {
        try {
            System.out.println("Menutup koneksi untuk client: " + clientID);

            // Hapus client berdasarkan clientID
            if (clientID != null) {
                server.removeClient(clientID);
                server.getRoomManager().removePlayer(clientID); //Hapus pemain dari room
            }

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("Koneksi berhasil ditutup untuk client: " + clientID);
        } catch (IOException e) {
            System.err.println("Error saat menutup koneksi untuk client: " + clientID + " - " + e.getMessage());
        }
    }

    public String getClientID() {
        return clientID;
    }
}
