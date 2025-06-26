package client;

import javax.swing.*;

import client.GameWindow.GamePanel;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameClient {
    private GameWindow gameWindow;
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private ConcurrentHashMap<String, int[]> otherPlayers = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Integer> playerScores = new ConcurrentHashMap<>();
    private String clientID;

    public GameClient(String serverAddress, int serverPort) throws IOException {
        try {
            socket = new Socket(serverAddress, serverPort);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            System.out.println("Terhubung ke server: " + serverAddress + ":" + serverPort);
        } catch (IOException e) {
            System.err.println("Gagal terhubung ke server: " + e.getMessage());
            throw e;
        }
    }

    public void sendPlayerPosition(int x, int y) {
        if (output != null) {
            output.println("PLAYER:X:" + x + ",Y:" + y);
        }
    }
    public void setClientID(String id) {
        this.clientID = id;
    }
    
    public String getClientID() {
        return clientID;
    }
    

    public void listenForUpdates(JPanel waitingRoom) {
        new Thread(() -> {
            try {
                String response;
                while ((response = input.readLine()) != null) {
                    if (response.startsWith("LEADERBOARD:")) {
                        processMessage(response); // Gunakan handler terstruktur
                    } else if (response.startsWith("ROOM_STATUS")) {
                        String[] parts = response.split(";");
                        String status = parts[0].replace("ROOM_STATUS:", "").trim();
                        String[] players = parts.length > 1 ? parts[1].split(",") : new String[0];

                        SwingUtilities.invokeLater(() -> {
                            if (waitingRoom instanceof WaitingRoomPanel) {
                                ((WaitingRoomPanel) waitingRoom).updateWaitingStatus(status);
                                ((WaitingRoomPanel) waitingRoom).updatePlayerList(players);
                            }
                        });
                    } else if ("START_GAME".equals(response)) {
                        SwingUtilities.invokeLater(() -> {
                            JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(waitingRoom);
                            if (parentFrame != null) {
                                parentFrame.dispose(); // Tutup waiting room
                            }
                            new GameWindow(this); // Mulai game
                        });
                        break; // Keluar dari loop setelah menerima sinyal START_GAME
                    } else {
                        System.out.println("Pesan tidak dikenal: " + response);
                    }
                }
            } catch (IOException e) {
                System.err.println("Koneksi ke server terputus: " + e.getMessage());
            }
        }).start();
    }   

    private void processMessage(String message) {
        String[] players = message.substring("LEADERBOARD:".length()).split(";");
        if (players.length == 0) {
            System.err.println("Pesan LEADERBOARD kosong.");
            return;
        }

        SwingUtilities.invokeLater(() -> {
            StringBuilder results = new StringBuilder("Hasil Permainan:\n\n");
            int rank = 1;
            for (String playerData : players) {
                String[] data = playerData.split(",");
                if (data.length < 2) continue; // Lewati jika format tidak sesuai
                results.append(rank++).append(". ").append(data[0]).append(" - ").append(data[1]).append(" poin\n");
            }
            JOptionPane.showMessageDialog(null, results.toString(), "Leaderboard", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    public BufferedReader getInput() {
        return input;
    }

    public PrintWriter getOutput() {
        return output;
    }

    public void close() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("Koneksi ditutup dengan aman.");
            }
        } catch (IOException e) {
            System.err.println("Gagal menutup koneksi: " + e.getMessage());
        }
    }

    private void broadcastGameResults() {
        // Contoh hasil dummy, Anda bisa mengganti dengan data relevan dari klien.
        String resultMessage = "GAME_RESULTS:Player1,10;Player2,8;Player3,5;";
    
        if (output != null) {
            output.println(resultMessage);
        } else {
            System.err.println("Gagal mengirim hasil game: Output stream tidak tersedia.");
        }
    }    

  
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame loginFrame = new JFrame("Login Game");
            loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            loginFrame.setSize(400, 200);

            JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
            JLabel ipLabel = new JLabel("Server IP:");
            JTextField ipField = new JTextField();
            JLabel portLabel = new JLabel("Port:");
            JTextField portField = new JTextField("1234");
            JButton loginButton = new JButton("Login");

            panel.add(ipLabel);
            panel.add(ipField);
            panel.add(portLabel);
            panel.add(portField);
            panel.add(new JLabel()); // Kosong
            panel.add(loginButton);

            loginFrame.add(panel, BorderLayout.CENTER);
            loginFrame.setVisible(true);

            loginButton.addActionListener(e -> {
                String serverIP = ipField.getText().trim();
                String portText = portField.getText().trim();
            
                if (serverIP.isEmpty() || portText.isEmpty()) {
                    JOptionPane.showMessageDialog(loginFrame, "IP Address dan Port tidak boleh kosong!", "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
            
                try {
                    int port = Integer.parseInt(portText);
                    GameClient client = new GameClient(serverIP, port);
            
                    // Kirim ID unik (misal: random atau dari input user)
                    String clientID = JOptionPane.showInputDialog(loginFrame, "Masukkan Nama atau ID:");
                    if (clientID == null || clientID.trim().isEmpty()) {
                        JOptionPane.showMessageDialog(loginFrame, "ID tidak boleh kosong!", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    client.getOutput().println(clientID); // Kirim ke server (sesuai harapan ClientHandler)

            
                    String response = client.getInput().readLine();
                    if ("LOGIN_SUCCESS".equals(response)) {
                        JOptionPane.showMessageDialog(loginFrame, "Login berhasil!");
                        loginFrame.dispose();
            
                        JFrame waitingFrame = new JFrame("Waiting Room");
                        WaitingRoomPanel waitingPanel = new WaitingRoomPanel(null);
                        waitingFrame.add(waitingPanel);
                        waitingFrame.setSize(400, 300);
                        waitingFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                        waitingFrame.setLocationRelativeTo(null);
                        waitingFrame.setVisible(true);
            
                        client.listenForUpdates(waitingPanel);
                    } else {
                        JOptionPane.showMessageDialog(loginFrame, "Login gagal! Pesan server: " + response, "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(loginFrame, "Port harus berupa angka!", "Error",
                            JOptionPane.ERROR_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(loginFrame, "Gagal terhubung ke server: " + ex.getMessage(), "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            });
            
        });
    }

    public void sendMessage(String string) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'sendMessage'");
    }
}
