package client;

// EncryptionUtil handles message encryption/decryption
import common.EncryptionUtil;
// javax.swing.* all the GUI windows, buttons, panels
import javax.swing.*;
// javax.swing.text.*  styled chat text rendering
import javax.swing.text.*;
// java.awt.* colors, fonts, layouts, graphics drawing
import java.awt.*;
// java.awt.event.*  keyboard and mouse click listeners
import java.awt.event.*;
// BufferedImage lets us draw the app icon
import java.awt.image.BufferedImage;
// java.io.*  reading and writing data over socket
import java.io.*;
// java.net.Socket  the actual network connection
import java.net.Socket;
// javax.sound.sampled.*  plays the ping notification sound
import javax.sound.sampled.*;
// Border interface for our custom rounded borders
import javax.swing.border.Border;

public class ClientGUI {

    // Main darkest background color
    private static final Color BG_DARK          = new Color(15,  23,  42);  // Slate 900
    // Slightly lighter panel background
    private static final Color BG_PANEL         = new Color(30,  41,  59);  // Slate 800
    // Input field background color
    private static final Color BG_INPUT         = new Color(15,  23,  42);  // Slate 900
    // Main button and accent color
    private static final Color ACCENT_PRIMARY   = new Color(79,  70,  229); // Indigo 600
    // Secondary gradient accent color
    private static final Color ACCENT_SECONDARY = new Color(6,   182, 212); // Cyan 500
    // Blue hover highlight color
    private static final Color NEON_BLUE        = new Color(59,  130, 246); // Blue 500
    // Your own message text color
    private static final Color MSG_YOU_FG       = new Color(186, 230, 253); // Sky 200
    // Other people's message color
    private static final Color MSG_OTHER_FG     = new Color(248, 250, 252); // Slate 50
    // Old chat history text color
    private static final Color MSG_HISTORY_FG   = new Color(148, 163, 184); // Slate 400
    // System notification message color
    private static final Color MSG_SYSTEM_FG    = new Color(52,  211, 153); // Emerald 400
    // Dimmed label and hint color
    private static final Color TEXT_MUTED       = new Color(100, 116, 139); // Slate 500
    // Border line subtle color
    private static final Color BORDER_DIM       = new Color(51,  65,  85);  // Slate 700

    //  FONTS and UI SCALING
    // Font used in chat messages
    private static final Font FONT_CHAT;
    // Font used in input box
    private static final Font FONT_INPUT;
    // Footer signature font style
    private static final Font FONT_FOOTER;
    // Font for emoji symbols rendering
    private static final Font FONT_SYMBOL;

    static {
        // Pick best available symbol font
        FONT_SYMBOL = findBestSymbolFont(18f);
        // Bold font for chat messages
        FONT_CHAT   = new Font("Segoe UI", Font.BOLD, 15);
        // Plain font for typing area
        FONT_INPUT  = new Font("Segoe UI", Font.PLAIN, 15);
        // Italic bold footer font
        FONT_FOOTER = new Font("Segoe UI", Font.BOLD | Font.ITALIC, 12);
    }

    private static Font findBestSymbolFont(float size) {
        // Try these fonts in order
        String[] fonts = {"Segoe UI Symbol", "Segoe UI Emoji", "Arial Unicode MS", "Symbola", "DejaVu Sans"};
        for (String f : fonts) {
            Font font = new Font(f, Font.PLAIN, (int) size);
            // Return first valid font found
            if (!font.getFamily().equals("Dialog") || f.equals("Dialog")) return font;
        }
        // Fallback to default dialog font
        return new Font("Dialog", Font.PLAIN, (int) size);
    }

    // Sparkle decoration for header
    private static final String DECO   = "\u2726"; // ✦ Sparkle
    // Live indicator dot symbol
    private static final String LIVE   = "\u25CF"; // ●
    // Horizontal line separator symbol
    private static final String BULLET = "\u2500\u2500"; // ──

    // All available emoji buttons array
    private static final String[] EMOJIS = {
            "\u263A", "\u263B", "\u2605", "\u2606", "\u2764", "\u2661", "\u266A", "\u266B", "\u2600", "\u2601",
            "\u2602", "\u2744", "\u26A1", "\u2713", "\u2717", "\u2709", "\u2699", "\u260E", "\u2302", "\u25C6",
            "\u2666", "\u2663", "\u2660", "\u2726", "\u2727", "\u272A", "\u272B", "\u27A4", "\u2191", "\u2193"
    };

    // Main application window frame
    private JFrame frame;
    // Pane that displays chat messages
    private JTextPane chatPane;
    // Styled document for rich text
    private StyledDocument chatDoc;
    // Text box for typing messages
    private JTextField inputField;
    // Shows who is currently typing
    private JLabel typingLabel;
    // Sends text to the server
    private PrintWriter out;
    // Reads text from server
    private BufferedReader in;
    // Logged in username string
    private String username;

    public ClientGUI() {
        // Build then connect to server
        buildUI();
        connectToServer();
    }

    private void buildUI() {
        // Create main application window
        frame = new JFrame("ChatApp");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(620, 780);
        // Prevent window getting too small
        frame.setMinimumSize(new Dimension(500, 600));
        // Center on user's screen
        frame.setLocationRelativeTo(null);
        // Set custom app icon
        frame.setIconImage(makeAppIcon());

        // Root panel with border layout
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_DARK);
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildChat(),   BorderLayout.CENTER);
        root.add(buildSouth(),  BorderLayout.SOUTH);

        frame.setContentPane(root);
        // Make the window visible
        frame.setVisible(true);
    }

    private JPanel buildHeader() {
        // Header panel with gradient paint
        JPanel hdr = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                // Draw left to right gradient
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, ACCENT_PRIMARY, getWidth(), 0, ACCENT_SECONDARY));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        hdr.setOpaque(false);
        // Fixed header height set
        hdr.setPreferredSize(new Dimension(0, 76));
        hdr.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 22));

        // App title with sparkle icons
        JLabel title = new JLabel(DECO + "  CHATAPP  " + DECO);
        title.setFont(findBestSymbolFont(26f).deriveFont(Font.BOLD));
        title.setForeground(Color.WHITE);

        // Green live status indicator
        JLabel live = new JLabel("  " + LIVE + " LIVE");
        live.setFont(new Font("Segoe UI", Font.BOLD, 11));
        live.setForeground(new Color(165, 243, 252));

        // Left side title panel
        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.add(title, BorderLayout.CENTER);
        left.add(live,  BorderLayout.SOUTH);

        // Encrypted label on right
        JLabel lock = new JLabel("Encrypted");
        lock.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lock.setForeground(new Color(255, 255, 255, 190));

        hdr.add(left, BorderLayout.WEST);
        hdr.add(lock, BorderLayout.EAST);
        return hdr;
    }

    private JScrollPane buildChat() {
        // Non editable chat display area
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setBackground(BG_DARK);
        chatPane.setFont(FONT_CHAT);
        // Padding inside chat pane
        chatPane.setMargin(new Insets(20, 25, 20, 25));
        chatDoc = chatPane.getStyledDocument();

        // Wrap chatPane in scrollpane
        JScrollPane scroll = new JScrollPane(chatPane);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBackground(BG_DARK);
        scroll.getViewport().setBackground(BG_DARK);
        // Apply custom scrollbar style
        styleScrollBar(scroll.getVerticalScrollBar());
        return scroll;
    }

    private JPanel buildSouth() {
        // Bottom panel holds input area
        JPanel south = new JPanel(new BorderLayout());
        south.setBackground(BG_PANEL);
        // Top border line separator
        south.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_DIM));

        // Label showing typing status
        typingLabel = new JLabel(" ");
        typingLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        typingLabel.setForeground(TEXT_MUTED);
        typingLabel.setBorder(BorderFactory.createEmptyBorder(5, 18, 0, 0));

        south.add(typingLabel,     BorderLayout.NORTH);
        south.add(buildEmojiBar(), BorderLayout.CENTER);
        south.add(buildInputRow(), BorderLayout.SOUTH);

        // Footer credit label at bottom
        JLabel footer = new JLabel("  * Made by Ali");
        footer.setFont(FONT_FOOTER);
        footer.setForeground(new Color(129, 140, 248));
        footer.setBorder(BorderFactory.createEmptyBorder(2, 12, 8, 0));

        // Wrapper combines south and footer
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_PANEL);
        wrapper.add(south,  BorderLayout.CENTER);
        wrapper.add(footer, BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel buildEmojiBar() {
        // Horizontal scrollable emoji row
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 5));
        bar.setBackground(BG_PANEL);
        bar.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));

        for (String sym : EMOJIS) {
            // Custom painted emoji button
            JButton btn = new JButton(sym) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (getModel().isRollover()) {
                        // Blue glow on hover
                        g2.setColor(new Color(59, 130, 246, 50));
                        g2.fillRoundRect(1, 1, getWidth()-2, getHeight()-2, 8, 8);
                        g2.setColor(NEON_BLUE);
                        g2.setStroke(new BasicStroke(1.5f));
                        g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 8, 8);
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            btn.setFont(FONT_SYMBOL);
            btn.setForeground(Color.WHITE);
            btn.setMargin(new Insets(0, 0, 0, 0));
            btn.setFocusPainted(false);
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setOpaque(false);
            // Hand cursor on emoji hover
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setPreferredSize(new Dimension(38, 34));
            btn.addActionListener(e -> {
                // Append emoji to input
                inputField.setText(inputField.getText() + sym);
                inputField.requestFocus();
            });
            bar.add(btn);
        }
        return bar;
    }

    private JPanel buildInputRow() {
        // Row with input and send button
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(BG_PANEL);
        row.setBorder(BorderFactory.createEmptyBorder(6, 12, 10, 12));

        // Styled rounded text input field
        inputField = new JTextField();
        inputField.setFont(FONT_INPUT);
        inputField.setBackground(BG_INPUT);
        inputField.setForeground(new Color(248, 250, 252));
        inputField.setCaretColor(ACCENT_SECONDARY);
        inputField.setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(BORDER_DIM, 12, 1.5f),
                BorderFactory.createEmptyBorder(9, 14, 9, 14)
        ));

        // Gradient send button right side
        JButton send = gradientButton("Send  >", ACCENT_PRIMARY, ACCENT_SECONDARY, 110, 44);
        // Click sends the message
        send.addActionListener(e -> sendMessage());
        // Enter key sends message
        inputField.addActionListener(e -> sendMessage());
        inputField.addKeyListener(new KeyAdapter() {
            // Notify server user is typing
            @Override public void keyTyped(KeyEvent e) { if (out != null) out.println(EncryptionUtil.encrypt("/typing")); }
        });

        row.add(inputField, BorderLayout.CENTER);
        row.add(send,       BorderLayout.EAST);
        return row;
    }

    private void connectToServer() {
        // Modal login dialog window
        JDialog dlg = new JDialog(frame, "Connect to ChatApp", true);
        dlg.setUndecorated(true);
        dlg.setSize(400, 310);
        // Center over main frame
        dlg.setLocationRelativeTo(frame);

        // Rounded card panel background
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Draw rounded panel background
                g2.setColor(BG_PANEL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
            }
        };
        card.setOpaque(false);

        // Dialog header with gradient top
        JPanel hdr = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, ACCENT_PRIMARY, getWidth(), 0, ACCENT_SECONDARY));
                // Rounded top, flat bottom
                g2.fillRoundRect(0, 0, getWidth(), getHeight() + 20, 20, 20);
                g2.fillRect(0, getHeight() - 20, getWidth(), 20);
                g2.dispose();
            }
        };
        hdr.setOpaque(false);
        hdr.setPreferredSize(new Dimension(0, 72));
        hdr.setBorder(BorderFactory.createEmptyBorder(14, 24, 10, 24));

        // Dialog title label text
        JLabel hTitle = new JLabel(DECO + "  CHATAPP");
        hTitle.setFont(findBestSymbolFont(24f).deriveFont(Font.BOLD));
        hTitle.setForeground(Color.WHITE);
        hdr.add(hTitle, BorderLayout.CENTER);

        // Form with name and secret
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1.0;

        // Input fields for login
        JTextField tfName = dlgField();
        JTextField tfSecret = dlgField();

        gc.gridy = 0; form.add(smallHint("Name"), gc);
        gc.gridy = 1; gc.insets = new Insets(0,0,14,0); form.add(tfName, gc);
        gc.gridy = 2; gc.insets = new Insets(0,0,0,0); form.add(smallHint("Secret Number"), gc);
        gc.gridy = 3; gc.insets = new Insets(0,0,20,0); form.add(tfSecret, gc);

        // Button to initiate connection
        JButton connectBtn = gradientButton("Connect", ACCENT_PRIMARY, ACCENT_SECONDARY, 330, 44);
        gc.gridy = 4; form.add(connectBtn, gc);

        card.add(hdr, BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);
        dlg.setContentPane(card);

        connectBtn.addActionListener(e -> {
            // Read username and port fields
            String name = tfName.getText().trim();
            String portTx = tfSecret.getText().trim();
            // Shake if fields are empty
            if (name.isEmpty() || portTx.isEmpty()) { shake(dlg); return; }
            username = name;
            dlg.dispose();
            try {
                // Open socket to server
                Socket socket = new Socket("bore.pub", Integer.parseInt(portTx));
                in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                // Read and discard server greeting
                EncryptionUtil.decrypt(in.readLine());
                // Send username to server
                out.println(EncryptionUtil.encrypt(username));
                // Background thread listens continuously
                new Thread(this::listenForMessages).start();
                SwingUtilities.invokeLater(() -> appendSystem("Connected to ChatApp as " + username));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> appendSystem("Connection failed."));
            }
        });
        // Block until dialog closes
        dlg.setVisible(true);
    }

    private void listenForMessages() {
        try {
            String raw;
            // Keep reading until disconnected
            while ((raw = in.readLine()) != null) {
                // Decrypt each incoming line
                String msg = EncryptionUtil.decrypt(raw);
                if (msg.startsWith("/typing ")) {
                    SwingUtilities.invokeLater(() -> {
                        // Show typing indicator briefly
                        typingLabel.setText("  " + msg.substring(8) + " is typing...");
                        // Clear typing label after 3s
                        new Timer(3000, e -> typingLabel.setText(" ")).start();
                    });
                } else if (!msg.startsWith("/")) {
                    SwingUtilities.invokeLater(() -> {
                        // Render message and play sound
                        appendIncoming(msg, msg.startsWith("[History]"));
                        playPing();
                    });
                }
            }
        } catch (IOException e) { SwingUtilities.invokeLater(() -> appendSystem("Connection lost.")); }
    }

    private void sendMessage() {
        // Don't send empty messages
        String text = inputField.getText().trim();
        if (text.isEmpty() || out == null) return;
        // Encrypt before sending out
        out.println(EncryptionUtil.encrypt(text));
        // Clear after sending message
        inputField.setText("");
        // Show sent message locally
        if (!text.startsWith("/")) appendSent("You:  " + text);
    }

    private void appendSent(String text) {
        // Right aligned sent message
        appendStyled(text, MSG_YOU_FG, StyleConstants.ALIGN_RIGHT, 15);
    }

    private void appendIncoming(String text, boolean hist) {
        // Left align incoming messages
        appendStyled(text, hist ? MSG_HISTORY_FG : MSG_OTHER_FG, StyleConstants.ALIGN_LEFT, 15);
    }

    private void appendSystem(String text) {
        // Center aligned system notification
        appendStyled(BULLET + " " + text + " " + BULLET, MSG_SYSTEM_FG, StyleConstants.ALIGN_CENTER, 12);
    }

    private void appendStyled(String text, Color c, int align, int size) {
        try {
            // Build rich text attribute set
            SimpleAttributeSet a = new SimpleAttributeSet();
            StyleConstants.setForeground(a, c);
            StyleConstants.setAlignment(a, align);
            StyleConstants.setFontFamily(a, "Segoe UI");
            StyleConstants.setFontSize(a, size);
            StyleConstants.setBold(a, true);
            // Space below each message
            StyleConstants.setSpaceBelow(a, 8f);

            int start = chatDoc.getLength();
            // Insert text into document
            chatDoc.insertString(start, text + "\n", a);
            chatDoc.setParagraphAttributes(start, text.length() + 1, a, false);
            // Scroll down after insert
            chatPane.setCaretPosition(chatDoc.getLength());
        } catch (BadLocationException ignored) {}
    }

    private void playPing() {
        // Plays short beep sound
        new Thread(() -> {
            try {
                // Generate 900hz sine wave
                float sr = 44100f; int dur = (int)(sr * 0.08); byte[] buf = new byte[dur * 8];
                for (int i = 0; i < dur * 2; i++) {
                    short s = (short)(Math.sin(2 * Math.PI * 900 * i / sr) * 2000);
                    buf[i*4] = (byte)(s & 0xff); buf[i*4+1] = (byte)((s >> 8) & 0xff);
                }
                // Open and play audio clip
                AudioFormat fmt = new AudioFormat(sr, 16, 2, true, false);
                Clip clip = (Clip) AudioSystem.getLine(new DataLine.Info(Clip.class, fmt));
                clip.open(fmt, buf, 0, buf.length); clip.start();
            } catch (Exception ignored) {}
        }).start();
    }

    private JLabel smallHint(String text) {
        // Small muted hint label
        JLabel l = new JLabel(text); l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(TEXT_MUTED); return l;
    }

    private JTextField dlgField() {
        // Styled rounded dialog input
        JTextField tf = new JTextField(); tf.setFont(FONT_INPUT); tf.setBackground(BG_INPUT);
        tf.setForeground(new Color(248, 250, 252)); tf.setCaretColor(ACCENT_SECONDARY);
        tf.setPreferredSize(new Dimension(330, 42));
        tf.setBorder(BorderFactory.createCompoundBorder(new RoundedLineBorder(BORDER_DIM, 10, 1.5f), BorderFactory.createEmptyBorder(7, 14, 7, 14)));
        return tf;
    }

    private JButton gradientButton(String label, Color c1, Color c2, int w, int h) {
        // Button with gradient paint override
        JButton btn = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Reverse gradient on hover
                g2.setPaint(new GradientPaint(0, 0, getModel().isRollover() ? c2 : c1, getWidth(), 0, getModel().isRollover() ? c1 : c2));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                // Draw centered button text
                g2.setColor(Color.WHITE); g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 15)); btn.setFocusPainted(false);
        btn.setBorderPainted(false); btn.setContentAreaFilled(false);
        // Set button preferred size
        btn.setPreferredSize(new Dimension(w, h)); btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void shake(Window w) {
        // Shake window on bad input
        Point o = w.getLocation(); Timer t = new Timer(25, null);
        int[] off = {8,-8, 6,-6, 4,-4, 0}; int[] s = {0};
        // Move window left and right
        t.addActionListener(e -> { if (s[0] >= off.length) { w.setLocation(o); t.stop(); } else w.setLocation(o.x + off[s[0]++], o.y); });
        t.start();
    }

    private void styleScrollBar(JScrollBar sb) {
        // Custom dark themed scrollbar
        sb.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() { thumbColor = new Color(71, 85, 105); trackColor = BG_PANEL; }
            // Remove default arrow buttons
            @Override protected JButton createDecreaseButton(int o) { return zeroBtn(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroBtn(); }
            // Zero size invisible button
            private JButton zeroBtn() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
        });
    }

    private Image makeAppIcon() {
        // Draw gradient circle with C
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setPaint(new GradientPaint(0, 0, ACCENT_PRIMARY, 64, 64, ACCENT_SECONDARY)); g2.fillOval(0, 0, 64, 64);
        // Draw letter C on icon
        g2.setColor(Color.WHITE); g2.setFont(new Font("Segoe UI", Font.BOLD, 32)); g2.drawString("C", 16, 46); g2.dispose();
        return img;
    }

    static class RoundedLineBorder implements Border {
        // Border color, radius, thickness
        private final Color c; private final int r; private final float t;
        RoundedLineBorder(Color color, int radius, float thick) { c = color; r = radius; t = thick; }
        @Override public void paintBorder(Component comp, Graphics g, int x, int y, int w, int h) {
            // Draw antialiased rounded rectangle
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c); g2.setStroke(new BasicStroke(t)); g2.drawRoundRect(x, y, w-1, h-1, r, r); g2.dispose();
        }
        // Insets for inside spacing
        @Override public Insets getBorderInsets(Component comp) { return new Insets(4,4,4,4); }
        // Border is not fully opaque
        @Override public boolean isBorderOpaque() { return false; }
    }

    public static void main(String[] args) {
        // Enable system font antialiasing
        System.setProperty("awt.useSystemAAFontSettings", "on");
        // Use native OS look and feel
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        // Launch GUI on event thread
        SwingUtilities.invokeLater(ClientGUI::new);
    }
}