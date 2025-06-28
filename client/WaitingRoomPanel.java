package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class WaitingRoomPanel extends JPanel {
    private JLabel waitingLabel;
    private JButton startButton;
    private JLabel playerListLabel;
    private JList<String> playerList;

    public WaitingRoomPanel(ActionListener onStartButtonClick) {
        setLayout(new BorderLayout());

        // Label status ruang tunggu
        waitingLabel = new JLabel("Menunggu pemain lain...", SwingConstants.CENTER);
        waitingLabel.setFont(new Font("Arial", Font.PLAIN, 18));

        // Label daftar pemain
        playerListLabel = new JLabel("<html><b>Daftar Pemain:</b><br>Tidak ada pemain...</html>", SwingConstants.CENTER);
        playerListLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        playerList = new JList<>();
        JScrollPane scrollPane = new JScrollPane(playerList);
        add(scrollPane, BorderLayout.CENTER);

        // Tombol Mulai Permainan
        startButton = new JButton("Mulai Permainan");
        startButton.setFont(new Font("Arial", Font.BOLD, 16));
        startButton.setEnabled(false); // Default: Tidak bisa diklik
        startButton.addActionListener(onStartButtonClick);

        // Panel tengah untuk daftar pemain
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(waitingLabel, BorderLayout.NORTH);
        centerPanel.add(playerListLabel, BorderLayout.CENTER);

        // Tambahkan ke panel utama
        add(centerPanel, BorderLayout.CENTER);
        add(startButton, BorderLayout.SOUTH);
    }

    // Update status ruang tunggu
    public void updateWaitingStatus(String message) {
        waitingLabel.setText(message);
    
        // Periksa jika sudah cukup pemain untuk memulai game
        if (message.contains("3 players connected")) {
            enableStartButton();
        }
    }

    // Update daftar pemain
    public void updatePlayerList(String[] players) {
        playerList.setListData(players); // `playerList` adalah JList<String> yang menampilkan daftar pemain
        StringBuilder playerListHTML = new StringBuilder("<html><b>Daftar Pemain:</b><br>");
        for (String player : players) {
            playerListHTML.append(player).append("<br>");
        }
        playerListHTML.append("</html>");
        playerListLabel.setText(playerListHTML.toString());
    }

    // Atur tombol Mulai Permainan
    public void setStartButtonEnabled(boolean enabled) {
        startButton.setEnabled(enabled);
    }
    private void enableStartButton() {
        startButton.setEnabled(true);
    }
    
}
