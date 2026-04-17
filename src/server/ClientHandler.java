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
    public String username;
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

    public void sendMessage(String message) {
        if (out != null) {
            out.println(EncryptionUtil.encrypt(message));
        }
    }

    @Override
    public void run() {
        try {
            // Step 1: Login/Handshake
            out.println(EncryptionUtil.encrypt("Enter username:"));
            String encryptedUser = in.readLine();
            if (encryptedUser == null) return;
            username = EncryptionUtil.decrypt(encryptedUser);

            // Step 2: Database Update
            DatabaseManager.registerUser(username, "defaultPassword123");
            DatabaseManager.setUserStatus(username, true);

            // Step 3: Join default room 'general'
            synchronized (Server.rooms) {
                if (Server.rooms.containsKey(currentRoom)) {
                    Server.rooms.get(currentRoom).add(this);
                }
            }

            // Step 4: Loading previous messages
            List<String> history = DatabaseManager.getRoomHistory(currentRoom);
            sendMessage("--- History for " + currentRoom + " ---");
            for (String msg : history) {
                sendMessage("[History] " + msg);
            }
            sendMessage("--- End of History ---");

            Server.broadcastToRoom(currentRoom, username + " has joined the room.", this);

            // Step 5: Main Loop with FIXES
            String encryptedInput;
            while ((encryptedInput = in.readLine()) != null) {
                String message = EncryptionUtil.decrypt(encryptedInput);

                // --- FIX 1: Ignore empty or null messages ---
                if (message == null || message.trim().isEmpty()) {
                    continue;
                }

                if (message.startsWith("/")) {
                    handleCommand(message);
                } else {
                    // --- FIX 2: Ensure trimmed message is not empty before saving ---
                    String cleanMsg = message.trim();
                    if (!cleanMsg.isEmpty()) {
                        Server.broadcastToRoom(currentRoom, username + ": " + cleanMsg, this);
                        DatabaseManager.saveMessage(username, currentRoom, cleanMsg);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Connection lost with user: " + username);
        } finally {
            closeEverything();
        }
    }

    private void handleCommand(String commandStr) {
        // Preserving your original 3-part split for /msg support
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
                    synchronized (Server.rooms) {
                        if (Server.rooms.containsKey(targetRoom)) {
                            Server.broadcastToRoom(currentRoom, username + " left the room.", this);
                            Server.rooms.get(currentRoom).remove(this);

                            currentRoom = targetRoom;
                            Server.rooms.get(currentRoom).add(this);
                            sendMessage("Switched to room: " + currentRoom);

                            for (String msg : DatabaseManager.getRoomHistory(currentRoom)) {
                                sendMessage("[History] " + msg);
                            }
                        } else {
                            sendMessage("Room does not exist.");
                        }
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
                // Broadcast typing signal (not saved to DB)
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
        } catch (IOException e) { e.printStackTrace(); }
    }
}