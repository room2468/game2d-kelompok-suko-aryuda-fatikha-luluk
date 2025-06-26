package client;

import javax.swing.*;
import java.io.IOException;

public class LoginUtil {
    public static GameClient connectToServer(String serverIP, int port) throws IOException {
        GameClient client = new GameClient(serverIP, port);

        // Kirim salam awal untuk server
        client.getOutput().println("HELLO_SERVER");

        // Tunggu respon server
        String response = client.getInput().readLine();
        if (!"LOGIN_SUCCESS".equals(response)) {
            throw new IOException("Login gagal! Pesan server: " + response);
        }

        return client;
    }
}
