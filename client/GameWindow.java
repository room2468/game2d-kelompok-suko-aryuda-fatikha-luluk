package client;

import javax.swing.*;

import server.GameServer;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GameWindow extends JFrame {
    private GamePanel panel;

    public GameWindow(GameClient client) {
        setTitle("Collect Item Game Multiplayer");
        setSize(600, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        panel = new GamePanel(client);
        add(panel);

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String mode = JOptionPane.showInputDialog(
                null,
                "Pilih mode (server/client):",
                "Mode Aplikasi",
                JOptionPane.QUESTION_MESSAGE
            );
    
            if ("server".equalsIgnoreCase(mode)) {
                try {
                    // Server tidak perlu login, langsung menggunakan localhost
                    GameClient client = new GameClient("localhost", 1234);
            
                    JOptionPane.showMessageDialog(null, "Server berjalan!");
                    
                    // Tampilkan ruang tunggu
                    JFrame waitingFrame = new JFrame("Waiting Room");
                    WaitingRoomPanel waitingRoom = new WaitingRoomPanel(e -> {
                        // Hanya server yang dapat memulai game
                        client.getOutput().println("START_GAME_REQUEST");
                    });
                    waitingFrame.add(waitingRoom);
                    waitingFrame.setSize(400, 300);
                    waitingFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    waitingFrame.setVisible(true);
            
                    // Jalankan metode listenForUpdates
                    client.listenForUpdates(waitingRoom);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null, "Gagal menjalankan server: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else if ("client".equalsIgnoreCase(mode)) {
                JFrame loginFrame = new JFrame("Login Game");
                loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                loginFrame.setSize(400, 250); // Tinggi diperbesar untuk 3 input field
            
                // Panel dan layout
                JPanel panel = new JPanel(new GridLayout(4, 2, 10, 10));
            
                // Komponen input
                JLabel ipLabel = new JLabel("Server IP:");
                JTextField ipField = new JTextField();
            
                JLabel portLabel = new JLabel("Port:");
                JTextField portField = new JTextField("1234");
            
                JLabel nameLabel = new JLabel("Nama Pemain:");
                JTextField nameField = new JTextField();  // ← Tambahkan nameField di sini
            
                JButton loginButton = new JButton("Login");
            
                // Tambahkan komponen ke panel
                panel.add(ipLabel);
                panel.add(ipField);
                panel.add(portLabel);
                panel.add(portField);
                panel.add(nameLabel);
                panel.add(nameField);
                panel.add(new JLabel()); // Placeholder kosong untuk layout
                panel.add(loginButton);
            
                loginFrame.add(panel, BorderLayout.CENTER);
                loginFrame.setLocationRelativeTo(null);
                loginFrame.setVisible(true);
            
                // ActionListener tombol login
                loginButton.addActionListener(e -> {
                    String serverIP = ipField.getText().trim();
                    String portText = portField.getText().trim();
                    String playerName = nameField.getText().trim(); // ← Ambil nama dari input
            
                    if (serverIP.isEmpty() || portText.isEmpty() || playerName.isEmpty()) {
                        JOptionPane.showMessageDialog(loginFrame, "IP, Port, dan Nama tidak boleh kosong!", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
            
                    try {
                        int port = Integer.parseInt(portText);
                        GameClient client = new GameClient(serverIP, port);
            
                        // Kirim nama user ke server (gunakan format LOGIN:<nama>)
                        client.getOutput().println(playerName);
                        client.getOutput().println(playerName); // Kirim nama pemain ke server
                        client.setClientID(playerName);         // Simpan nama pemain sebagai clientID

            
                        // Tunggu respons dari server
                        String response = client.getInput().readLine();
                        if (response != null && response.startsWith("LOGIN_SUCCESS")) {
                            JOptionPane.showMessageDialog(loginFrame, "Login berhasil sebagai " + playerName + "!");
                            loginFrame.dispose();
            
                            System.out.println("Membuka ruang tunggu...");
                            JFrame waitingFrame = new JFrame("Waiting Room");
                            WaitingRoomPanel waitingRoom = new WaitingRoomPanel(e1 -> client.getOutput().println("START_GAME_REQUEST"));
                            waitingFrame.add(waitingRoom);
                            waitingFrame.setSize(400, 300);
                            waitingFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                            waitingFrame.setVisible(true);
            
                            System.out.println("Ruang tunggu berhasil ditampilkan.");
                            client.listenForUpdates(waitingRoom);
                        } else {
                            JOptionPane.showMessageDialog(loginFrame, "Login gagal!", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(loginFrame, "Gagal terhubung ke server!", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                });
            } 
        });           
    }
    

    public void waitForStart(GameClient client) {
        new Thread(() -> {
            try {
                String response;
                while ((response = client.getInput().readLine()) != null) {
                    if ("START_GAME".equals(response)) {
                        JOptionPane.showMessageDialog(this, "Game dimulai!");
                        break; // Keluar dari loop setelah menerima sinyal start
                    } else {
                        System.out.println(response); // Status room
                    }
                }
            } catch (IOException e) {
                System.out.println("Gagal menerima pesan dari server: " + e.getMessage());
            }
        }).start();
    }

    public JPanel getGamePanel() {
        return panel; // Pastikan `panel` sudah diinisialisasi sebelumnya
    }
    
    
    public static class GamePanel extends JPanel {
        private ConcurrentHashMap<String, Integer> playerScores = new ConcurrentHashMap<>();

        private int playerX = 100, playerY = 100;
        private int itemX = (int) (Math.random() * 550), itemY = (int) (Math.random() * 550);
        private int score = 0;
        private int timeLeft = 60; // waktu permainan
        private boolean up, down, left, right;
        private Timer moveTimer;
    
        private int animX = -100, animY = -100; // posisi efek skor
        private float animAlpha = 0f; // transparansi (0 = tak terlihat, 1 = penuh)
    
        private GameClient client;
        private ConcurrentHashMap<String, int[]> otherPlayers = new ConcurrentHashMap<>();
        private GameServer server;
        private String playerID;

        public GamePanel(GameClient client) {
    
            this.client = client;
            this.playerID = client.getClientID();

            // Timer Penghitung Waktu
            new Timer(1000, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    timeLeft--;
                    repaint(); // Update tampilan waktu

                    if (timeLeft <= 0) {
                        ((Timer) e.getSource()).stop();
                        moveTimer.stop(); // Hentikan gerakan
                        up = down = left = right = false; // Clear input
                    
                        // Tambahkan skor pemain ke playerScores sebelum menampilkan hasil
                        playerScores.put(playerID, score); // Gunakan ID/Nama pemain yang benar
                        String result = playerID + "," + score;
                        client.getOutput().println("SCORE:" + result);

                        String[] results = playerScores.entrySet().stream()
                        .map(entry -> entry.getKey() + "," + entry.getValue())
                        .toArray(String[]::new);

                        
                    JOptionPane.showMessageDialog(null, "Waktu habis! Skor Anda: " + score);
                    showGameResults();
                    }     
                }
            }).start();

            setBackground(new Color(240, 240, 240));
            if (client != null) {
                client.sendPlayerPosition(playerX, playerY); // Kirim posisi jika client tidak null
            }
            
    
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    int key = e.getKeyCode();
                    if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP)
                        up = true;
                    if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN)
                        down = true;
                    if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT)
                        left = true;
                    if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT)
                        right = true;
                }
    
                @Override
                public void keyReleased(KeyEvent e) {
                    int key = e.getKeyCode();
                    if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP)
                        up = false;
                    if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN)
                        down = false;
                    if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT)
                        left = false;
                    if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT)
                        right = false;
                }
            });
    
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
                    // Cek dan animasi tabrakan dengan item
                    if (Math.abs(playerX - itemX) < 30 && Math.abs(playerY - itemY) < 30) {
                        score++;
                        itemX = (int) (Math.random() * 550);
                        itemY = (int) (Math.random() * 550);
    
                        // Mulai animasi skor
                        animX = playerX;
                        animY = playerY;
                        animAlpha = 1.0f;
                    }
    
                    // Batasi dalam area 0 - 570 (karena pemain 30x30 dan window 600x600)
                    playerX = Math.max(0, Math.min(playerX, getWidth() - 30));
                    playerY = Math.max(0, Math.min(playerY, getHeight() - 30));
    
                    client.sendPlayerPosition(playerX, playerY); // kirim posisi baru
                    repaint();
                }
            });
            moveTimer.start();
    
    
            setFocusable(true);
        }

        public GamePanel(GameServer server) {
            this.server = server; // Simpan referensi ke server
            this.playerID = "Server";
            setBackground(new Color(240, 240, 240));
            
            // Setup keyboard listener untuk server
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    int key = e.getKeyCode();
                    if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP)
                        up = true;
                    if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN)
                        down = true;
                    if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT)
                        left = true;
                    if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT)
                        right = true;
                }
        
                @Override
                public void keyReleased(KeyEvent e) {
                    int key = e.getKeyCode();
                    if (key == KeyEvent.VK_W || key == KeyEvent.VK_UP)
                        up = false;
                    if (key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN)
                        down = false;
                    if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT)
                        left = false;
                    if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT)
                        right = false;
                }
            });
        
            // Timer untuk server
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
                    // Deteksi tabrakan dengan item
                    if (Math.abs(playerX - itemX) < 30 && Math.abs(playerY - itemY) < 30) {
                        score++;
                        itemX = (int) (Math.random() * 550);
                        itemY = (int) (Math.random() * 550);
        
                        // Animasi skor
                        // Mulai animasi skor
                        animX = playerX;
                        animY = playerY;
                        animAlpha = 1.0f;
                        playerScores.put(playerID, score); // simpan skor server
                        if (server != null) {
                            Map<String, Integer> all = server.getPlayerScores();
                            playerScores.putAll(all); // masukkan semua skor client
                        }

                    }
        
                    // Batasi gerakan di dalam area
                    playerX = Math.max(0, Math.min(playerX, getWidth() - 30));
                    playerY = Math.max(0, Math.min(playerY, getHeight() - 30));
        
                    repaint(); // Perbarui tampilan
                }
            });
            moveTimer.start();
        
            // Timer untuk menghitung waktu
            new Timer(1000, e -> {
                timeLeft--;
                repaint();
                if (timeLeft <= 0) {
                    ((Timer) e.getSource()).stop();
                    moveTimer.stop(); // Hentikan gerakan
                    up = down = left = right = false; // Clear input
                    JOptionPane.showMessageDialog(null, "Waktu habis! Skor Anda: " + score);
                    showGameResults(); // Tampilkan leaderboard
                }
            }).start();
        
            setFocusable(true);
        }       

        public void triggerScoreAnimation(int x, int y) {
            // Implementasi animasi skor di sini
            System.out.println("Trigger score animation at: (" + x + ", " + y + ")");
            // Tambahkan skor ke playerScores
            // Gunakan playerID dinamis
            playerScores.put(playerID, score);

        }
        
    
        public void setOtherPlayers(ConcurrentHashMap<String, int[]> others) {
            this.otherPlayers = others;
        }
    
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            GradientPaint gp = new GradientPaint(0, 0, Color.WHITE, 0, getHeight(), new Color(200, 230, 255));
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());
    
            // Bayangan
            g2.setColor(new Color(0, 0, 0, 50));
            g2.fillOval(playerX + 5, playerY + 5, 30, 30);
    
            // Karakter
            g2.setColor(Color.CYAN);
            g2.fillOval(playerX, playerY, 30, 30);
    
            // Gambar item
            g2.setColor(Color.YELLOW);
            g2.fillRect(itemX, itemY, 20, 20);
            g2.setColor(Color.ORANGE);
            g2.drawRect(itemX, itemY, 20, 20);
    
            // Gambar pemain sendiri
            g2.setColor(Color.CYAN);
            g2.fillOval(playerX, playerY, 30, 30);
    
            // Gambar pemain lain
            g2.setColor(Color.MAGENTA);
            for (int[] pos : otherPlayers.values()) {
                g2.fillOval(pos[0], pos[1], 30, 30);
            }
    
            // Skor & waktu
            g2.setColor(Color.DARK_GRAY);
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            g2.drawString("Skor: " + score, 10, 20);
            g2.drawString("Waktu: " + timeLeft, 500, 20);
    
            // Efek animasi skor +1
            if (animAlpha > 0f) {
                g2.setFont(new Font("Arial", Font.BOLD, 18));
                g2.setColor(new Color(0, 150, 0, (int) (animAlpha * 255))); // hijau dengan alpha
                g2.drawString("+1", animX, animY);
    
                animY -= 1; // naik perlahan
                animAlpha -= 0.03f; // makin transparan
            }
        }
        public void moveUp() {
            playerY = Math.max(playerY - 5, 0); // Batasi area gerak
            repaint();
        }
        
        public void moveDown() {
            playerY = Math.min(playerY + 5, getHeight() - 30);
            repaint();
        }
        
        public void moveLeft() {
            playerX = Math.max(playerX - 5, 0);
            repaint();
        }
        
        public void moveRight() {
            playerX = Math.min(playerX + 5, getWidth() - 30);
            repaint();
        }     
        
        public void updateGameState(int playerX, int playerY, int itemX, int itemY, ConcurrentHashMap<String, int[]> otherPlayers) {
            this.playerX = playerX;
            this.playerY = playerY;
            this.itemX = itemX;
            this.itemY = itemY;
            this.otherPlayers = otherPlayers;
            repaint();
        }

        public void showResults(String[] results) {
            StringBuilder message = new StringBuilder("Hasil Permainan:\n\n");
            int rank = 1;
            for (String playerData : results) {
                String[] data = playerData.split(",");
                if (data.length < 2) continue;
                message.append(rank++).append(". ").append(data[0]).append(" - ").append(data[1]).append(" poin\n");
            }
        
            JOptionPane.showMessageDialog(this, message.toString(), "Leaderboard", JOptionPane.INFORMATION_MESSAGE);
        }                      
          

        public void showGameResults() {
            StringBuilder results = new StringBuilder("🏆 Hasil Permainan - Leaderboard 🏆\n\n");

            // Gabungkan data playerScores (termasuk server & client)
            Map<String, Integer> allScores = new HashMap<>(playerScores);

            // Tambahkan data dari server jika client
            if (client == null && server != null) {
                allScores.putAll(server.getPlayerScores());
            }

            AtomicInteger rank = new AtomicInteger(1);
            allScores.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .forEachOrdered(entry -> {
                    results.append(String.format("%d. %s : %d poin\n", rank.getAndIncrement(), entry.getKey(), entry.getValue()));
                });


            JOptionPane.showMessageDialog(this, results.toString(), "Game Over - Leaderboard", JOptionPane.INFORMATION_MESSAGE);
        }
         
    }   
}

