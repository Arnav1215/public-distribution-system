import com.formdev.flatlaf.FlatClientProperties;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.border.*;

public class LoginFrame extends JFrame {

    private final JTextField tfUser = new JTextField(18);
    private final JPasswordField tfPass = new JPasswordField(18);
    private final JLabel lblErr = new JLabel(" ");
    private static final String FAMILY = pickFamily();
    private static final Color BRAND_TOP = PDSApp.SIDEBAR_BG;
    private static final Color BRAND_BOT = PDSApp.SIDEBAR_SEC;
    private static final Color ACCENT = PDSApp.ACCENT;
    private static final Color TEXT  = PDSApp.TEXT_PRIMARY;
    private static final Color MUTED = PDSApp.TEXT_MUTED;
    private static final Color BORDER = PDSApp.BORDER_CLR;
    private static final Color DANGER = PDSApp.DANGER;

    private static String pickFamily() {
        String[] prefs = {"Inter","SF Pro Text","Segoe UI","Helvetica Neue","Arial"};
        java.util.Set<String> avail = new java.util.HashSet<>();
        for (String f : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
            avail.add(f);
        for (String p : prefs) if (avail.contains(p)) return p;
        return "SansSerif";
    }

    public LoginFrame() {
        super("PDS Portal — Sign In");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 700);
        setMinimumSize(new Dimension(960, 600));
        setLocationRelativeTo(null);
        setContentPane(buildUI());
        setVisible(true);
        // Animate card in after frame shows
        SwingUtilities.invokeLater(this::animateCardIn);
    }

    private JPanel loginCard;
    private void animateCardIn() {
        if (loginCard == null) return;
        final float[] alpha = {0f};
        final int[] offY  = {30};
        loginCard.setVisible(false);
        Timer t = new Timer(12, null);
        t.addActionListener(e -> {
            alpha[0] += 0.06f; offY[0] -= 2;
            if (alpha[0] >= 1f) { alpha[0] = 1f; offY[0] = 0; ((Timer)e.getSource()).stop(); }
            loginCard.setVisible(true);
            loginCard.putClientProperty("alpha", alpha[0]);
            loginCard.putClientProperty("offY",  offY[0]);
            loginCard.repaint();
        });
        t.start();
    }

    private JPanel buildUI() {
        JPanel root = new JPanel(new GridLayout(1, 2));
        root.add(buildBrandPanel());
        root.add(buildLoginPanel());
        return root;
    }
    private JPanel buildBrandPanel() {
        JPanel brand = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, BRAND_TOP, getWidth(), getHeight(), BRAND_BOT);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(255, 255, 255, 12));
                g2.setStroke(new BasicStroke(60f));
                g2.drawArc(getWidth() - 180, -80, 280, 280, 0, 360);
                g2.setColor(new Color(255, 255, 255, 7));
                g2.setStroke(new BasicStroke(80f));
                g2.drawArc(-100, getHeight() - 160, 260, 260, 0, 360);
                GradientPaint accent = new GradientPaint(0, 0, ACCENT, getWidth(), 0, new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 0));
                g2.setPaint(accent);
                g2.setStroke(new BasicStroke(1f));
                g2.fillRect(0, getHeight() - 2, getWidth(), 2);
                g2.setColor(new Color(255, 255, 255, 15));
                for (int x = 20; x < getWidth(); x += 28)
                    for (int y = 20; y < getHeight(); y += 28)
                        g2.fillOval(x, y, 2, 2);
                g2.dispose();
            }
        };
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBorder(new EmptyBorder(64, 60, 60, 60));

        // Emblem
        JLabel emblem = new JLabel() {
            private float pulse = 0f;
            { Timer t = new Timer(40, e -> { pulse += 0.06f; repaint(); }); t.start(); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                float glow = (float)(0.06 + 0.04 * Math.sin(pulse));
                g2.setColor(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), (int)(glow * 255)));
                g2.fillOval(-10, -10, getWidth() + 20, getHeight() + 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        try {
            ImageIcon icon = new ImageIcon(PDSApp.class.getResource("/logo.png"));
            Image scaled = icon.getImage().getScaledInstance(80, 116, Image.SCALE_SMOOTH);
            emblem.setIcon(new ImageIcon(scaled));
        } catch (Exception e) {}
        emblem.setAlignmentX(LEFT_ALIGNMENT);

        JLabel satyam = new JLabel("Satyameva Jayate");
        satyam.setFont(new Font(FAMILY, Font.PLAIN, 12));
        satyam.setForeground(MUTED); // Slate-400
        satyam.setAlignmentX(LEFT_ALIGNMENT);

        JLabel title1 = new JLabel("Public Distribution");
        title1.setFont(new Font(FAMILY, Font.BOLD, 36));
        title1.setForeground(Color.WHITE);
        title1.setAlignmentX(LEFT_ALIGNMENT);

        JLabel title2 = new JLabel("System Portal");
        title2.setFont(new Font(FAMILY, Font.BOLD, 36));
        title2.setForeground(Color.WHITE);
        title2.setAlignmentX(LEFT_ALIGNMENT);

        // Animated accent rule
        JPanel rule = new JPanel() {
            private int w = 0;
            { Timer t = new Timer(10, e -> { w = Math.min(w + 3, 72); repaint(); }); t.start(); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(ACCENT);
                g2.fillRoundRect(0, 0, w, getHeight(), 3, 3);
                g2.dispose();
            }
        };
        rule.setOpaque(false);
        rule.setMaximumSize(new Dimension(72, 3));
        rule.setPreferredSize(new Dimension(72, 3));
        rule.setAlignmentX(LEFT_ALIGNMENT);

        JLabel sub = new JLabel("<html>Ministry of Consumer Affairs, Food &amp;<br/>Public Distribution — Government of India</html>");
        sub.setFont(new Font(FAMILY, Font.PLAIN, 14));
        sub.setForeground(MUTED);
        sub.setAlignmentX(LEFT_ALIGNMENT);

        brand.add(emblem);
        brand.add(Box.createVerticalStrut(2));
        brand.add(satyam);
        brand.add(Box.createVerticalStrut(72));
        brand.add(title1);
        brand.add(title2);
        brand.add(Box.createVerticalStrut(20));
        brand.add(rule);
        brand.add(Box.createVerticalStrut(18));
        brand.add(sub);
        brand.add(Box.createVerticalGlue());

        // Trust badges
        JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        badges.setOpaque(false);
        badges.setAlignmentX(LEFT_ALIGNMENT);
        badges.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        badges.add(badge("🔒 SSL Secured"));
        badges.add(badge("☑ ISO 27001"));
        badges.add(badge("⊕ Audit Ready"));
        brand.add(badges);
        return brand;
    }

    private JLabel badge(String t) {
        JLabel l = new JLabel(t) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 18));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setFont(new Font(FAMILY, Font.BOLD, 10));
        l.setForeground(Color.WHITE);
        l.setBorder(new EmptyBorder(5, 10, 5, 10));
        l.setOpaque(false);
        return l;
    }
    private JPanel buildLoginPanel() {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setBackground(PDSApp.PAGE_BG);

        loginCard = new JPanel() {
            private float alpha = 0f;
            @Override protected void paintComponent(Graphics g) {
                Object a = getClientProperty("alpha");
                if (a instanceof Float f) alpha = f; else alpha = 1f;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                // Premium diffuse shadow
                for (int i = 0; i < 10; i++) {
                    g2.setColor(new Color(0, 0, 0, i * 2));
                    g2.fill(new RoundRectangle2D.Float(i, i + 4, getWidth() - i * 2, getHeight() - i * 2, 24, 24));
                }
                g2.setColor(PDSApp.CARD_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 24, 24));
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 24, 24));
                g2.dispose();
            }
        };
        loginCard.setOpaque(false);
        loginCard.setLayout(new BoxLayout(loginCard, BoxLayout.Y_AXIS));
        loginCard.setBorder(new EmptyBorder(40, 44, 40, 44));
        loginCard.setPreferredSize(new Dimension(400, 560));

        JLabel h1 = new JLabel("Sign in to Portal");
        h1.setFont(new Font(FAMILY, Font.BOLD, 26));
        h1.setForeground(TEXT);
        h1.setAlignmentX(LEFT_ALIGNMENT);
        JLabel sub = new JLabel("Authorized access only — all actions are audited.");
        sub.setFont(new Font(FAMILY, Font.PLAIN, 13));
        sub.setForeground(MUTED);
        sub.setAlignmentX(LEFT_ALIGNMENT);

        loginCard.add(h1);
        loginCard.add(Box.createVerticalStrut(6));
        loginCard.add(sub);
        loginCard.add(Box.createVerticalStrut(30));

        loginCard.add(fieldLabel("Officer ID / Username"));
        loginCard.add(Box.createVerticalStrut(6));
        styleInput(tfUser, "Enter your Officer ID");
        tfUser.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        tfUser.setAlignmentX(LEFT_ALIGNMENT);
        loginCard.add(tfUser);
        loginCard.add(Box.createVerticalStrut(16));

        loginCard.add(fieldLabel("Password"));
        loginCard.add(Box.createVerticalStrut(6));
        styleInput(tfPass, "Enter your password");
        tfPass.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        tfPass.setAlignmentX(LEFT_ALIGNMENT);
        loginCard.add(tfPass);
        loginCard.add(Box.createVerticalStrut(10));

        lblErr.setFont(new Font(FAMILY, Font.BOLD, 12));
        lblErr.setForeground(DANGER);
        lblErr.setAlignmentX(LEFT_ALIGNMENT);
        loginCard.add(lblErr);
        loginCard.add(Box.createVerticalStrut(10));

        JButton btn = buildSignInBtn();
        btn.setAlignmentX(LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        loginCard.add(btn);
        loginCard.add(Box.createVerticalStrut(24));

        JLabel demoHdr = new JLabel("QUICK LOGIN — DEMO");
        demoHdr.setFont(new Font(FAMILY, Font.BOLD, 10));
        demoHdr.setForeground(MUTED);
        demoHdr.setAlignmentX(LEFT_ALIGNMENT);
        loginCard.add(demoHdr);
        loginCard.add(Box.createVerticalStrut(8));

        JPanel demo = new JPanel(new GridLayout(1, 3, 8, 0));
        demo.setOpaque(false);
        demo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        demo.setAlignmentX(LEFT_ALIGNMENT);
        demo.add(demoBtn("Admin",       "admin",   "admin123"));
        demo.add(demoBtn("Shop",        "shop101", "shop101"));
        demo.add(demoBtn("Beneficiary", "ben1",    "pass123"));
        loginCard.add(demo);
        loginCard.add(Box.createVerticalStrut(28));

        JLabel ver = new JLabel("v2.0  ·  © 2026 Government of India");
        ver.setFont(new Font(FAMILY, Font.PLAIN, 11));
        ver.setForeground(new Color(0x71717A));
        ver.setAlignmentX(LEFT_ALIGNMENT);
        loginCard.add(ver);

        tfPass.addActionListener(e -> doLogin());
        wrap.add(loginCard);
        return wrap;
    }

    private JLabel fieldLabel(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font(FAMILY, Font.BOLD, 12));
        l.setForeground(PDSApp.TEXT_SEC);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private String toHexString(Color c) {
        return String.format("#%06X", (0xFFFFFF & c.getRGB()));
    }

    private void styleInput(JTextField f, String placeholder) {
        f.setFont(new Font(FAMILY, Font.PLAIN, 14));
        f.setForeground(TEXT);
        f.setBackground(PDSApp.PAGE_BG);
    
        f.putClientProperty(
            FlatClientProperties.STYLE,
            "arc:12; " +
            "focusedBorderColor:" + toHexString(ACCENT) + "; " +
            "focusWidth:2"
        );
    
        f.setBorder(new EmptyBorder(10, 14, 10, 14));
    
        try {
            f.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, placeholder);
        } catch (Throwable ignore) {}
    }

    private JButton buildSignInBtn() {
        JButton b = new PDSApp.RippleButton("Sign In  →", ACCENT, PDSApp.ACCENT_HOV, Color.WHITE);
        b.setFont(new Font(FAMILY, Font.BOLD, 15));
        b.setBorder(new EmptyBorder(13, 0, 13, 0));
        b.addActionListener(e -> doLogin());
        return b;
    }

    private JButton demoBtn(String role, String user, String pass) {
        JButton b = new JButton("<html><center><b>" + role + "</b><br><span style='color:#71717A'>" + user + "</span></center></html>") {
            private boolean hov = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                public void mouseExited(MouseEvent e)  { hov = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? PDSApp.ACCENT_BG : PDSApp.CARD_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(hov ? ACCENT : BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font(FAMILY, Font.PLAIN, 11));
        b.setForeground(TEXT);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> { tfUser.setText(user); tfPass.setText(pass); });
        return b;
    }

    private void doLogin() {
        String username = tfUser.getText().trim();
        String password = new String(tfPass.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            shakeError("Please enter your Officer ID and password.");
            return;
        }
        try {
            LoginController.UserSession session = LoginController.authenticate(username, password);
            if (session == null) {
                shakeError("Invalid Officer ID or password.");
                tfPass.setText("");
                return;
            }
            String role = session.role;
            int linkedId = session.linkedId;
            String uname = session.username;
            fadeOutAndSwitch(() -> {
                try {
                    switch (role) {
                        case "ADMIN" -> { dispose(); new AdminFrame(uname); }
                        case "SHOP_OPERATOR" -> { dispose(); new ShopFrame(uname, linkedId); }
                        case "BENEFICIARY" -> { dispose(); new BeneficiaryFrame(uname, linkedId); }
                        default -> lblErr.setText("Unknown role: " + role);
                    }
                } catch (Exception ex) {
                    lblErr.setText("Error: " + ex.getMessage());
                    ex.printStackTrace();
                    setVisible(true);
                }
            });
        } catch (SQLException ex) {
            shakeError("DB Error: " + ex.getMessage());
        }
    }

    private void shakeError(String msg) {
        lblErr.setText(msg);
        int origX = loginCard.getX();
        Timer shake = new Timer(14, null);
        final int[] step = {0};
        int[] offsets = {-6, 6, -5, 5, -3, 3, -1, 1, 0};
        shake.addActionListener(e -> {
            if (step[0] < offsets.length) {
                loginCard.setLocation(origX + offsets[step[0]], loginCard.getY());
                step[0]++;
            } else {
                loginCard.setLocation(origX, loginCard.getY());
                ((Timer) e.getSource()).stop();
            }
        });
        shake.start();
    }

    private void fadeOutAndSwitch(Runnable action) {
        JPanel glass = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Object a = getClientProperty("alpha");
                float alpha = (a instanceof Float f) ? f : 0f;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2.setColor(new Color(0x0D1B2A));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        glass.setOpaque(false);
        glass.setBounds(0, 0, getWidth(), getHeight());
        glass.setVisible(true);
        getLayeredPane().add(glass, JLayeredPane.POPUP_LAYER);

        Timer t = new Timer(14, null);
        final float[] a = {0f};
        t.addActionListener(e -> {
            a[0] += 0.07f;
            if (a[0] >= 1f) { a[0] = 1f; ((Timer)e.getSource()).stop(); action.run(); }
            glass.putClientProperty("alpha", a[0]);
            glass.repaint();
        });
        t.start();
    }

    public static void main(String[] args) throws Exception {
        PDSApp.installLookAndFeel();
        PDSApp.conn = DB.getConnection();
        SwingUtilities.invokeLater(LoginFrame::new);
    }
}
