package server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String URL = "jdbc:mysql://localhost:3306/chatapp";
    private static final String USER = "root";
    private static final String PASS = "123#Ali123"; // Update this!

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static void registerUser(String username, String password) {
        // Ensure we provide values for all 'NOT NULL' columns
        String query = "INSERT INTO users (username, password_hash, is_online) " +
                "VALUES (?, SHA2(?, 256), 1) " +
                "ON DUPLICATE KEY UPDATE is_online = 1, last_seen = CURRENT_TIMESTAMP";

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            int rows = pstmt.executeUpdate();
            System.out.println("DEBUG: registerUser executed. Rows affected: " + rows);

        } catch (SQLException e) {
            System.err.println("CRITICAL DB ERROR: Could not register user!");
            e.printStackTrace(); // This is your best friend for debugging
        }
    }

    public static boolean authenticateUser(String username, String password) {
        String query = "SELECT id FROM users WHERE username = ? AND password_hash = SHA2(?, 256)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void saveMessage(String sender, String roomName, String messageText) {
        String query = "INSERT INTO messages (sender_username, room_name, message_text) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, sender);
            pstmt.setString(2, roomName);
            pstmt.setString(3, messageText);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<String> getRoomHistory(String roomName) {
        List<String> history = new ArrayList<>();
        String query = "SELECT sender_username, message_text FROM messages WHERE room_name = ? ORDER BY sent_at ASC LIMIT 50";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, roomName);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                history.add(rs.getString("sender_username") + ": " + rs.getString("message_text"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return history;
    }

    public static void savePrivateMessage(String sender, String receiver, String messageText) {
        String query = "INSERT INTO private_messages (sender_username, receiver_username, message_text) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, sender);
            pstmt.setString(2, receiver);
            pstmt.setString(3, messageText);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void createRoom(String roomName, String creator) {
        String query = "INSERT IGNORE INTO rooms (room_name, created_by) VALUES (?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, roomName);
            pstmt.setString(2, creator);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<String> getAllRooms() {
        List<String> rooms = new ArrayList<>();
        String query = "SELECT room_name FROM rooms";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                rooms.add(rs.getString("room_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rooms;
    }

    public static void setUserStatus(String username, boolean isOnline) {
        String query = "UPDATE users SET is_online = ?, last_seen = CURRENT_TIMESTAMP WHERE username = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setBoolean(1, isOnline);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}