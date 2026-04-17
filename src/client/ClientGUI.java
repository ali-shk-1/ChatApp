package client;

import common.EncryptionUtil;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import javax.sound.sampled.*;
import javax.swing.border.Border;

public class ClientGUI {

    // ─────────────────── ENHANCED MODERN PALETTE ────────────────────────────
    private static final Color BG_DARK          = new Color(15,  23,  42);  // Slate 900
    private static final Color BG_PANEL         = new Color(30,  41,  59);  // Slate 800
    private static final Color BG_INPUT         = new Color(15,  23,  42);  // Slate 900
    private static final Color ACCENT_PRIMARY   = new Color(79,  70,  229); // Indigo 600
    private static final Color ACCENT_SECONDARY = new Color(6,   182, 212); // Cyan 500
    private static final Color NEON_BLUE        = new Color(59,  130, 246); // Blue 500
    private static final Color MSG_YOU_FG       = new Color(186, 230, 253); // Sky 200
    private static final Color MSG_OTHER_FG     = new Color(248, 250, 252); // Slate 50
    private static final Color MSG_HISTORY_FG   = new Color(148, 163, 184); // Slate 400
    private static final Color MSG_SYSTEM_FG    = new Color(52,  211, 153); // Emerald 400
    private static final Color TEXT_MUTED       = new Color(100, 116, 139); // Slate 500
    private static final Color BORDER_DIM       = new Color(51,  65,  85);  // Slate 700

    // ─────────────────── FONTS & UI SCALING ─────────────────────────────────
    private static final Font FONT_CHAT;
    private static final Font FONT_INPUT;
    private static final Font FONT_FOOTER;
    private static final Font FONT_SYMBOL;

    static {
        FONT_SYMBOL = findBestSymbolFont(18f);
        FONT_CHAT   = new Font("Segoe UI", Font.BOLD, 15);
        FONT_INPUT  = new Font("Segoe UI", Font.PLAIN, 15);
        FONT_FOOTER = new Font("Segoe UI", Font.BOLD | Font.ITALIC, 12);
    }

    private static Font findBestSymbolFont(float size) {
        String[] fonts = {"Segoe UI Symbol", "Segoe UI Emoji", "Arial Unicode MS", "Symbola", "DejaVu Sans"};
        for (String f : fonts) {
            Font font = new Font(f, Font.PLAIN, (int) size);
            if (!font.getFamily().equals("Dialog") || f.equals("Dialog")) return font;
        }
        return new Font("Dialog", Font.PLAIN, (int) size);
    }

    // ── Icons & Emoji Set ──────────────────────────────────────────────────
    private static final String DECO   = "\u2726"; // ✦ Sparkle
    private static final String LIVE   = "\u25CF"; // ●
    private static final String BULLET = "\u2500\u2500"; // ──

    private static final String[] EMOJIS = {
            "\u263A", "\u263B", "\u2605", "\u2606", "\u2764", "\u2661", "\u266A", "\u266B", "\u2600", "\u2601",
            "\u2602", "\u2744", "\u26A1", "\u2713", "\u2717", "\u2709", "\u2699", "\u260E", "\u2302", "\u25C6",
            "\u2666", "\u2663", "\u2660", "\u2726", "\u2727", "\u272A", "\u272B", "\u27A4", "\u2191", "\u2193"
    };

    // ─────────────────── STATE ──────────────────────────────────────────
    private JFrame         frame;
    private JTextPane      chatPane;
    private StyledDocument chatDoc;
    private JTextField     inputField;
    private JLabel         typingLabel;
    private PrintWriter    out;
    private BufferedReader in;
    private String         username;

    public ClientGUI() {
        buildUI();
        connectToServer();
    }

    private void buildUI() {
        frame = new JFrame("ChatApp");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(620, 780);
        frame.setMinimumSize(new Dimension(500, 600));
        frame.setLocationRelativeTo(null);
        frame.setIconImage(makeAppIcon());

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_DARK);
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildChat(),   BorderLayout.CENTER);
        root.add(buildSouth(),  BorderLayout.SOUTH);

        frame.setContentPane(root);
        frame.setVisible(true);
    }

    private JPanel buildHeader() {
        JPanel hdr = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, ACCENT_PRIMARY, getWidth(), 0, ACCENT_SECONDARY));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        hdr.setOpaque(false);
        hdr.setPreferredSize(new Dimension(0, 76));
        hdr.setBorder(BorderFactory.createEmptyBorder(0, 22, 0, 22));

        JLabel title = new JLabel(DECO + "  CHATAPP  " + DECO);
        title.setFont(findBestSymbolFont(26f).deriveFont(Font.BOLD));
        title.setForeground(Color.WHITE);

        JLabel live = new JLabel("  " + LIVE + " LIVE");
        live.setFont(new Font("Segoe UI", Font.BOLD, 11));
        live.setForeground(new Color(165, 243, 252));

        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.add(title, BorderLayout.CENTER);
        left.add(live,  BorderLayout.SOUTH);

        JLabel lock = new JLabel("Encrypted");
        lock.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lock.setForeground(new Color(255, 255, 255, 190));

        hdr.add(left, BorderLayout.WEST);
        hdr.add(lock, BorderLayout.EAST);
        return hdr;
    }

    private JScrollPane buildChat() {
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setBackground(BG_DARK);
        chatPane.setFont(FONT_CHAT);
        chatPane.setMargin(new Insets(20, 25, 20, 25));
        chatDoc = chatPane.getStyledDocument();

        JScrollPane scroll = new JScrollPane(chatPane);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBackground(BG_DARK);
        scroll.getViewport().setBackground(BG_DARK);
        styleScrollBar(scroll.getVerticalScrollBar());
        return scroll;
    }

    private JPanel buildSouth() {
        JPanel south = new JPanel(new BorderLayout());
        south.setBackground(BG_PANEL);
        south.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_DIM));

        typingLabel = new JLabel(" ");
        typingLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        typingLabel.setForeground(TEXT_MUTED);
        typingLabel.setBorder(BorderFactory.createEmptyBorder(5, 18, 0, 0));

        south.add(typingLabel,     BorderLayout.NORTH);
        south.add(buildEmojiBar(), BorderLayout.CENTER);
        south.add(buildInputRow(), BorderLayout.SOUTH);

        JLabel footer = new JLabel("  * Made by Ali");
        footer.setFont(FONT_FOOTER);
        footer.setForeground(new Color(129, 140, 248));
        footer.setBorder(BorderFactory.createEmptyBorder(2, 12, 8, 0));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG_PANEL);
        wrapper.add(south,  BorderLayout.CENTER);
        wrapper.add(footer, BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel buildEmojiBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 5));
        bar.setBackground(BG_PANEL);
        bar.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));

        for (String sym : EMOJIS) {
            JButton btn = new JButton(sym) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (getModel().isRollover()) {
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
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setPreferredSize(new Dimension(38, 34));
            btn.addActionListener(e -> {
                inputField.setText(inputField.getText() + sym);
                inputField.requestFocus();
            });
            bar.add(btn);
        }
        return bar;
    }

    private JPanel buildInputRow() {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(BG_PANEL);
        row.setBorder(BorderFactory.createEmptyBorder(6, 12, 10, 12));

        inputField = new JTextField();
        inputField.setFont(FONT_INPUT);
        inputField.setBackground(BG_INPUT);
        inputField.setForeground(new Color(248, 250, 252));
        inputField.setCaretColor(ACCENT_SECONDARY);
        inputField.setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(BORDER_DIM, 12, 1.5f),
                BorderFactory.createEmptyBorder(9, 14, 9, 14)
        ));

        JButton send = gradientButton("Send  >", ACCENT_PRIMARY, ACCENT_SECONDARY, 110, 44);
        send.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
        inputField.addKeyListener(new KeyAdapter() {
            @Override public void keyTyped(KeyEvent e) { if (out != null) out.println(EncryptionUtil.encrypt("/typing")); }
        });

        row.add(inputField, BorderLayout.CENTER);
        row.add(send,       BorderLayout.EAST);
        return row;
    }

    private void connectToServer() {
        JDialog dlg = new JDialog(frame, "Connect to ChatApp", true);
        dlg.setUndecorated(true);
        dlg.setSize(400, 310);
        dlg.setLocationRelativeTo(frame);

        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_PANEL);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
            }
        };
        card.setOpaque(false);

        JPanel hdr = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, ACCENT_PRIMARY, getWidth(), 0, ACCENT_SECONDARY));
                g2.fillRoundRect(0, 0, getWidth(), getHeight() + 20, 20, 20);
                g2.fillRect(0, getHeight() - 20, getWidth(), 20);
                g2.dispose();
            }
        };
        hdr.setOpaque(false);
        hdr.setPreferredSize(new Dimension(0, 72));
        hdr.setBorder(BorderFactory.createEmptyBorder(14, 24, 10, 24));

        JLabel hTitle = new JLabel(DECO + "  CHATAPP");
        hTitle.setFont(findBestSymbolFont(24f).deriveFont(Font.BOLD));
        hTitle.setForeground(Color.WHITE);
        hdr.add(hTitle, BorderLayout.CENTER);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1.0;

        JTextField tfName = dlgField();
        JTextField tfSecret = dlgField();

        gc.gridy = 0; form.add(smallHint("Name"), gc);
        gc.gridy = 1; gc.insets = new Insets(0,0,14,0); form.add(tfName, gc);
        gc.gridy = 2; gc.insets = new Insets(0,0,0,0); form.add(smallHint("Secret Number"), gc);
        gc.gridy = 3; gc.insets = new Insets(0,0,20,0); form.add(tfSecret, gc);

        JButton connectBtn = gradientButton("Connect", ACCENT_PRIMARY, ACCENT_SECONDARY, 330, 44);
        gc.gridy = 4; form.add(connectBtn, gc);

        card.add(hdr, BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);
        dlg.setContentPane(card);

        connectBtn.addActionListener(e -> {
            String name = tfName.getText().trim();
            String portTx = tfSecret.getText().trim();
            if (name.isEmpty() || portTx.isEmpty()) { shake(dlg); return; }
            username = name;
            dlg.dispose();
            try {
                Socket socket = new Socket("bore.pub", Integer.parseInt(portTx));
                in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                EncryptionUtil.decrypt(in.readLine());
                out.println(EncryptionUtil.encrypt(username));
                new Thread(this::listenForMessages).start();
                SwingUtilities.invokeLater(() -> appendSystem("Connected to ChatApp as " + username));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> appendSystem("Connection failed."));
            }
        });
        dlg.setVisible(true);
    }

    private void listenForMessages() {
        try {
            String raw;
            while ((raw = in.readLine()) != null) {
                String msg = EncryptionUtil.decrypt(raw);
                if (msg.startsWith("/typing ")) {
                    SwingUtilities.invokeLater(() -> {
                        typingLabel.setText("  " + msg.substring(8) + " is typing...");
                        new Timer(3000, e -> typingLabel.setText(" ")).start();
                    });
                } else if (!msg.startsWith("/")) {
                    SwingUtilities.invokeLater(() -> {
                        appendIncoming(msg, msg.startsWith("[History]"));
                        playPing();
                    });
                }
            }
        } catch (IOException e) { SwingUtilities.invokeLater(() -> appendSystem("Connection lost.")); }
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty() || out == null) return;
        out.println(EncryptionUtil.encrypt(text));
        inputField.setText("");
        if (!text.startsWith("/")) appendSent("You:  " + text);
    }

    private void appendSent(String text) {
        appendStyled(text, MSG_YOU_FG, StyleConstants.ALIGN_RIGHT, 15);
    }

    private void appendIncoming(String text, boolean hist) {
        appendStyled(text, hist ? MSG_HISTORY_FG : MSG_OTHER_FG, StyleConstants.ALIGN_LEFT, 15);
    }

    private void appendSystem(String text) {
        appendStyled(BULLET + " " + text + " " + BULLET, MSG_SYSTEM_FG, StyleConstants.ALIGN_CENTER, 12);
    }

    private void appendStyled(String text, Color c, int align, int size) {
        try {
            SimpleAttributeSet a = new SimpleAttributeSet();
            StyleConstants.setForeground(a, c);
            StyleConstants.setAlignment(a, align);
            StyleConstants.setFontFamily(a, "Segoe UI");
            StyleConstants.setFontSize(a, size);
            StyleConstants.setBold(a, true);
            StyleConstants.setSpaceBelow(a, 8f);

            int start = chatDoc.getLength();
            chatDoc.insertString(start, text + "\n", a);
            chatDoc.setParagraphAttributes(start, text.length() + 1, a, false);
            chatPane.setCaretPosition(chatDoc.getLength());
        } catch (BadLocationException ignored) {}
    }

    private void playPing() {
        new Thread(() -> {
            try {
                float sr = 44100f; int dur = (int)(sr * 0.08); byte[] buf = new byte[dur * 8];
                for (int i = 0; i < dur * 2; i++) {
                    short s = (short)(Math.sin(2 * Math.PI * 900 * i / sr) * 2000);
                    buf[i*4] = (byte)(s & 0xff); buf[i*4+1] = (byte)((s >> 8) & 0xff);
                }
                AudioFormat fmt = new AudioFormat(sr, 16, 2, true, false);
                Clip clip = (Clip) AudioSystem.getLine(new DataLine.Info(Clip.class, fmt));
                clip.open(fmt, buf, 0, buf.length); clip.start();
            } catch (Exception ignored) {}
        }).start();
    }

    private JLabel smallHint(String text) {
        JLabel l = new JLabel(text); l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(TEXT_MUTED); return l;
    }

    private JTextField dlgField() {
        JTextField tf = new JTextField(); tf.setFont(FONT_INPUT); tf.setBackground(BG_INPUT);
        tf.setForeground(new Color(248, 250, 252)); tf.setCaretColor(ACCENT_SECONDARY);
        tf.setPreferredSize(new Dimension(330, 42));
        tf.setBorder(BorderFactory.createCompoundBorder(new RoundedLineBorder(BORDER_DIM, 10, 1.5f), BorderFactory.createEmptyBorder(7, 14, 7, 14)));
        return tf;
    }

    private JButton gradientButton(String label, Color c1, Color c2, int w, int h) {
        JButton btn = new JButton(label) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, getModel().isRollover() ? c2 : c1, getWidth(), 0, getModel().isRollover() ? c1 : c2));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(Color.WHITE); g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 15)); btn.setFocusPainted(false);
        btn.setBorderPainted(false); btn.setContentAreaFilled(false);
        btn.setPreferredSize(new Dimension(w, h)); btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void shake(Window w) {
        Point o = w.getLocation(); Timer t = new Timer(25, null);
        int[] off = {8,-8, 6,-6, 4,-4, 0}; int[] s = {0};
        t.addActionListener(e -> { if (s[0] >= off.length) { w.setLocation(o); t.stop(); } else w.setLocation(o.x + off[s[0]++], o.y); });
        t.start();
    }

    private void styleScrollBar(JScrollBar sb) {
        sb.setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void configureScrollBarColors() { thumbColor = new Color(71, 85, 105); trackColor = BG_PANEL; }
            @Override protected JButton createDecreaseButton(int o) { return zeroBtn(); }
            @Override protected JButton createIncreaseButton(int o) { return zeroBtn(); }
            private JButton zeroBtn() { JButton b = new JButton(); b.setPreferredSize(new Dimension(0, 0)); return b; }
        });
    }

    private Image makeAppIcon() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setPaint(new GradientPaint(0, 0, ACCENT_PRIMARY, 64, 64, ACCENT_SECONDARY)); g2.fillOval(0, 0, 64, 64);
        g2.setColor(Color.WHITE); g2.setFont(new Font("Segoe UI", Font.BOLD, 32)); g2.drawString("C", 16, 46); g2.dispose();
        return img;
    }

    static class RoundedLineBorder implements Border {
        private final Color c; private final int r; private final float t;
        RoundedLineBorder(Color color, int radius, float thick) { c = color; r = radius; t = thick; }
        @Override public void paintBorder(Component comp, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c); g2.setStroke(new BasicStroke(t)); g2.drawRoundRect(x, y, w-1, h-1, r, r); g2.dispose();
        }
        @Override public Insets getBorderInsets(Component comp) { return new Insets(4,4,4,4); }
        @Override public boolean isBorderOpaque() { return false; }
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(ClientGUI::new);
    }
}