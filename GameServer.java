package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import auth.AuthHandler; // Import AuthHandler
import auth.RoomManager; // Import RoomManager
import client.GameWindow;
import client.GameWindow.GamePanel;
import client.WaitingRoomPanel;

public class GameServer {
    private GamePanel gamePanel;

    private int playerX = 100; // Posisi awal X
    private int playerY = 100; // Posisi awal Y
    private int itemX = (int) (Math.random() * 600);
    private int itemY = (int) (Math.random() * 600);
    private boolean up, down, left, right;
    private Timer moveTimer;
    private int animX = -100, animY = -100; // Posisi animasi
    private float animAlpha = 0f; // Transparansi animasi (0 = tak terlihat, 1 = penuh)

    private ServerSocket serverSocket;
    private final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, int[]> playerPositions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> playerScores = new ConcurrentHashMap<>();
    public AuthHandler authHandler;
    public RoomManager roomManager;
    private WaitingRoomPanel waitingRoomPanel;
    private AtomicInteger connectedPlayers = new AtomicInteger(1); // Hitung server sebagai pemain pertama

    public GameServer(int port) {
        moveTimer = new Timer(30, e -> {
            boolean moved = false;
        
            if (up) {
                playerY -= 5;
                moved = true;
            }
            if (down) {
                playerY += 5;
                moved = true;
            }
            if (left) {
                playerX -= 5;
                moved = true;
            }
            if (right) {
                playerX += 5;
                moved = true;
            }
        
            if (moved) {
                // Periksa tabrakan dengan item
                if (Math.abs(playerX - itemX) < 30 && Math.abs(playerY - itemY) < 30) {
                    addScore("Server", 1);
                    updateItemPosition((int) (Math.random() * 600), (int) (Math.random() * 600));
                }
        
                // Broadcast posisi pemain server ke semua klien
                broadcastMessage("PLAYER_POSITION:Server:" + playerX + ":" + playerY, null);
            }
        });
        moveTimer.start();
        
        try {
            serverSocket = new ServerSocket(port);
            authHandler = new AuthHandler();
            roomManager = new RoomManager();
            System.out.println("Server berjalan di port " + port);

            // Inisialisasi posisi awal server
            playerPositions.put("Server", new int[]{playerX, playerY});
            
            // Inisialisasi GUI Waiting Room
            SwingUtilities.invokeLater(() -> {
                JFrame waitingFrame = new JFrame("Waiting Room (Server)");
                waitingRoomPanel = new WaitingRoomPanel(e -> {
                    // Server memulai permainan
                    broadcastMessage("START_GAME", null);

                    // Berpindah ke game screen untuk server
                    SwingUtilities.invokeLater(() -> {
                        JFrame gameFrame = new JFrame("Game Collect Item Multiplayer (Server)");
                        GameWindow.GamePanel gamePanel = new GameWindow.GamePanel(this); // Server tidak menggunakan koneksi Client
                        
                        gamePanel.addKeyListener(new KeyAdapter() {
                            @Override
                            public void keyPressed(KeyEvent e) {
                                int key = e.getKeyCode();
                                if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP) gamePanel.moveUp();
                                if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN) gamePanel.moveDown();
                                if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT) gamePanel.moveLeft();
                                if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT) gamePanel.moveRight();
                            }
                    
                            @Override
                            public void keyReleased(KeyEvent e) {
                                // Tambahkan logika untuk berhenti jika diperlukan
                            }

                        });

                        // Timer untuk terus merender ulang GUI setiap 30ms
                        new Timer(30, event -> gamePanel.repaint()).start();
                        gameFrame.add(gamePanel);
                        gameFrame.setSize(600, 600);
                        gameFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                        gameFrame.setVisible(true);

                        gamePanel.setFocusable(true);
                        gamePanel.requestFocusInWindow();
                        System.out.println("GamePanel fokus: " + gamePanel.isFocusOwner());
                    });
                });
                waitingFrame.add(waitingRoomPanel);
                waitingFrame.setSize(400, 300);
                waitingFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                waitingFrame.setVisible(true);
            });

            acceptClients();
        } catch (IOException e) {
            System.err.println("Gagal memulai server: " + e.getMessage());
        }
    }


    private void acceptClients() {
        List<String> players = new ArrayList<>();
        players.add("Server"); // Tambahkan Server sekali di awal
    
        SwingUtilities.invokeLater(() -> {
            waitingRoomPanel.updatePlayerList(players.toArray(new String[0]));
            waitingRoomPanel.updateWaitingStatus(players.size() + "/3 players connected");
        });
    
        broadcastMessage("ROOM_STATUS:" + players.size() + "/3 players connected;" + String.join(",", players), null);
    
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                System.out.println("Client baru terhubung dari: " + socket.getInetAddress());
    
                // Buat handler client
                ClientHandler clientHandler = new ClientHandler(socket, this);
                new Thread(clientHandler).start();
    
                // Tunggu client mengirim ID atau login
                // Di dalam ClientHandler kamu harus memastikan ID client sudah diset setelah login
                while (clientHandler.getClientID() == null) {
                    Thread.sleep(100); // tunggu sampai client ID tersedia
                }
    
                String clientID = clientHandler.getClientID();
    
                // Tambahkan ke daftar pemain jika belum ada
                synchronized (players) {
                    if (!players.contains(clientID)) {
                        players.add(clientID);
                    }
                }
    
                SwingUtilities.invokeLater(() -> {
                    waitingRoomPanel.updatePlayerList(players.toArray(new String[0]));
                    waitingRoomPanel.updateWaitingStatus(players.size() + "/3 players connected");
                });
    
                broadcastMessage("ROOM_STATUS:" + players.size() + "/3 players connected;" + String.join(",", players), null);
    
                // Jika sudah 3 pemain (termasuk server), mulai game
                if (players.size() >= 3) {
                    broadcastMessage("START_GAME", null);
                    break;
                }
    
            } catch (IOException | InterruptedException e) {
                System.err.println("Gagal menerima client: " + e.getMessage());
            }
        }
    }    
    

    public void addClient(String clientID, ClientHandler clientHandler) {
        if (clientID == null || clientHandler == null) {
            System.err.println("Client ID atau handler tidak valid untuk penambahan.");
            return;
        }

        clients.put(clientID, clientHandler);
        System.out.println("Client ditambahkan: " + clientID);
    }

    public void removeClient(String clientID) {
        if (clientID == null || !clients.containsKey(clientID)) {
            System.err.println("Client ID tidak ditemukan: " + clientID);
            return;
        }

        clients.remove(clientID);
        System.out.println("Client dihapus: " + clientID);
    }
    
    public void updatePlayerPosition(String clientID, String positionData) {    
        String[] parts = positionData.split(":");
        int x = Integer.parseInt(parts[1]);
        int y = Integer.parseInt(parts[2]);
        playerPositions.put(clientID, new int[]{x, y});
    
        if (Math.abs(x - itemX) < 20 && Math.abs(y - itemY) < 20) {
            playerScores.put(clientID, playerScores.getOrDefault(clientID, 0) + 1);
            updateItemPosition((int) (Math.random() * 600), (int) (Math.random() * 600));
        }

    }
    
    public void broadcastMessage(String message, ClientHandler sender) {   
        if (message == null || message.isEmpty()) {
            System.err.println("Pesan broadcast kosong, tidak dikirim.");
            return;
        }

        for (ClientHandler client : clients.values()) {
            try {
                if (client != sender) {
                    client.sendMessage(message);
                }
            } catch (Exception e) {
                System.err.println("Gagal mengirim pesan ke client: " + e.getMessage());
            }
        }
        System.out.println("Pesan dikirim ke semua client: " + message);
    }

    public RoomManager getRoomManager() {
        return roomManager;
    }

    public void removePlayerFromRoom(String clientID) {
        if (clientID != null && roomManager != null) {
            roomManager.removePlayer(clientID);
            System.out.println("Pemain dihapus dari room: " + clientID);
        } else {
            System.err.println("Gagal menghapus player: clientID atau roomManager null");
        }
    }
    

    public synchronized void addPlayerToRoom(ClientHandler clientHandler) {
        String clientID = clientHandler.getClientID();
        if (clientID == null) {
            System.err.println("Client ID null, tidak bisa menambahkan ke room.");
            return;
        }
    
        String roomID = roomManager.addPlayerToRoom(clientID);
        if (roomID == null) {
            System.err.println("Room ID null, pemain tidak bisa ditambahkan.");
            return;
        }
    
        clientHandler.sendMessage("JOINED_ROOM:" + roomID);
    
        List<String> playersInRoom = roomManager.getRooms().get(roomID);
        if (playersInRoom != null) {
            // Kirim status ruang ke semua pemain di ruangan
            for (String player : playersInRoom) {
                ClientHandler handler = clients.get(player);
                if (handler != null) {
                    handler.sendMessage("ROOM_STATUS:" + playersInRoom.size() + " players connected");
                }
            }
    
            // Jika cukup pemain, kirim sinyal untuk memulai game
            if (roomManager.isRoomReady(roomID)) {
                for (String player : playersInRoom) {
                    ClientHandler handler = clients.get(player);
                    if (handler != null) {
                        handler.sendMessage("START_GAME");
                    }
                }
            }
        } else {
            System.err.println("Room dengan ID " + roomID + " tidak ditemukan.");
        }
    }    

    public synchronized void updateGameState(String playerID, int x, int y) {
        // Perbarui posisi pemain
        playerPositions.put(playerID, new int[]{x, y});

        // Deteksi tabrakan dengan item
        int[] itemPosition = playerPositions.get("Item");
        if (Math.abs(x - itemPosition[0]) < 30 && Math.abs(y - itemPosition[1]) < 30) {
            playerScores.put(playerID, playerScores.getOrDefault(playerID, 0) + 1);
            // Pindahkan item ke posisi baru
            itemPosition[0] = (int) (Math.random() * 600);
            itemPosition[1] = (int) (Math.random() * 600);
        }

        // Kirimkan keadaan permainan ke semua client
        broadcastGameState();
    }

    private void broadcastGameState() {
        StringBuilder state = new StringBuilder("GAME_STATE:");
        state.append(itemX).append(":").append(itemY);

        for (String player : playerScores.keySet()) {
            state.append(":").append(player).append(":").append(playerScores.get(player));
        }

        broadcastMessage(state.toString(), null);
    }
    
    public void addScore(String playerID, int score) {
        playerScores.put(playerID, playerScores.getOrDefault(playerID, 0) + score);
        System.out.println("Skor pemain " + playerID + " sekarang: " + playerScores.get(playerID));
    }

    public void updateItemPosition(int x, int y) {
        itemX = x;
        itemY = y;
        broadcastMessage("UPDATE_ITEM:" + itemX + ":" + itemY, null);
    }       

    private void checkCollisionWithItem() {
        if (Math.abs(playerX - itemX) < 20 && Math.abs(playerY - itemY) < 20) {
            playerScores.put("Server", playerScores.getOrDefault("Server", 0) + 1);
            itemX = (int) (Math.random() * 600);
            itemY = (int) (Math.random() * 600);
    
            // Mulai animasi skor
            animX = playerX;
            animY = playerY;
            animAlpha = 1.0f;
    
            broadcastMessage("UPDATE_ITEM:" + itemX + ":" + itemY, null);
            System.out.println("Server skor bertambah menjadi: " + playerScores.get("Server"));
    
            gamePanel.repaint(); // Pastikan panel di-refresh
        }
    }

    public GamePanel getGamePanel() {
        return this.gamePanel;
    }    

    private List<String> getLeaderboard() {
        List<String> leaderboard = new ArrayList<>(playerScores.keySet());
        leaderboard.sort((a, b) -> playerScores.get(b) - playerScores.get(a)); // Urutkan berdasarkan skor
        return leaderboard;
    }    

    private void broadcastLeaderboard() {
        // Buat pesan leaderboard berdasarkan skor pemain
        StringBuilder leaderboard = new StringBuilder("LEADERBOARD:");
        playerScores.entrySet().stream()
            .sorted((a, b) -> b.getValue() - a.getValue()) // Urutkan dari skor tertinggi ke terendah
            .forEach(entry -> leaderboard.append(entry.getKey()).append(",").append(entry.getValue()).append(";"));

        // Kirimkan ke semua client
        broadcastMessage(leaderboard.toString(), null);
    }

    private void showGameResults() {
        List<String> leaderboard = getLeaderboard();
        StringBuilder resultMessage = new StringBuilder("Hasil Akhir:\n\n");

        int rank = 1;
        for (String player : leaderboard) {
            int score = playerScores.getOrDefault(player, 0);
            resultMessage.append(rank++).append(". ").append(player).append(" - ").append(score).append(" poin\n");
        }

        // Tampilkan di server
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, resultMessage.toString(), "Game Over", JOptionPane.INFORMATION_MESSAGE);
        });

        broadcastLeaderboard(); // <- pastikan ini dipanggil
    }

    public Map<String, Integer> getPlayerScores() {
        return playerScores;
    }
   

    public static void main(String[] args) {
        int port = 1234;
        try {
            new GameServer(port);
        } catch (Exception e) {
            System.err.println("Kesalahan fatal saat menjalankan server: " + e.getMessage());
        }
    }
}
