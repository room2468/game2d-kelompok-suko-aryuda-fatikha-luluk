package server;
    
// File: SimpleServer.java
import java.net.*;
import java.io.*;

public class SimpleServer {
    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(1234); // port 1234
        System.out.println("Server ready, menunggu koneksi...");
        Socket client = server.accept();
        System.out.println("Client terhubung!");

        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        String msg;
        while ((msg = in.readLine()) != null) {
            System.out.println("Pesan dari client: " + msg);
        }
        client.close();
        server.close();
    }
}
