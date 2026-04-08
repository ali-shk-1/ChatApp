package client;

import common.EncryptionUtil;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static final String SERVER_ADDRESS = "bore.pub";
    private static final int PORT = 61664;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to the Chat Server!");

            // THREAD 1: Receiving messages from server
            Thread receiveThread = new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        // Decrypt before printing to console
                        String decrypted = EncryptionUtil.decrypt(message);
                        System.out.println("\n[Server]: " + decrypted);
                        System.out.print("> "); // Console prompt
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            });
            receiveThread.start();

            // THREAD 2: Reading from keyboard and sending to server
            System.out.print("> ");
            while (scanner.hasNextLine()) {
                String input = scanner.nextLine();
                // Encrypt before sending to network
                out.println(EncryptionUtil.encrypt(input));

                if (input.equalsIgnoreCase("/quit")) {
                    break;
                }
            }

        } catch (IOException e) {
            System.err.println("Could not connect to server. Is Server.java running?");
        }
    }
}