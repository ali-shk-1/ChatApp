package server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    // MySQL database connection URL
    private static final String URL = "jdbc:mysql://localhost:3306/chatapp";
    // Database username credential
    private static final String USER = "root";
    // Database password credential
    private static final String PASS = "123#Ali123";

    public static Connection getConnection() throws SQLException {
        // Return new database connection
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static void registerUser(String username, String password) {
        // Insert or update user record
        String query = "INSERT INTO users (username, password_hash, is_online) " +
                "VALUES (?, SHA2(?, 256), 1) " +
                "ON DUPLICATE KEY UPDATE is_online = 1, last_seen = CURRENT_TIMESTAMP";

        try (Connection conn = getConnection();
             // Prepare statement with query
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            // Execute and capture affected rows
            int rows = pstmt.executeUpdate();
            System.out.println("DEBUG: registerUser executed. Rows affected: " + rows);

        } catch (SQLException e) {
            System.err.println("CRITICAL DB ERROR: Could not register user!");
            e.printStackTrace();
        }
    }

    public static boolean authenticateUser(String username, String password) {
        // Query user by credentials
        String query = "SELECT id FROM users WHERE username = ? AND password_hash = SHA2(?, 256)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            // Execute credential lookup query
            ResultSet rs = pstmt.executeQuery();
            // Return true if found
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void saveMessage(String sender, String roomName, String messageText) {
        // Insert room message to database
        String query = "INSERT INTO messages (sender_username, room_name, message_text) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, sender);
            pstmt.setString(2, roomName);
            pstmt.setString(3, messageText);
            // Save message to database
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<String> getRoomHistory(String roomName) {
        // List to store history
        List<String> history = new ArrayList<>();
        // Fetch last fifty messages
        String query = "SELECT sender_username, message_text FROM messages WHERE room_name = ? ORDER BY sent_at ASC LIMIT 50";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, roomName);
            // Execute room history query
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                // Append formatted message entry
                history.add(rs.getString("sender_username") + ": " + rs.getString("message_text"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return history;
    }

    public static void savePrivateMessage(String sender, String receiver, String messageText) {
        // Insert private message to database
        String query = "INSERT INTO private_messages (sender_username, receiver_username, message_text) VALUES (?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, sender);
            pstmt.setString(2, receiver);
            pstmt.setString(3, messageText);
            // Execute private message insert
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void createRoom(String roomName, String creator) {
        // Insert new room if absent
        String query = "INSERT IGNORE INTO rooms (room_name, created_by) VALUES (?, ?)";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, roomName);
            pstmt.setString(2, creator);
            // Execute room creation query
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<String> getAllRooms() {
        // List to hold room names
        List<String> rooms = new ArrayList<>();
        // Select all room names
        String query = "SELECT room_name FROM rooms";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                // Add each room name
                rooms.add(rs.getString("room_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return rooms;
    }

    public static void setUserStatus(String username, boolean isOnline) {
        // Update user online status
        String query = "UPDATE users SET is_online = ?, last_seen = CURRENT_TIMESTAMP WHERE username = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setBoolean(1, isOnline);
            pstmt.setString(2, username);
            // Execute status update query
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}