package client;

import common.EncryptionUtil;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

public class ClientGUI {
    private JFrame frame;
    private JTextArea chatArea;
    private JTextField inputField;
    private JLabel typingLabel;
    private PrintWriter out;
    private BufferedReader in;
    private String username;

    public ClientGUI() {
        // 1. Build the Window Layout
        frame = new JFrame("Chat Application");
        frame.setSize(500, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 14));
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // Typing indicator at the bottom
        typingLabel = new JLabel(" ");
        typingLabel.setForeground(Color.GRAY);

        JPanel southPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        JButton sendButton = new JButton("Send");

        southPanel.add(typingLabel, BorderLayout.NORTH);
        southPanel.add(inputField, BorderLayout.CENTER);
        southPanel.add(sendButton, BorderLayout.EAST);
        frame.add(southPanel, BorderLayout.SOUTH);

        // 2. Add Message Sending Logic
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        // 3. Add Typing Signal Logic
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (out != null) {
                    out.println(EncryptionUtil.encrypt("/typing"));
                }
            }
        });

        frame.setVisible(true);
        connectToServer();
    }

    private void connectToServer() {
        // Ask for username on launch
        username = JOptionPane.showInputDialog(frame, "Enter your username:");
        if (username == null || username.trim().isEmpty()) {
            System.exit(0);
        }

        try {
            Socket socket = new Socket("192.168.0.103", 1234);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Read the initial "Enter username" prompt from server
            EncryptionUtil.decrypt(in.readLine());
            // Send the username
            out.println(EncryptionUtil.encrypt(username));

            // Start receiving thread
            new Thread(this::listenForMessages).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error: Could not connect to server.");
            System.exit(0);
        }
    }

    private void listenForMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                String decrypted = EncryptionUtil.decrypt(message);

                // CRITICAL: Check for the typing signal first!
                if (decrypted.startsWith("/typing ")) {
                    String userTyping = decrypted.substring(8);
                    SwingUtilities.invokeLater(() -> {
                        typingLabel.setText(userTyping + " is typing...");

                        // This timer clears the text after 3 seconds
                        Timer timer = new Timer(3000, e -> typingLabel.setText(" "));
                        timer.setRepeats(false);
                        timer.start();
                    });
                } else if (!decrypted.startsWith("/")) {
                    // Only show in chat if it's NOT a command
                    SwingUtilities.invokeLater(() -> {
                        chatArea.append(decrypted + "\n");
                        chatArea.setCaretPosition(chatArea.getDocument().getLength());
                    });
                }
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> chatArea.append("Connection lost.\n"));
        }
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (!text.isEmpty()) {
            out.println(EncryptionUtil.encrypt(text));
            inputField.setText("");
            // Don't show commands in the local chat area
            if (!text.startsWith("/")) {
                chatArea.append("You: " + text + "\n");
            }
        }
    }

    public static void main(String[] args) {
        // Run the GUI on the Event Dispatch Thread
        SwingUtilities.invokeLater(ClientGUI::new);
    }
}