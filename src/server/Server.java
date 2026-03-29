package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Server {
    private static final int PORT = 1234;
    static final List<ClientHandler> clients = new ArrayList<>();
    static final HashMap<String, List<ClientHandler>> rooms = new HashMap<>();

    public static void main(String[] args) {

        try {
            DatabaseManager.getConnection();
            System.out.println("DATABASE: Connection Successful!");
        } catch (Exception e) {
            System.err.println("DATABASE: Connection Failed! Check your password.");
            e.printStackTrace();
            return; // Stop the server if DB is broken
        }

        rooms.put("general", new ArrayList<>());
        // ... rest of your code ...

        rooms.put("general", new ArrayList<>()); // Default room

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected.");
                ClientHandler clientHandler = new ClientHandler(socket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void broadcastToRoom(String roomName, String message, ClientHandler sender) {
        List<ClientHandler> roomClients = rooms.get(roomName);
        if (roomClients != null) {
            for (ClientHandler client : roomClients) {
                if (client != sender) {
                    client.sendMessage(message);
                }
            }
        }
    }

    public static synchronized void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        for (List<ClientHandler> roomClients : rooms.values()) {
            roomClients.remove(clientHandler);
        }
        if (clientHandler.getUsername() != null) {
            DatabaseManager.setUserStatus(clientHandler.getUsername(), false);
        }
    }

    public static synchronized String getUserList() {
        StringBuilder userList = new StringBuilder("Online users: ");
        for (ClientHandler client : clients) {
            if (client.getUsername() != null) {
                userList.append(client.getUsername()).append(", ");
            }
        }
        return userList.toString();
    }
}