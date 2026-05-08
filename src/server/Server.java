package server;

// Thrown when an input/output operation fails
import java.io.IOException;
// Creates server that listens for connections
import java.net.ServerSocket;
// Represents a single client connection
import java.net.Socket;
// Resizable array to store client list
import java.util.ArrayList;
// Stores room names mapped to clients
import java.util.HashMap;
// Interface type used for client lists
import java.util.List;

public class Server {
    // Default server port number
    private static final int PORT = 1234;
    // Stores all connected clients
    static final List<ClientHandler> clients = new ArrayList<>();
    // Maps room names to clients
    static final HashMap<String, List<ClientHandler>> rooms = new HashMap<>();

    public static void main(String[] args) {

        try {
            // Attempt database connection here
            DatabaseManager.getConnection();
            System.out.println("DATABASEN: Connection Successful!");
        } catch (Exception e) {
            System.err.println("DATABASE: Connection Failed! Check your password.");
            e.printStackTrace();
            // Stop server if database fails
            return;
        }

        // Initialize default general room
        rooms.put("general", new ArrayList<>());

        // Reinitialize general room again
        rooms.put("general", new ArrayList<>());

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                // Accept incoming client connection
                Socket socket = serverSocket.accept();
                System.out.println("New client connected.");
                // Create handler for new client
                ClientHandler clientHandler = new ClientHandler(socket);
                // Add client to global list
                clients.add(clientHandler);
                // Start client thread here
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void broadcastToRoom(String roomName, String message, ClientHandler sender) {
        // Fetch clients in room
        List<ClientHandler> roomClients = rooms.get(roomName);
        if (roomClients != null) {
            for (ClientHandler client : roomClients) {
                if (client != sender) {
                    // Send message to client
                    client.sendMessage(message);
                }
            }
        }
    }

    public static synchronized void removeClient(ClientHandler clientHandler) {
        // Remove from global clients list
        clients.remove(clientHandler);
        for (List<ClientHandler> roomClients : rooms.values()) {
            // Remove client from room
            roomClients.remove(clientHandler);
        }
        if (clientHandler.getUsername() != null) {
            // Mark user as offline
            DatabaseManager.setUserStatus(clientHandler.getUsername(), false);
        }
    }

    public static synchronized String getUserList() {
        // Build online users string
        StringBuilder userList = new StringBuilder("Online users: ");
        for (ClientHandler client : clients) {
            if (client.getUsername() != null) {
                // Append each username here
                userList.append(client.getUsername()).append(", ");
            }
        }
        return userList.toString();
    }
}