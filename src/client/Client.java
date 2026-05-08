package client;

// Handles message encryption and decryption
import common.EncryptionUtil;
// java.io.* reading and writing streams
import java.io.*;
// Socket connects client to server
import java.net.Socket;
// Scanner reads keyboard input lines
import java.util.Scanner;

public class Client {
    // Remote server address to connect
    private static final String SERVER_ADDRESS = "bore.pub";
    // Port number for server connection
    private static final int PORT = 61664;

    public static void main(String[] args) {
        try (// Open connection to server
             Socket socket = new Socket(SERVER_ADDRESS, PORT);
             // Reads incoming messages from server
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             // Sends outgoing messages to server
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             // Reads user keyboard input
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to the Chat Server!");

            // THREAD 1: Receiving messages from server
            Thread receiveThread = new Thread(() -> {
                try {
                    // Holds each incoming message
                    String message;
                    while ((message = in.readLine()) != null) {
                        // Decrypt before printing to console
                        String decrypted = EncryptionUtil.decrypt(message);
                        // Print server message received
                        System.out.println("\n[Server]: " + decrypted);
                        System.out.print("> "); // Console prompt
                    }
                } catch (IOException e) {
                    // Server closed or disconnected
                    System.out.println("Disconnected from server.");
                }
            });
            // Start background receiving thread
            receiveThread.start();

            // THREAD 2: Reading from keyboard and sending to server
            System.out.print("> ");
            while (scanner.hasNextLine()) {
                // Read one line from keyboard
                String input = scanner.nextLine();
                // Encrypt before sending to network
                out.println(EncryptionUtil.encrypt(input));

                // Quit command exits loop
                if (input.equalsIgnoreCase("/quit")) {
                    break;
                }
            }

        } catch (IOException e) {
            // Server not running or unreachable
            System.err.println("Could not connect to server. Is Server.java running?");
        }
    }
}