package server;

// Handles message encryption and decryption
import common.EncryptionUtil;
// java.io.* — reading and writing streams
import java.io.*;
// Socket represents one client connection
import java.net.Socket;
// Resizable list for history messages
import java.util.ArrayList;
// Interface type for message lists
import java.util.List;

public class ClientHandler implements Runnable {
    // The client's network socket
    private Socket socket;
    // Reads messages from client
    private BufferedReader in;
    // Sends messages to client
    private PrintWriter out;
    // Logged in client username
    public String username;
    // Client starts in general room
    private String currentRoom = "general";

    public ClientHandler(Socket socket) {
        // Store the client socket
        this.socket = socket;
        try {
            // Setup input stream reader
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // Setup output stream writer
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Returns this client's username
    public String getUsername() { return username; }

    public void sendMessage(String message) {
        if (out != null) {
            // Encrypt before sending message
            out.println(EncryptionUtil.encrypt(message));
        }
    }

    @Override
    public void run() {
        try {
            // Step 1: Login/Handshake
            // Ask client for username
            out.println(EncryptionUtil.encrypt("Enter username:"));
            // Read encrypted username line
            String encryptedUser = in.readLine();
            // Disconnect if nothing received
            if (encryptedUser == null) return;
            // Decrypt to get real username
            username = EncryptionUtil.decrypt(encryptedUser);

            // Step 2: Database Update
            // Register or update user record
            DatabaseManager.registerUser(username, "defaultPassword123");
            // Mark this user as online
            DatabaseManager.setUserStatus(username, true);

            // Step 3: Join default room 'general'
            synchronized (Server.rooms) {
                if (Server.rooms.containsKey(currentRoom)) {
                    // Add client to general room
                    Server.rooms.get(currentRoom).add(this);
                }
            }

            // Step 4: Loading previous messages
            // Fetch last 100 room messages
            List<String> history = DatabaseManager.getRoomHistory(currentRoom);
            // Send history start marker
            sendMessage("--- History for " + currentRoom + " ---");
            for (String msg : history) {
                // Tag each history message
                sendMessage("[History] " + msg);
            }
            // Send history end marker
            sendMessage("--- End of History ---");

            // Notify others user joined
            Server.broadcastToRoom(currentRoom, username + " has joined the room.", this);

            // Step 5: Main Loop with FIXES
            // Holds each encrypted input
            String encryptedInput;
            while ((encryptedInput = in.readLine()) != null) {
                // Decrypt the incoming message
                String message = EncryptionUtil.decrypt(encryptedInput);

                // --- FIX 1: Ignore empty or null messages ---
                if (message == null || message.trim().isEmpty()) {
                    // Skip blank messages entirely
                    continue;
                }

                if (message.startsWith("/")) {
                    // Route to command handler
                    handleCommand(message);
                } else {
                    // --- FIX 2: Ensure trimmed message is not empty before saving ---
                    // Strip leading trailing whitespace
                    String cleanMsg = message.trim();
                    if (!cleanMsg.isEmpty()) {
                        // Broadcast to everyone in room
                        Server.broadcastToRoom(currentRoom, username + ": " + cleanMsg, this);
                        // Persist message to database
                        DatabaseManager.saveMessage(username, currentRoom, cleanMsg);
                    }
                }
            }
        } catch (IOException e) {
            // Client dropped unexpectedly
            System.out.println("Connection lost with user: " + username);
        } finally {
            // Always clean up on exit
            closeEverything();
        }
    }

    private void handleCommand(String commandStr) {
        // Split into max three parts
        String[] parts = commandStr.split(" ", 3);
        // Lowercase for easy matching
        String command = parts[0].toLowerCase();

        switch (command) {
            case "/users":
                // Send online users list
                sendMessage(Server.getUserList());
                break;

            case "/rooms":
                // Send all available rooms
                sendMessage("Available rooms: " + String.join(", ", DatabaseManager.getAllRooms()));
                break;

            case "/create":
                if (parts.length > 1) {
                    // New room name from input
                    String newRoom = parts[1];
                    // Save room to database
                    DatabaseManager.createRoom(newRoom, username);
                    synchronized (Server.rooms) {
                        // Add room if not exists
                        Server.rooms.putIfAbsent(newRoom, new ArrayList<>());
                    }
                    // Tell user room created
                    sendMessage("Room '" + newRoom + "' created. Type /join " + newRoom + " to enter.");
                }
                break;

            case "/join":
                if (parts.length > 1) {
                    // Room the user wants
                    String targetRoom = parts[1];
                    synchronized (Server.rooms) {
                        if (Server.rooms.containsKey(targetRoom)) {
                            // Notify old room departure
                            Server.broadcastToRoom(currentRoom, username + " left the room.", this);
                            // Remove from old room
                            Server.rooms.get(currentRoom).remove(this);

                            // Switch to new room
                            currentRoom = targetRoom;
                            // Add to new room list
                            Server.rooms.get(currentRoom).add(this);
                            // Confirm room switch success
                            sendMessage("Switched to room: " + currentRoom);

                            // Send new room history
                            for (String msg : DatabaseManager.getRoomHistory(currentRoom)) {
                                sendMessage("[History] " + msg);
                            }
                        } else {
                            // Tell user room missing
                            sendMessage("Room does not exist.");
                        }
                    }
                }
                break;

            case "/msg":
                if (parts.length >= 3) {
                    // Target recipient username
                    String targetUser = parts[1];
                    // The private message text
                    String privateMsg = parts[2];
                    // Track if user was found
                    boolean found = false;
                    for (ClientHandler client : Server.clients) {
                        if (client.username.equalsIgnoreCase(targetUser)) {
                            // Deliver message to target
                            client.sendMessage("[PM from " + username + "]: " + privateMsg);
                            // Save private message db
                            DatabaseManager.savePrivateMessage(username, targetUser, privateMsg);
                            // Confirm send to sender
                            sendMessage("[PM to " + targetUser + "]: " + privateMsg);
                            found = true;
                            break;
                        }
                    }
                    // Warn if user offline
                    if (!found) sendMessage("User " + targetUser + " is not online.");
                }
                break;

            case "/typing":
                // Broadcast typing signal (not saved to DB)
                // Tell room who is typing
                Server.broadcastToRoom(currentRoom, "/typing " + username, this);
                break;

            case "/kick":
                if (username.equalsIgnoreCase("admin") && parts.length > 1) {
                    // Username to be kicked
                    String target = parts[1];
                    for (ClientHandler client : Server.clients) {
                        if (client.username.equalsIgnoreCase(target)) {
                            // Warn target before kick
                            client.sendMessage("SYSTEM: You have been kicked by the admin.");
                            // Force close their connection
                            client.closeEverything();
                            break;
                        }
                    }
                } else {
                    // Only admin can kick
                    sendMessage("Error: Only 'admin' can use /kick.");
                }
                break;

            case "/quit":
                // Gracefully close connection
                closeEverything();
                break;

            default:
                // Tell user unknown command
                sendMessage("Unknown command: " + command);
                break;
        }
    }

    public void closeEverything() {
        // Remove from all server lists
        Server.removeClient(this);
        // Notify room user left
        Server.broadcastToRoom(currentRoom, username + " has left the chat.", null);
        try {
            // Close the network socket
            if (socket != null) socket.close();
        } catch (IOException e) { e.printStackTrace(); }
    }
}