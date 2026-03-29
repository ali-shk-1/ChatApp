package server;

import common.EncryptionUtil;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    public String username; // Public so Server.java can access it easily
    private String currentRoom = "general";

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getUsername() { return username; }

    /**
     * Sends an encrypted message to this specific client.
     */
    public void sendMessage(String message) {
        out.println(EncryptionUtil.encrypt(message));
    }

    @Override
    public void run() {
        try {
            // Step 1: Login/Handshake
            out.println(EncryptionUtil.encrypt("Enter username:"));
            String encryptedUser = in.readLine();
            if (encryptedUser == null) return;
            username = EncryptionUtil.decrypt(encryptedUser);

            // Step 2: Database Update - Register the user or mark as online
            // FIX: Using registerUser so new users are actually saved to the database.
            // Note: We are passing a default password since the GUI only asks for a username right now.
            DatabaseManager.registerUser(username, "defaultPassword123");

            // Step 3: Join default room 'general'
            synchronized (Server.rooms) {
                Server.rooms.get(currentRoom).add(this);
            }

            // Step 4: Loading previous messages (The WhatsApp behavior)
            List<String> history = DatabaseManager.getRoomHistory(currentRoom);
            sendMessage("--- History for " + currentRoom + " ---");
            for (String msg : history) {
                sendMessage("[History] " + msg);
            }
            sendMessage("--- End of History ---");

            // Notify others
            Server.broadcastToRoom(currentRoom, username + " has joined the room.", this);

            // Step 5: Main Command & Message Loop
            String encryptedInput;
            while ((encryptedInput = in.readLine()) != null) {
                String message = EncryptionUtil.decrypt(encryptedInput);

                if (message.startsWith("/")) {
                    handleCommand(message);
                } else {
                    // Normal message: Broadcast to room and save to DB
                    Server.broadcastToRoom(currentRoom, username + ": " + message, this);
                    DatabaseManager.saveMessage(username, currentRoom, message);
                }
            }
        } catch (IOException e) {
            System.out.println("Connection lost with user: " + username);
        } finally {
            closeEverything();
        }
    }

    private void handleCommand(String commandStr) {
        String[] parts = commandStr.split(" ", 3);
        String command = parts[0].toLowerCase();

        switch (command) {
            case "/users":
                sendMessage(Server.getUserList());
                break;

            case "/rooms":
                sendMessage("Available rooms: " + String.join(", ", DatabaseManager.getAllRooms()));
                break;

            case "/create":
                if (parts.length > 1) {
                    String newRoom = parts[1];
                    DatabaseManager.createRoom(newRoom, username);
                    synchronized (Server.rooms) {
                        Server.rooms.putIfAbsent(newRoom, new ArrayList<>());
                    }
                    sendMessage("Room '" + newRoom + "' created. Type /join " + newRoom + " to enter.");
                }
                break;

            case "/join":
                if (parts.length > 1) {
                    String targetRoom = parts[1];
                    if (Server.rooms.containsKey(targetRoom)) {
                        // Leave old room
                        Server.broadcastToRoom(currentRoom, username + " left the room.", this);
                        Server.rooms.get(currentRoom).remove(this);

                        // Join new room
                        currentRoom = targetRoom;
                        Server.rooms.get(currentRoom).add(this);
                        sendMessage("Switched to room: " + currentRoom);

                        // Load history for the new room
                        for (String msg : DatabaseManager.getRoomHistory(currentRoom)) {
                            sendMessage("[History] " + msg);
                        }
                    } else {
                        sendMessage("Room does not exist.");
                    }
                }
                break;

            case "/msg":
                if (parts.length >= 3) {
                    String targetUser = parts[1];
                    String privateMsg = parts[2];
                    boolean found = false;
                    for (ClientHandler client : Server.clients) {
                        if (client.username.equalsIgnoreCase(targetUser)) {
                            client.sendMessage("[PM from " + username + "]: " + privateMsg);
                            DatabaseManager.savePrivateMessage(username, targetUser, privateMsg);
                            sendMessage("[PM to " + targetUser + "]: " + privateMsg);
                            found = true;
                            break;
                        }
                    }
                    if (!found) sendMessage("User " + targetUser + " is not online.");
                }
                break;

            case "/typing":
                // Broadcast typing signal to everyone else in the room
                Server.broadcastToRoom(currentRoom, "/typing " + username, this);
                break;

            case "/kick":
                if (username.equalsIgnoreCase("admin") && parts.length > 1) {
                    String target = parts[1];
                    for (ClientHandler client : Server.clients) {
                        if (client.username.equalsIgnoreCase(target)) {
                            client.sendMessage("SYSTEM: You have been kicked by the admin.");
                            client.closeEverything();
                            break;
                        }
                    }
                } else {
                    sendMessage("Error: Only 'admin' can use /kick.");
                }
                break;

            case "/quit":
                closeEverything();
                break;

            default:
                sendMessage("Unknown command: " + command);
                break;
        }
    }

    public void closeEverything() {
        Server.removeClient(this);
        Server.broadcastToRoom(currentRoom, username + " has left the chat.", null);
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}