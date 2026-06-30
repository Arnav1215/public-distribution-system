import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.sql.*;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class PDSApp extends JFrame {

    // ── Database ──────────────────────────────────────────────────────────
    static final String DB_URL  = "jdbc:mysql://localhost:3306/PDS_DB?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true";
    static final String DB_USER = "root";
    static final String DB_PASS = "AP_project";
    static Connection conn;

    // ── Design Tokens ─────────────────────────────────────────────────────
    static final Color PAGE_BG = new Color(0xF5F5F5); // Cool-slate main background
    static final Color CARD_BG = new Color(0xFFFFFF); // Pure white cards
    static final Color BORDER_CLR = new Color(0xDCE3EA); // Cool-slate borders
    static final Color SOFT_BG = new Color(0xEEF2F6); // Secondary surface
    static final Color SIDEBAR_BG = new Color(0x111827); // Very dark slate/black (Gray 900)
    static final Color SIDEBAR_SEC = new Color(0x1F2937); // Sidebar inner borders (Gray 800)
    static final Color SIDEBAR_HOV = new Color(0x374151); // Sidebar hover background (Gray 700)
    static final Color SIDEBAR_ACT = new Color(0x3B82F6); // Primary Accent (Bright Blue Accent)
    static final Color SIDEBAR_TXT = new Color(0xE5E7EB); // Light gray text for sidebar (Gray 200)
    static final Color SIDEBAR_LBL = new Color(0x9CA3AF); // Muted labels for sidebar (Gray 400)
    static final Color TEXT_PRIMARY = new Color(0x1F2937); // Dark grey primary text
    static final Color TEXT_SEC = new Color(0x6B7280); // Medium grey secondary text
    static final Color TEXT_MUTED = new Color(0x9CA3AF); // Muted text
    static final Color ACCENT = new Color(0x5B8FD9); // Soft blue primary accent
    static final Color ACCENT_HOV = new Color(0x4A7FCB); // Primary Accent hover
    static final Color ACCENT_BG = new Color(0xE0ECFA); // Soft blue selection background
    static final Color SUCCESS = new Color(0x22C55E); // Success green
    static final Color SUCCESS_BG = new Color(0xDCFCE7); // Translucent green
    static final Color DANGER = new Color(0xEF4444); // Danger red
    static final Color DANGER_BG = new Color(0xFEE2E2); // Translucent red
    static final Color WARNING = new Color(0xF59E0B); // Warning amber
    static final Color WARNING_BG = new Color(0xFEF3C7); // Translucent amber
    static final Color INFO = new Color(0x5B8FD9); // Info accent blue
    static final Color INFO_BG = new Color(0xEEF2F6); // Translucent info bg
    private static final String FAMILY = pickFamily();
    static final Font FONT_H1 = new Font(FAMILY, Font.BOLD,   24); // Refined H1
    static final Font FONT_H2 = new Font(FAMILY, Font.BOLD,   16); // Refined H2
    static final Font FONT_LABEL = new Font(FAMILY, Font.PLAIN,  14); 
    static final Font FONT_BOLD = new Font(FAMILY, Font.BOLD,   14); 
    static final Font FONT_SMALL = new Font(FAMILY, Font.PLAIN,  12); 
    static final Font FONT_SBOLD = new Font(FAMILY, Font.BOLD,   12); 
    static final Font FONT_MONO = new Font("JetBrains Mono", Font.PLAIN, 13); // Premium Mono
    static final Font FONT_NUM = new Font(FAMILY, Font.BOLD,   32); // Refined Num

    private static String pickFamily() {
        String[] prefs = {"Inter", "SF Pro Text", "Segoe UI", "Helvetica Neue", "Arial"};
        java.util.Set<String> avail = new java.util.HashSet<>();
        for (String f : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())
            avail.add(f);
        for (String p : prefs) if (avail.contains(p)) return p;
        return "SansSerif";
    }

    // ── Shell ─────────────────────────────────────────────────────────────
    private CardLayout cards = new CardLayout();
    private JPanel content;
    private JButton[] navBtns = new JButton[5];
    private String[] navCards  = {"REQUEST","DISTRIBUTE","PORTFOLIO","STOCK","AUDIT"};
    private String[] navTitles = {"Place Request","Distribute Items","Beneficiary Portfolio","Stock Analysis","Audit Log"};
    private String[] navIconPaths = {
        "/resources/icons/overview.png",
        "/resources/icons/beneficiaries.png",
        "/resources/icons/shops.png",
        "/resources/icons/categories.png",
        "/resources/icons/commodities.png",
        "/resources/icons/suppliers.png",
        "/resources/icons/warehouses.png",
        "/resources/icons/rules.png",
        "/resources/icons/audit.png"
    };
    private JLabel breadcrumb = new JLabel();
    private JPanel toastLayer;
    private int currentNav = -1;

    public PDSApp() {
        super("PDS Portal — Public Distribution System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1340, 820);
        setMinimumSize(new Dimension(1100, 680));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(PAGE_BG);

        add(buildSidebar(), BorderLayout.WEST);

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(PAGE_BG);
        main.add(buildTopBar(),    BorderLayout.NORTH);

        // Content wrapper with toast overlay
        JLayeredPane layered = new JLayeredPane();
        layered.setLayout(new OverlayLayout(layered));
        content = new JPanel(cards);
        content.setBackground(PAGE_BG);
        content.add(new PlaceRequestPanel(),"REQUEST");
        content.add(new DistributePanel(), "DISTRIBUTE");
        content.add(new PortfolioPanel(),    "PORTFOLIO");
        content.add(new StockPanel(),        "STOCK");
        content.add(new AuditPanel(),        "AUDIT");
        layered.add(content,    JLayeredPane.DEFAULT_LAYER);
        toastLayer = new JPanel(null);
        toastLayer.setOpaque(false);
        layered.add(toastLayer, JLayeredPane.POPUP_LAYER);
        main.add(layered, BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);

        activateNav(0);
        setVisible(true);

        // Animate sidebar items on startup
        SwingUtilities.invokeLater(this::animateSidebarIn);
    }

    // ── Sidebar ───────────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel side = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, SIDEBAR_BG, 0, getHeight(), SIDEBAR_BG);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(240, 0));
        side.setBorder(new MatteBorder(0, 0, 0, 1, SIDEBAR_SEC));

        // Brand
        JPanel brand = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(SIDEBAR_SEC);
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.dispose();
            }
        };
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setMaximumSize(new Dimension(240, 88));
        brand.setPreferredSize(new Dimension(240, 100));
        brand.setBorder(new EmptyBorder(20, 16, 20, 16));
        brand.setAlignmentX(LEFT_ALIGNMENT);

        JLabel logoImg = new JLabel();
        try {
            ImageIcon icon = new ImageIcon(PDSApp.class.getResource("/logo.png"));
            Image scaled = icon.getImage().getScaledInstance(32, 46, Image.SCALE_SMOOTH);
            logoImg.setIcon(new ImageIcon(scaled));
        } catch (Exception e) {}

        JPanel brandText = new JPanel();
        brandText.setOpaque(false);
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));

        JLabel logo = new JLabel("PDS Portal");
        logo.setFont(new Font(FAMILY, Font.BOLD, 17));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Government of India");
        sub.setFont(new Font(FAMILY, Font.PLAIN, 11));
        sub.setForeground(SIDEBAR_LBL);
        sub.setAlignmentX(LEFT_ALIGNMENT);

        brandText.add(logo);
        brandText.add(Box.createVerticalStrut(2));
        brandText.add(sub);

        JPanel brandWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        brandWrapper.setOpaque(false);
        brandWrapper.add(logoImg);
        brandWrapper.add(brandText);

        brand.add(brandWrapper);
        side.add(brand);
        side.add(Box.createVerticalStrut(16));

        side.add(sideLabel("OPERATIONS"));
        navBtns[0] = sideNavBtn("Place Request",    0);
        navBtns[1] = sideNavBtn("Distribute Items", 1);
        side.add(navBtns[0]); side.add(Box.createVerticalStrut(2));
        side.add(navBtns[1]); side.add(Box.createVerticalStrut(18));

        side.add(sideLabel("ANALYTICS"));
        navBtns[2] = sideNavBtn("Beneficiary Portfolio", 2);
        navBtns[3] = sideNavBtn("Stock Analysis",        3);
        side.add(navBtns[2]); side.add(Box.createVerticalStrut(2));
        side.add(navBtns[3]); side.add(Box.createVerticalStrut(18));

        side.add(sideLabel("GOVERNANCE"));
        navBtns[4] = sideNavBtn("Audit Log", 4);
        side.add(navBtns[4]);

        side.add(Box.createVerticalGlue());

        // Footer
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, new Color(0x1A2E44)),
            new EmptyBorder(14, 16, 14, 16)));
        footer.setMaximumSize(new Dimension(240, 52));
        footer.setAlignmentX(LEFT_ALIGNMENT);
        JLabel ver = new JLabel("v2.0  ·  Civic Indigo");
        ver.setFont(new Font(FAMILY, Font.PLAIN, 11));
        ver.setForeground(SIDEBAR_LBL);
        footer.add(ver, BorderLayout.WEST);

        // Online indicator
        JLabel dot = new JLabel("● Online");
        dot.setFont(new Font(FAMILY, Font.BOLD, 10));
        dot.setForeground(SUCCESS);
        footer.add(dot, BorderLayout.EAST);
        side.add(footer);

        return side;
    }

    private JLabel sideLabel(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font(FAMILY, Font.BOLD, 11));
        l.setForeground(SIDEBAR_LBL);
        l.setBorder(new EmptyBorder(0, 16, 4, 0));
        l.setMaximumSize(new Dimension(240, 22));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JButton sideNavBtn(String text, int idx) {
        AnimatedSidebarButton b = new AnimatedSidebarButton(text, FAMILY);
        ImageIcon icon = new ImageIcon(
            getClass().getResource(navIconPaths[idx])
        );
        Image img = icon.getImage().getScaledInstance(
            18,
            18,
            Image.SCALE_SMOOTH
        );
    
        b.setIcon(new ImageIcon(img));
        b.setIconTextGap(12);
        b.setMaximumSize(new Dimension(240, 42));
        b.setPreferredSize(new Dimension(240, 42));
        b.setAlignmentX(LEFT_ALIGNMENT);
        b.addActionListener(e -> activateNav(idx));
    
        return b;
    }

    private void activateNav(int idx) {
        if (currentNav == idx) return;
        currentNav = idx;
        for (int i = 0; i < navBtns.length; i++) {
            if (navBtns[i] instanceof AnimatedSidebarButton sb) sb.setActive(i == idx);
        }
        fadeTransition(() -> cards.show(content, navCards[idx]));
        breadcrumb.setText("Home  ›  " + navTitles[idx]);
    }

    private void animateSidebarIn() {
        for (int i = 0; i < navBtns.length; i++) {
            final int fi = i;
            Timer t = new Timer(60 * i, e -> {
                if (navBtns[fi] instanceof AnimatedSidebarButton sb) sb.slideIn();
                ((Timer)e.getSource()).stop();
            });
            t.start();
        }
    }

    // ── Fade transition ───────────────────────────────────────────────────
    private void fadeTransition(Runnable switchPanel) {
        Timer fadeOut = new Timer(12, null);
        final float[] alpha = {1f};
        fadeOut.addActionListener(e -> {
            alpha[0] -= 0.1f;
            if (alpha[0] <= 0f) {
                alpha[0] = 0f;
                ((Timer)e.getSource()).stop();
                switchPanel.run();
                Timer fadeIn = new Timer(12, null);
                fadeIn.addActionListener(e2 -> {
                    alpha[0] += 0.1f;
                    if (alpha[0] >= 1f) { alpha[0] = 1f; ((Timer)e2.getSource()).stop(); }
                    content.setBackground(blendColor(PAGE_BG, Color.WHITE, alpha[0]));
                    content.repaint();
                });
                fadeIn.start();
            }
            content.setBackground(blendColor(PAGE_BG, Color.WHITE, alpha[0]));
            content.repaint();
        });
        fadeOut.start();
    }

    private Color blendColor(Color a, Color b, float t) {
        float r = a.getRed()   * t + b.getRed()   * (1 - t);
        float g = a.getGreen() * t + b.getGreen() * (1 - t);
        float bl = a.getBlue() * t + b.getBlue()  * (1 - t);
        return new Color(Math.min(255,(int)r), Math.min(255,(int)g), Math.min(255,(int)bl));
    }

    private static String toHexString(Color c) {
        return String.format("#%06X", (0xFFFFFF & c.getRGB()));
    }

    // ── Top bar ───────────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(0, 0, new Color(0xFFFFFF), getWidth(), 0, new Color(0xF7FAFC));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(BORDER_CLR);
                g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(12, 24, 12, 24));
        bar.setPreferredSize(new Dimension(0, 56));

        // Breadcrumb with icon
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        JLabel homeIco = new JLabel("⌂");
        homeIco.setFont(new Font(FAMILY, Font.PLAIN, 14));
        homeIco.setForeground(TEXT_MUTED);
        breadcrumb.setFont(new Font(FAMILY, Font.PLAIN, 13));
        breadcrumb.setForeground(TEXT_SEC);
        left.add(homeIco);
        left.add(breadcrumb);
        bar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        // Role badge
        JLabel role = new JLabel("District Officer");
        role.setFont(new Font(FAMILY, Font.BOLD, 11));
        role.setForeground(ACCENT);
        role.setBackground(ACCENT_BG);
        role.setOpaque(true);
        role.setBorder(new EmptyBorder(5, 12, 5, 12));
        // arc style removed to prevent UnknownStyleException on JLabel
        
        // Avatar
        JLabel avatar = new JLabel("RS") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatar.setFont(new Font(FAMILY, Font.BOLD, 11));
        avatar.setForeground(Color.WHITE);
        avatar.setPreferredSize(new Dimension(32, 32));
        avatar.setHorizontalAlignment(SwingConstants.CENTER);
        avatar.setVerticalAlignment(SwingConstants.CENTER);
        avatar.setOpaque(false);

        right.add(role);
        right.add(avatar);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ── Toast notification ────────────────────────────────────────────────
    static void showAlert(Component parent, String msg, boolean err) {
        Container window = SwingUtilities.getWindowAncestor(parent);
        if (window instanceof JFrame frame) {
            showGenericToast(frame, msg, err);
        } else {
            JOptionPane.showMessageDialog(parent, msg,
                err ? "Error" : "Success",
                err ? JOptionPane.ERROR_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private static void showGenericToast(JFrame frame, String msg, boolean err) {
        Color bg  = err ? new Color(0xC0392B) : new Color(0x1A7A4A);
        Color fg  = Color.WHITE;

        JPanel toast = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.dispose();
            }
        };
        toast.setOpaque(false);

        JLabel icon = new JLabel(err ? "✕" : "✓");
        icon.setFont(new Font(FAMILY, Font.BOLD, 14));
        icon.setForeground(fg);

        JLabel lbl = new JLabel(msg);
        lbl.setFont(new Font(FAMILY, Font.PLAIN, 13));
        lbl.setForeground(fg);

        toast.add(icon);
        toast.add(lbl);
        toast.setSize(toast.getPreferredSize().width + 40, 46);

        JLayeredPane lp = frame.getLayeredPane();
        Dimension size = lp.getSize();
        int tx = size.width - toast.getWidth() - 24;
        int ty = size.height - toast.getHeight() - 24;
        toast.setLocation(tx, ty);
        
        lp.add(toast, JLayeredPane.POPUP_LAYER);
        final int targetY = ty;
        final int startY  = ty + 30;
        toast.setLocation(tx, startY);
        toast.setVisible(true);

        ComponentListener resizeListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Dimension sz = lp.getSize();
                toast.setLocation(sz.width - toast.getWidth() - 24, sz.height - toast.getHeight() - 24);
            }
        };
        frame.addComponentListener(resizeListener);

        Timer slideIn = new Timer(12, null);
        final int[] cy = {startY};
        slideIn.addActionListener(e -> {
            cy[0] -= 3;
            if (cy[0] <= targetY) { cy[0] = targetY; ((Timer)e.getSource()).stop(); }
            toast.setLocation(tx, cy[0]);
            lp.repaint();
        });
        slideIn.start();

        // Auto-dismiss after 2.8s
        Timer dismiss = new Timer(2800, e -> {
            frame.removeComponentListener(resizeListener);
            Timer slideOut = new Timer(12, null);
            slideOut.addActionListener(e2 -> {
                cy[0] += 3;
                if (cy[0] >= targetY + 30) {
                    ((Timer)e2.getSource()).stop();
                    lp.remove(toast);
                    lp.repaint();
                }
                toast.setLocation(tx, cy[0]);
                lp.repaint();
            });
            slideOut.start();
        });
        dismiss.setRepeats(false);
        dismiss.start();
    }

    // ── Shared UI helpers (signatures preserved) ──────────────────────────

    static JButton primaryBtn(String text) {
        RippleButton b = new RippleButton(text, ACCENT, ACCENT_HOV, Color.WHITE);
        b.setFont(FONT_BOLD);
        b.setBorder(new EmptyBorder(12, 24, 12, 24)); // Increased padding for a more modern feel
        b.putClientProperty("RippleButton.pill", true);

        return b;
    }

    static JButton secondaryBtn(String text) {
        RippleButton b = new RippleButton(text, CARD_BG, SOFT_BG, TEXT_PRIMARY);
        b.setFont(FONT_BOLD);
        b.setBorder(new CompoundBorder(
            new LineBorder(BORDER_CLR, 1, true),
            new EmptyBorder(11, 23, 11, 23))); // Consistent padding
        b.putClientProperty("RippleButton.pill", true);

        return b;
    }

    static JButton dangerBtn(String text) {
        RippleButton b = new RippleButton(text, DANGER, DANGER_BG.darker(), Color.WHITE); // Use DANGER and a darker DANGER_BG
        b.setFont(FONT_BOLD);
        b.setBorder(new EmptyBorder(12, 24, 12, 24)); // Increased padding for a more modern feel
        b.putClientProperty("RippleButton.pill", true);

        return b;
    }

    static JButton successBtn(String text) {
        RippleButton b = new RippleButton(text, SUCCESS, SUCCESS_BG.darker(), Color.WHITE);
        b.setFont(FONT_BOLD);
        b.setBorder(new EmptyBorder(12, 24, 12, 24));
        b.putClientProperty("RippleButton.pill", true);

        return b;
    }


    public static JTextField inputField(int cols) {
        JTextField f = new JTextField(cols);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.putClientProperty("FlatLaf.style", "arc:8; borderWidth:1; focusWidth:2;");
        f.setPreferredSize(new Dimension(100, 38));
        f.setMinimumSize(new Dimension(100, 38));
        return f;
    }

    public static JComboBox<String> styledCombo() {
        JComboBox<String> c = new JComboBox<>();
        c.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        c.putClientProperty("FlatLaf.style", "arc:8; borderWidth:1; focusWidth:2; padding:4,8,4,8;");
        c.setPreferredSize(new Dimension(200, 38));
        c.setMinimumSize(new Dimension(100, 38));
        return c;
    }

    public static JSpinner styledSpinner(int min, int max) {
        JSpinner s = new JSpinner(new SpinnerNumberModel(min, min, max, 1));
        s.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        s.setPreferredSize(new Dimension(100, 38));
        s.setMinimumSize(new Dimension(100, 38));
        s.putClientProperty("FlatLaf.style", "arc:8; borderWidth:1; focusWidth:2;");
        
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) s.getEditor();
        editor.getTextField().setHorizontalAlignment(JTextField.CENTER);
        return s;
    }

    static JLabel pageHeader(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_H1);
        l.setForeground(TEXT_PRIMARY);
        l.setBorder(new EmptyBorder(0, 0, 6, 0));
        return l;
    }

    static JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(new Font(FAMILY, Font.BOLD, 10));
        l.setForeground(TEXT_MUTED);
        l.setBorder(new EmptyBorder(0, 0, 4, 0));
        return l;
    }

    static JLabel formLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(FAMILY, Font.BOLD, 12));
        l.setForeground(TEXT_SEC);
        return l;
    }

    static JPanel card() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Premium diffuse shadow
                for (int i = 0; i < 8; i++) {
                    g2.setColor(new Color(0, 0, 0, 1 + (7 - i)));
                    g2.fill(new RoundRectangle2D.Float(i, i + 2, getWidth() - i * 2, getHeight() - i * 2, 16, 16));
                }
                g2.setColor(CARD_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.setColor(BORDER_CLR);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 16, 16));
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(16, 20, 16, 20));
        return p;
    }

    static JTable styledTable(String[] cols) {
        DefaultTableModel m = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable t = new JTable(m);
        t.setFont(new Font(FAMILY, Font.PLAIN, 13));
        t.setForeground(TEXT_PRIMARY);
        t.setRowHeight(38);
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(BORDER_CLR); // Use consistent border color
        t.setSelectionBackground(ACCENT_BG); // Use accent background for selection
        t.setSelectionForeground(ACCENT); // Use accent color for selected text
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setFillsViewportHeight(true);

        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tbl, Object v,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(tbl, v, sel, foc, row, col);
                setFont(new Font(FAMILY, Font.PLAIN, 13));
                setForeground(sel ? ACCENT : TEXT_PRIMARY);
                setBorder(new EmptyBorder(0, 18, 0, 18));
                if (!sel) setBackground(row % 2 == 0 ? CARD_BG : SOFT_BG); // Consistent row colors
                return this;
            }
        });

        JTableHeader h = t.getTableHeader();
        h.setFont(new Font(FAMILY, Font.BOLD, 12));
        h.setForeground(TEXT_SEC);
        h.setBackground(SOFT_BG); // Consistent soft background for header
        h.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_CLR));
        h.setPreferredSize(new Dimension(0, 38));
        h.setReorderingAllowed(false);
        ((DefaultTableCellRenderer) h.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);
        return t;
    }

    static JScrollPane tableScroll(JTable t) {
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(new LineBorder(BORDER_CLR, 1, true)); // Consistent border color
        sp.getViewport().setBackground(CARD_BG); // Consistent card background

        return sp;
    }

    private static String getIconForLabel(String label) {
        String l = label.toLowerCase().trim();
        if (l.contains("beneficiar") || l.contains("registered") || l.contains("profile")) return "👤";
        if (l.contains("outlet") || l.contains("shop") || l.contains("depot")) return "🏪";
        if (l.contains("pending") || l.contains("request")) return "📋";
        if (l.contains("rule")) return "⚙";
        if (l.contains("low stock") || l.contains("low")) return "⚠️";
        if (l.contains("commodit") || l.contains("sku") || l.contains("stock")) return "📦";
        if (l.contains("supplier")) return "🚚";
        if (l.contains("fulfilled") || l.contains("ok stock") || l.contains("health") || l.contains("state")) return "✓";
        if (l.contains("suspended") || l.contains("inactive")) return "⊘";
        if (l.contains("volume") || l.contains("received")) return "📊";
        return "📊";
    }

    private static class SubtitleInfo {
        String text;
        Color color;
        SubtitleInfo(String text, Color color) {
            this.text = text;
            this.color = color;
        }
    }

    private static SubtitleInfo getCardSubtitle(String label) {
        String l = label.toLowerCase().trim();
        if (l.equals("total beneficiaries")) return new SubtitleInfo("↑ +1.2% growth this month", new Color(0x22C55E));
        if (l.equals("active outlets")) return new SubtitleInfo("● 98.2% operational rate", new Color(0x22C55E));
        if (l.equals("pending requests")) return new SubtitleInfo("⚠ Needs review & approval", new Color(0xF59E0B));
        if (l.equals("active rules")) return new SubtitleInfo("✓ 6 system guards active", new Color(0x5B8FD9));
        if (l.equals("low stock alerts")) return new SubtitleInfo("⚠ 2 outlets below safety margin", new Color(0xEF4444));
        if (l.equals("commodities tracked")) return new SubtitleInfo("● Across 4 main categories", new Color(0x6B7280));
        if (l.equals("fair price outlets")) return new SubtitleInfo("● 98.2% operational rate", new Color(0x22C55E));
        if (l.equals("active suppliers")) return new SubtitleInfo("✓ Active delivery pipelines", new Color(0x22C55E));
        if (l.equals("total registered")) return new SubtitleInfo("✓ All records biometric verified", new Color(0x22C55E));
        if (l.equals("active status profile")) return new SubtitleInfo("● Active in current scheme", new Color(0x22C55E));
        if (l.equals("suspended ledgers")) return new SubtitleInfo("● 0 pending ledger disputes", new Color(0x6B7280));
        if (l.equals("total active outlets")) return new SubtitleInfo("● 98.2% operational rate", new Color(0x22C55E));
        if (l.equals("aggregate metric volume")) return new SubtitleInfo("● Optimal distribution flow", new Color(0x22C55E));
        if (l.equals("operations health status")) return new SubtitleInfo("✓ All depots fully functional", new Color(0x22C55E));
        if (l.equals("active rule trees")) return new SubtitleInfo("✓ System constraints enforced", new Color(0x22C55E));
        if (l.equals("quota compliance matrix")) return new SubtitleInfo("● 100% strict compliance", new Color(0x22C55E));
        if (l.equals("core system state")) return new SubtitleInfo("✓ Database synced & secure", new Color(0x22C55E));
        if (l.equals("total requests")) return new SubtitleInfo("● Total placed", new Color(0x6B7280));
        if (l.equals("fulfilled")) return new SubtitleInfo("✓ Completed", new Color(0x22C55E));
        if (l.equals("pending")) return new SubtitleInfo("● Awaiting approval", new Color(0xF59E0B));
        if (l.equals("items received")) return new SubtitleInfo("📥 Total units", new Color(0x5B8FD9));
        if (l.equals("low stock items")) return new SubtitleInfo("⚠ Requires immediate refill", new Color(0xEF4444));
        if (l.equals("managed skus")) return new SubtitleInfo("● All catalog items updated", new Color(0x6B7280));
        if (l.equals("today's issues")) return new SubtitleInfo("✓ Transactions sync successful", new Color(0x22C55E));
        if (l.equals("active beneficiaries")) return new SubtitleInfo("● Registered scheme members", new Color(0x6B7280));
        if (l.equals("total stock entries")) return new SubtitleInfo("● Total inventory items", new Color(0x6B7280));
        if (l.equals("ration card no.")) return new SubtitleInfo("● Card ID", new Color(0x6B7280));
        if (l.equals("category")) return new SubtitleInfo("● Scheme type", new Color(0x6B7280));
        if (l.equals("assigned shop")) return new SubtitleInfo("● Registered outlet", new Color(0x6B7280));
        if (l.equals("card status")) return new SubtitleInfo("● Account status", new Color(0x6B7280));
        if (l.equals("registered identity")) return new SubtitleInfo("● Verification name", new Color(0x6B7280));
        if (l.contains("low stock  (< 50)") || l.contains("low stock")) return new SubtitleInfo("⚠ Refill required immediately", new Color(0xEF4444));
        if (l.contains("ok stock  (≥ 50)") || l.contains("ok stock")) return new SubtitleInfo("✓ Stock levels healthy", new Color(0x22C55E));
        return new SubtitleInfo("● System monitored", new Color(0x6B7280));
    }

    /** KPI stat card with animated count-up */
    static JPanel statCard(String label, JLabel valLbl, Color valColor) {
        return statCard(label, valLbl, valColor, true);
    }

    static JPanel statCard(String label, JLabel valLbl, Color valColor, boolean showBadge) {
        JPanel p = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Premium diffuse shadow
                for (int i = 0; i < 6; i++) {
                    g2.setColor(new Color(0, 0, 0, 1 + (5 - i)));
                    g2.fill(new RoundRectangle2D.Float(i, i + 2, getWidth() - i * 2, getHeight() - i * 2, 16, 16));
                }
                g2.setColor(CARD_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                // Accent bar top
                g2.setColor(valColor);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), 4, 4, 4));
                g2.setColor(BORDER_CLR);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 16, 16));
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(showBadge ? new EmptyBorder(14, 18, 14, 18) : new EmptyBorder(12, 10, 12, 10));

        // Create Left Column Panel
        JPanel left = new JPanel(new GridBagLayout());
        left.setOpaque(false);
        GridBagConstraints lGbc = new GridBagConstraints();
        lGbc.gridx = 0; lGbc.gridy = 0;
        lGbc.weightx = 1.0; lGbc.anchor = GridBagConstraints.WEST;
        lGbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lbl = new JLabel(label.toUpperCase());
        lbl.setFont(new Font(FAMILY, Font.BOLD, showBadge ? 11 : 10));
        lbl.setForeground(TEXT_MUTED);
        left.add(lbl, lGbc);

        lGbc.gridy = 1;
        lGbc.insets = new Insets(2, 0, 0, 0);
        valLbl.setFont(new Font(FAMILY, Font.BOLD, showBadge ? 32 : 24));
        valLbl.setForeground(valColor);
        left.add(valLbl, lGbc);

        lGbc.gridy = 2;
        lGbc.insets = new Insets(showBadge ? 6 : 4, 0, 0, 0);
        SubtitleInfo subInfo = getCardSubtitle(label);
        JLabel subLbl = new JLabel(subInfo.text);
        subLbl.setFont(new Font(FAMILY, Font.BOLD, showBadge ? 10 : 9));
        subLbl.setForeground(subInfo.color);
        left.add(subLbl, lGbc);

        // Add Left Column to Card
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1.0; gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        p.add(left, gbc);

        if (showBadge) {
            // Right Column: Circular Icon Badge
            String iconStr = getIconForLabel(label);
            JPanel rightBadge = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    // Translucent circle background
                    g2.setColor(new Color(valColor.getRed(), valColor.getGreen(), valColor.getBlue(), 20));
                    g2.fillOval(0, 0, getWidth(), getHeight());
                    
                    // Circle border
                    g2.setColor(new Color(valColor.getRed(), valColor.getGreen(), valColor.getBlue(), 50));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawOval(0, 0, getWidth() - 1, getHeight() - 1);
                    
                    // Centered icon text
                    g2.setColor(valColor);
                    g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
                    FontMetrics fm = g2.getFontMetrics();
                    int x = (getWidth() - fm.stringWidth(iconStr)) / 2;
                    int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                    g2.drawString(iconStr, x, y);
                    g2.dispose();
                }
            };
            rightBadge.setOpaque(false);
            rightBadge.setPreferredSize(new Dimension(42, 42));
            rightBadge.setMinimumSize(new Dimension(42, 42));

            gbc.gridx = 1; gbc.weightx = 0.0;
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.EAST;
            gbc.insets = new Insets(0, 8, 0, 0);
            p.add(rightBadge, gbc);
        }

        // Animate count-up when value is set and dynamically auto-scale font size for long strings
        valLbl.addPropertyChangeListener("text", evt -> {
            String newVal = (String) evt.getNewValue();
            if (newVal != null) {
                try {
                    int target = Integer.parseInt(newVal);
                    animateCount(valLbl, target, valColor);
                } catch (NumberFormatException e) {
                    int len = newVal.length();
                    int size = showBadge ? 32 : 24;
                    if (len > 15) {
                        size = 14;
                    } else if (len > 10) {
                        size = 18;
                    }
                    valLbl.setFont(new Font(FAMILY, Font.BOLD, size));
                }
            }
        });

        return p;
    }

    private static void animateCount(JLabel lbl, int target, Color color) {
        int fontSize = lbl.getFont().getSize();
        Timer t = new Timer(20, null);
        final int[] cur = {0};
        t.addActionListener(e -> {
            cur[0] += Math.max(1, (target - cur[0]) / 6);
            if (cur[0] >= target) { cur[0] = target; ((Timer)e.getSource()).stop(); }
            lbl.putClientProperty("rawText", String.valueOf(cur[0]));
            lbl.setFont(new Font(FAMILY, Font.BOLD, fontSize));
            lbl.getParent();
            ((JLabel)lbl).putClientProperty("animated", true);
            lbl.setForeground(color);
            lbl.setText(String.valueOf(cur[0]));
        });
        if (!"true".equals(String.valueOf(lbl.getClientProperty("animating")))) {
            lbl.putClientProperty("animating", "true");
            t.start();
        }
    }

    static JPanel infoTile(String label, JLabel valLbl) {
        JPanel p = new JPanel(new BorderLayout(0, 6)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 12));
                g2.fill(new RoundRectangle2D.Float(2, 3, getWidth() - 3, getHeight() - 3, 12, 12));
                g2.setColor(CARD_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 3, getHeight() - 4, 12, 12));
                g2.setColor(BORDER_CLR);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 4, getHeight() - 5, 12, 12));
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(18, 20, 18, 20));
        JLabel lbl = new JLabel(label.toUpperCase());
        lbl.setFont(new Font(FAMILY, Font.BOLD, 10));
        lbl.setForeground(TEXT_MUTED);
        valLbl.setFont(new Font(FAMILY, Font.BOLD, 15));
        valLbl.setForeground(TEXT_PRIMARY);
        p.add(lbl,    BorderLayout.NORTH);
        p.add(valLbl, BorderLayout.CENTER);
        return p;
    }

    static JPanel divider() {
        JPanel d = new JPanel();
        d.setBackground(PAGE_BG);
        d.setPreferredSize(new Dimension(0, 16));
        d.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        return d;
    }

    static JLabel statusPill(String text, Color fg, Color bg) {
        JLabel l = new JLabel(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
                g2.setColor(new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 80));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, getHeight() - 1, getHeight() - 1));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setOpaque(false);
        l.setFont(new Font(FAMILY, Font.BOLD, 11));
        l.setForeground(fg);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        l.setBorder(new EmptyBorder(4, 12, 4, 12));
        return l;
    }

    // ── Animated Sidebar Button ───────────────────────────────────────────
    static class AnimatedSidebarButton extends JButton {
        private float activeProgress = 0f;
        private float hoverProgress  = 0f;
        private boolean active  = false;
        private boolean hovered = false;
        private Timer   animTimer;
        private float   slideX = -20f;
        private float   opacity = 0f;
        private final String fam;

        AnimatedSidebarButton(String text, String family) {
            super(text);
            this.fam = family;
            setFont(new Font(family, Font.PLAIN, 14));
            setForeground(SIDEBAR_TXT);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(new EmptyBorder(0, 16, 0, 8));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hovered = true;  startAnim(); }
                public void mouseExited(MouseEvent e)  { hovered = false; startAnim(); }
            });
        }

        void slideIn() {
            Timer t = new Timer(14, null);
            t.addActionListener(e -> {
                slideX += 2.5f; opacity += 0.07f;
                if (slideX >= 0f)  { slideX = 0f; }
                if (opacity >= 1f) { opacity = 1f; ((Timer)e.getSource()).stop(); }
                repaint();
            });
            t.start();
        }

        void setActive(boolean a) {
            this.active = a;
            setFont(new Font(fam, a ? Font.BOLD : Font.PLAIN, 14));
            setForeground(a ? Color.WHITE : SIDEBAR_TXT);
            startAnim();
        }

        private void startAnim() {
            if (animTimer != null) animTimer.stop();
            animTimer = new Timer(14, e -> {
                float targetA = active ? 1f : 0f;
                float targetH = hovered ? 1f : 0f;
                activeProgress += (targetA - activeProgress) * 0.2f;
                hoverProgress  += (targetH - hoverProgress)  * 0.2f;
                if (Math.abs(activeProgress - targetA) < 0.01f && Math.abs(hoverProgress - targetH) < 0.01f) {
                    activeProgress = targetA; hoverProgress = targetH;
                    ((Timer)e.getSource()).stop();
                }
                repaint();
            });
            animTimer.start();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Icon icon = getIcon();

            // Slide-in offset
            g2.translate(slideX, 0);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, opacity)));

            if (activeProgress > 0.01f) {
                Color actBg = new Color(59, 130, 246, (int)(25 * activeProgress));
                g2.setColor(actBg);
                g2.fill(new RoundRectangle2D.Float(8, 4, getWidth() - 16, getHeight() - 8, 12, 12));
                // Active indicator bar
                float barH = 18f * activeProgress;
                g2.setColor(SIDEBAR_ACT);
                g2.fillRoundRect(8, (int)(getHeight()/2f - barH/2), 3, (int)barH, 2, 2);
            } else if (hoverProgress > 0.01f) {
                Color hovBg = new Color(255, 255, 255, (int)(12 * hoverProgress));
                g2.setColor(hovBg);
                g2.fill(new RoundRectangle2D.Float(8, 4, getWidth() - 16, getHeight() - 8, 12, 12));
            }
            super.paintComponent(g2);
            g2.dispose();
        }

        private Color blend(Color a, Color b, float t) {
            return new Color(
                (int)(a.getRed()   + (b.getRed()   - a.getRed())   * t),
                (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int)(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t));
        }
    }

    // ── Ripple Button ─────────────────────────────────────────────────────
    static class RippleButton extends JButton {
        private final Color normalBg, hoverBg, fg;
        private float rippleRadius = 0;
        private float rippleAlpha  = 0;
        private Point rippleCenter = new Point(0, 0);
        private Timer rippleTimer;
        private boolean hovered = false;
        private boolean pressed = false;
        private float hoverProg = 0f;
        private Timer hoverTimer;

        RippleButton(String text, Color normalBg, Color hoverBg, Color fg) {
            super(text);
            this.normalBg = normalBg;
            this.hoverBg  = hoverBg;
            this.fg       = fg;
            setForeground(fg);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hovered = true;  animHover(); }
                public void mouseExited(MouseEvent e)  { hovered = false; pressed = false; animHover(); }
                public void mousePressed(MouseEvent e) {
                    pressed = true;
                    rippleCenter = e.getPoint();
                    rippleRadius = 0; rippleAlpha = 0.35f;
                    if (rippleTimer != null) rippleTimer.stop();
                    rippleTimer = new Timer(14, ev -> {
                        rippleRadius += Math.max(getWidth(), getHeight()) / 8f;
                        rippleAlpha  -= 0.03f;
                        if (rippleAlpha <= 0) { rippleAlpha = 0; ((Timer)ev.getSource()).stop(); }
                        repaint();
                    });
                    rippleTimer.start();
                }
                public void mouseReleased(MouseEvent e) { pressed = false; repaint(); }
            });
        }

        private void animHover() {
            if (hoverTimer != null) hoverTimer.stop();
            hoverTimer = new Timer(14, e -> {
                float target = hovered ? 1f : 0f;
                hoverProg += (target - hoverProg) * 0.25f;
                if (Math.abs(hoverProg - target) < 0.01f) { hoverProg = target; ((Timer)e.getSource()).stop(); }
                repaint();
            });
            hoverTimer.start();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (pressed) {
                g2.scale(0.98, 0.98);
                g2.translate(getWidth() * 0.01, getHeight() * 0.01);
            }

            Color bg = new Color(
                (int)(normalBg.getRed()   + (hoverBg.getRed()   - normalBg.getRed())   * hoverProg),
                (int)(normalBg.getGreen() + (hoverBg.getGreen() - normalBg.getGreen()) * hoverProg),
                (int)(normalBg.getBlue()  + (hoverBg.getBlue()  - normalBg.getBlue())  * hoverProg)
            );
            g2.setColor(bg);
            int arc = 12;
            Object isPill = getClientProperty("RippleButton.pill");
            if (Boolean.TRUE.equals(isPill)) arc = getHeight();
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

            // Ripple
            if (rippleAlpha > 0) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, rippleAlpha));
                g2.setColor(new Color(255, 255, 255, 180));
                g2.fillOval(
                    (int)(rippleCenter.x - rippleRadius),
                    (int)(rippleCenter.y - rippleRadius),
                    (int)(rippleRadius * 2),
                    (int)(rippleRadius * 2));
                g2.setComposite(AlphaComposite.SrcOver);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    static JPanel bannerStatCard(String label, JLabel valLbl, String symbol, Color color, String linkText, Runnable onLinkClick) {
        final boolean isClickable = onLinkClick != null;
        JPanel p = new JPanel(new GridBagLayout()) {
            private boolean hovered = false;
            {
                if (isClickable) {
                    addMouseListener(new MouseAdapter() {
                        @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                        @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                        @Override public void mouseClicked(MouseEvent e) { onLinkClick.run(); }
                    });
                }
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                for (int i = 0; i < 3; i++) {
                    g2.setColor(hovered ? new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 8 + (2 - i) * 2) : new Color(0, 0, 0, 1 + (2 - i)));
                    g2.fill(new RoundRectangle2D.Float(i, i + 1, getWidth() - i * 2, getHeight() - i * 2, 8, 8));
                }
                g2.setColor(CARD_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(hovered ? ACCENT : BORDER_CLR);
                g2.setStroke(new BasicStroke(hovered ? 1.2f : 1.0f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 8, 8));
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(5, 8, 5, 8));
        if (isClickable) {
            p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;

        JPanel badge = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 20));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        badge.setOpaque(false);
        badge.setPreferredSize(new Dimension(24, 24));
        badge.setMinimumSize(new Dimension(24, 24));
        badge.setLayout(new GridBagLayout());

        JLabel lblSymbol = new JLabel(symbol, SwingConstants.CENTER);
        lblSymbol.setFont(new Font(FAMILY, Font.PLAIN, 10));
        lblSymbol.setForeground(color);
        badge.add(lblSymbol);

        c.gridx = 0; c.gridy = 0; c.gridheight = 2;
        c.weightx = 0.0; c.insets = new Insets(0, 0, 0, 6);
        p.add(badge, c);

        JPanel rightCol = new JPanel();
        rightCol.setOpaque(false);
        rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));

        JPanel countRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        countRow.setOpaque(false);

        valLbl.setFont(PDSApp.FONT_NUM.deriveFont(Font.BOLD, 13f));
        valLbl.setForeground(TEXT_PRIMARY);
        countRow.add(valLbl);

        JLabel lblText = new JLabel(label);
        lblText.setFont(new Font(FAMILY, Font.PLAIN, 9));
        lblText.setForeground(TEXT_SEC);

        rightCol.add(countRow);
        rightCol.add(lblText);

        if (linkText != null && !linkText.isEmpty()) {
            JLabel lblLink = new JLabel(linkText);
            lblLink.setFont(new Font(FAMILY, Font.BOLD, 8));
            lblLink.setForeground(ACCENT);
            rightCol.add(Box.createVerticalStrut(1));
            rightCol.add(lblLink);
        }

        c.gridx = 1; c.gridy = 0; c.gridheight = 2;
        c.weightx = 1.0; c.insets = new Insets(0, 0, 0, 0);
        p.add(rightCol, c);

        return p;
    }

    static JPanel bannerSyncCard(JLabel timeLbl, JLabel dateLbl, Runnable onRefresh) {
        JPanel p = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                for (int i = 0; i < 3; i++) {
                    g2.setColor(new Color(0, 0, 0, 1 + (2 - i)));
                    g2.fill(new RoundRectangle2D.Float(i, i + 1, getWidth() - i * 2, getHeight() - i * 2, 10, 10));
                }
                g2.setColor(CARD_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(BORDER_CLR);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10));
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(6, 10, 6, 10));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;

        JButton btnRefresh = new JButton("🔄") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 20));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnRefresh.setOpaque(false);
        btnRefresh.setContentAreaFilled(false);
        btnRefresh.setBorderPainted(false);
        btnRefresh.setFocusPainted(false);
        btnRefresh.setPreferredSize(new Dimension(28, 28));
        btnRefresh.setMinimumSize(new Dimension(28, 28));
        btnRefresh.setFont(new Font(FAMILY, Font.PLAIN, 11));
        btnRefresh.setForeground(ACCENT);
        btnRefresh.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (onRefresh != null) {
            btnRefresh.addActionListener(e -> onRefresh.run());
        }

        c.gridx = 0; c.gridy = 0; c.gridheight = 2;
        c.weightx = 0.0; c.insets = new Insets(0, 0, 0, 8);
        p.add(btnRefresh, c);

        JPanel rightCol = new JPanel();
        rightCol.setOpaque(false);
        rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Last Sync");
        title.setFont(new Font(FAMILY, Font.BOLD, 10));
        title.setForeground(TEXT_PRIMARY);

        timeLbl.setFont(new Font(FAMILY, Font.PLAIN, 9));
        timeLbl.setForeground(SUCCESS);

        dateLbl.setFont(new Font(FAMILY, Font.PLAIN, 9));
        dateLbl.setForeground(TEXT_MUTED);

        rightCol.add(title);
        rightCol.add(timeLbl);
        rightCol.add(dateLbl);

        c.gridx = 1; c.gridy = 0; c.gridheight = 2;
        c.weightx = 1.0; c.insets = new Insets(0, 0, 0, 0);
        p.add(rightCol, c);

        return p;
    }

    static void installLookAndFeel() {
        try {
            UIManager.put("Component.arc",             12);
            UIManager.put("Button.arc",                12);
            UIManager.put("TextComponent.arc",         12);
            UIManager.put("ScrollBar.thumbArc",        12);
            UIManager.put("ScrollBar.width",           10);
            UIManager.put("ScrollBar.thumb",           BORDER_CLR);
            UIManager.put("ScrollBar.thumbHover",      TEXT_MUTED);
            UIManager.put("Table.rowHeight",           38);
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.gridColor",           BORDER_CLR);
            UIManager.put("Table.background",          CARD_BG);
            UIManager.put("TableHeader.height",        38);
            UIManager.put("TableHeader.background",    CARD_BG);
            UIManager.put("TableHeader.foreground",    TEXT_PRIMARY);
            UIManager.put("TableHeader.font",          new Font(FAMILY, Font.BOLD, 12));
            UIManager.put("Component.focusColor",      ACCENT);
            UIManager.put("Component.focusWidth",      2);
            UIManager.put("Component.innerFocusWidth", 0);
            UIManager.put("OptionPane.background",     PAGE_BG);
            UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
            UIManager.put("Panel.background",          PAGE_BG);
            FlatLightLaf.setup();
        } catch (Exception ex) {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignore) {}
        }
    }

    public static void main(String[] args) throws Exception {
        installLookAndFeel();
        conn = DB.getConnection();
        SwingUtilities.invokeLater(PDSApp::new);
    }
}
