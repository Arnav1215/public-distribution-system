
import com.formdev.flatlaf.FlatClientProperties;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.sql.*;

public class AdminFrame extends JFrame {

    private static final String FAM = pickFamily();

    private static String pickFamily() {
        String[] p = {"Inter", "SF Pro Text", "Segoe UI", "Helvetica Neue", "Arial"};
        java.util.Set<String> a = new java.util.HashSet<>();
        for (String f : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            a.add(f);
        }
        for (String x : p) {
            if (a.contains(x)) {
                return x;
            }
        }
        return "SansSerif";
    }

    private final String username;
    private CardLayout cards = new CardLayout();
    private JPanel content = new JPanel(cards);
    private PDSApp.AnimatedSidebarButton[] navBtns;
    private JLabel breadcrumb = new JLabel();
    private int currentNav = -1;

    private String[] navCards = {"OVERVIEW", "BENEFICIARIES", "SHOPS", "CATEGORIES", "COMMODITIES", "SUPPLIERS", "WAREHOUSES", "STOCK", "RULES", "AUDIT", "PASSWORD"};
    private String[] navLabels = {"Overview", "Beneficiaries", "Shops", "Categories", "Commodities", "Suppliers", "Warehouses", "Stock Analysis", "Allocation Rules", "Audit Log", "Change Password"};
    private String[] navIconPaths = {
        "/resources/icons/overview.png",
        "/resources/icons/beneficiaries.png",
        "/resources/icons/shops.png",
        "/resources/icons/categories.png",
        "/resources/icons/commodities.png",
        "/resources/icons/suppliers.png",
        "/resources/icons/warehouses.png",
        "/resources/icons/overview.png",
        "/resources/icons/rules.png",
        "/resources/icons/audit.png",
        "/resources/icons/categories.png"
    };

    public AdminFrame(String username) {
        super("PDS — Admin Dashboard  |  " + username);
        this.username = username;
        System.out.println("HELLO ADMIN");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1340, 860);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);
        JPanel main = new JPanel(new BorderLayout());
        main.add(buildTopBar(), BorderLayout.NORTH);
        main.add(buildContent(), BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);
        activateNav(0);
        setVisible(true);
        SwingUtilities.invokeLater(this::animateSidebarIn);
    }

    // ── Sidebar ───────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel side = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, PDSApp.SIDEBAR_BG, 0, getHeight(), PDSApp.SIDEBAR_BG);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(240, 0));
        side.setBorder(new MatteBorder(0, 0, 0, 1, PDSApp.SIDEBAR_SEC));

        // Brand
        JPanel brand = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(PDSApp.SIDEBAR_SEC);
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.dispose();
            }
        };
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setMaximumSize(new Dimension(240, 100));
        brand.setPreferredSize(new Dimension(240, 100));
        brand.setBorder(new EmptyBorder(20, 16, 20, 16));
        brand.setAlignmentX(LEFT_ALIGNMENT);

        JLabel logoImg = new JLabel();
        try {
            ImageIcon icon = new ImageIcon(PDSApp.class.getResource("/logo.png"));
            Image scaled = icon.getImage().getScaledInstance(32, 46, Image.SCALE_SMOOTH);
            logoImg.setIcon(new ImageIcon(scaled));
        } catch (Exception e) {
        }

        JPanel brandText = new JPanel();
        brandText.setOpaque(false);
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));

        JLabel logo = new JLabel("PDS Portal");
        logo.setFont(new Font(FAM, Font.BOLD, 17));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(LEFT_ALIGNMENT);
        JLabel sub = new JLabel("District Administration");
        sub.setFont(new Font(FAM, Font.PLAIN, 11));
        sub.setForeground(PDSApp.SIDEBAR_LBL);
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
        side.add(Box.createVerticalStrut(14));

        // Group: MANAGEMENT
        side.add(sideLabel("MANAGEMENT"));
        navBtns = new PDSApp.AnimatedSidebarButton[navCards.length];
        String[] groups = {"MANAGEMENT", "MANAGEMENT", "MANAGEMENT", "MASTER DATA", "MASTER DATA", "MASTER DATA", "MASTER DATA", "ANALYTICS", "GOVERNANCE", "GOVERNANCE", "GOVERNANCE"};
        String lastGroup = "";
        for (int i = 0; i < navCards.length; i++) {
            if (!groups[i].equals(lastGroup)) {
                if (!lastGroup.isEmpty()) {
                    side.add(Box.createVerticalStrut(12));
                    side.add(sideLabel(groups[i]));
                }
                lastGroup = groups[i];
            }
            navBtns[i] = new PDSApp.AnimatedSidebarButton(navLabels[i], FAM);
            try {
                ImageIcon icon = new ImageIcon(AdminFrame.class.getResource(navIconPaths[i]));
                Image img = icon.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH);
                navBtns[i].setIcon(new ImageIcon(img));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            navBtns[i].setIconTextGap(12);
            navBtns[i].setMaximumSize(new Dimension(240, 42));
            navBtns[i].setPreferredSize(new Dimension(240, 42));
            navBtns[i].setAlignmentX(LEFT_ALIGNMENT);
            final int idx = i;
            navBtns[i].addActionListener(e -> activateNav(idx));
            side.add(navBtns[i]);
            side.add(Box.createVerticalStrut(2));
        }

        side.add(Box.createVerticalGlue());

        // Sign out
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, PDSApp.SIDEBAR_SEC),
                new EmptyBorder(14, 16, 14, 16)));
        footer.setMaximumSize(new Dimension(240, 52));
        footer.setAlignmentX(LEFT_ALIGNMENT);
        PDSApp.RippleButton btnLogout = new PDSApp.RippleButton("  ⤢  Sign Out", PDSApp.SIDEBAR_BG, PDSApp.SIDEBAR_HOV, PDSApp.SIDEBAR_LBL);
        btnLogout.setFont(new Font(FAM, Font.BOLD, 12));
        btnLogout.setHorizontalAlignment(SwingConstants.LEFT);
        btnLogout.setMaximumSize(new Dimension(240, 52));
        btnLogout.setPreferredSize(new Dimension(240, 52));
        btnLogout.setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, PDSApp.SIDEBAR_SEC), new EmptyBorder(0, 0, 0, 0)));
        btnLogout.addActionListener(e -> {
            dispose();
            new LoginFrame();
        });
        footer.add(btnLogout);
        side.add(footer);
        return side;
    }

    private JLabel sideLabel(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font(FAM, Font.BOLD, 11));
        l.setForeground(PDSApp.SIDEBAR_LBL);
        l.setBorder(new EmptyBorder(0, 16, 4, 0));
        l.setMaximumSize(new Dimension(240, 22));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private void activateNav(int idx) {
        if (currentNav == idx) {
            return;
        }
        currentNav = idx;
        for (int i = 0; i < navBtns.length; i++) {
            navBtns[i].setActive(i == idx);
        }
        cards.show(content, navCards[idx]);
        breadcrumb.setText("Admin  ›  " + navLabels[idx]);
    }

    private void animateSidebarIn() {
        for (int i = 0; i < navBtns.length; i++) {
            final int fi = i;
            Timer t = new Timer(50 * i, e -> {
                navBtns[fi].slideIn();
                ((Timer) e.getSource()).stop();
            });
            t.start();
        }
    }

    // ── Top bar ───────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(PDSApp.CARD_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(PDSApp.BORDER_CLR);
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(12, 24, 12, 24));
        bar.setPreferredSize(new Dimension(0, 56));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        JLabel ico = new JLabel("⌂");
        ico.setFont(new Font(FAM, Font.PLAIN, 14));
        ico.setForeground(PDSApp.TEXT_MUTED);
        breadcrumb.setFont(new Font(FAM, Font.PLAIN, 13));
        breadcrumb.setForeground(PDSApp.TEXT_SEC);
        left.add(ico);
        left.add(breadcrumb);
        bar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        JLabel role = new JLabel("Administrator");
        role.setFont(new Font(FAM, Font.BOLD, 11));
        role.setForeground(PDSApp.ACCENT);
        role.setBackground(PDSApp.ACCENT_BG);
        role.setOpaque(true);
        role.setBorder(new EmptyBorder(5, 12, 5, 12));

        JLabel av = new JLabel(username.substring(0, Math.min(2, username.length())).toUpperCase()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PDSApp.ACCENT);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        av.setFont(new Font(FAM, Font.BOLD, 11));
        av.setForeground(Color.WHITE);
        av.setPreferredSize(new Dimension(32, 32));
        av.setHorizontalAlignment(SwingConstants.CENTER);
        av.setOpaque(false);
        av.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        av.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showChangePasswordDialog();
            }
        });
        right.add(role);
        right.add(av);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ── Content ───────────────────────────────────────────────────────
    private JPanel buildContent() {
        content.setBackground(PDSApp.PAGE_BG);
        content.add(buildOverview(), "OVERVIEW");
        content.add(buildBeneficiaries(), "BENEFICIARIES");
        content.add(buildShops(), "SHOPS");
        content.add(buildCategories(), "CATEGORIES");
        content.add(buildCommodities(), "COMMODITIES");
        content.add(buildSuppliers(), "SUPPLIERS");
        content.add(buildWarehouses(), "WAREHOUSES");
        content.add(new StockPanel(), "STOCK");
        content.add(buildRules(), "RULES");
        content.add(new AuditPanel(), "AUDIT");
        content.add(buildPassword(), "PASSWORD");
        return content;
    }

    private JPanel buildPassword() {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setBackground(PAGE);
        p.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(PAGE);
        top.add(PDSApp.pageHeader("Change Account Password"), BorderLayout.WEST);
        top.setBorder(new EmptyBorder(0, 0, 14, 0));

        JPanel container = new JPanel(new GridBagLayout());
        container.setBackground(PAGE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        JPanel passCard = PDSApp.card();
        passCard.setLayout(new GridBagLayout());
        passCard.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints pGbc = new GridBagConstraints();
        pGbc.fill = GridBagConstraints.HORIZONTAL;
        pGbc.weightx = 1.0; pGbc.gridx = 0; pGbc.gridy = 0;
        
        passCard.add(PDSApp.sectionLabel("Security Access Credentials Setup"), pGbc);
        pGbc.insets = new Insets(14, 0, 4, 0);
        
        JPasswordField pfOld = new JPasswordField(); pfOld.putClientProperty("FlatLaf.style", "arc:8; borderWidth:1; focusWidth:2;"); pfOld.setPreferredSize(new Dimension(100, 38)); pfOld.setMinimumSize(new Dimension(100, 38));
        JPasswordField pfNew = new JPasswordField(); pfNew.putClientProperty("FlatLaf.style", "arc:8; borderWidth:1; focusWidth:2;"); pfNew.setPreferredSize(new Dimension(100, 38)); pfNew.setMinimumSize(new Dimension(100, 38));
        JPasswordField pfConf = new JPasswordField(); pfConf.putClientProperty("FlatLaf.style", "arc:8; borderWidth:1; focusWidth:2;"); pfConf.setPreferredSize(new Dimension(100, 38)); pfConf.setMinimumSize(new Dimension(100, 38));
        JButton btnChange = PDSApp.primaryBtn("Update Secure Credentials");
        JLabel lblPassMsg = new JLabel(" "); lblPassMsg.setFont(PDSApp.FONT_LABEL);

        pGbc.gridy++; pGbc.insets = new Insets(10, 0, 4, 0); passCard.add(PDSApp.formLabel("Current Account Password"), pGbc);
        pGbc.gridy++; pGbc.insets = new Insets(0, 0, 12, 0); passCard.add(pfOld, pGbc);
        pGbc.gridy++; pGbc.insets = new Insets(0, 0, 4, 0); passCard.add(PDSApp.formLabel("New Password Boundary"), pGbc);
        pGbc.gridy++; pGbc.insets = new Insets(0, 0, 12, 0); passCard.add(pfNew, pGbc);
        pGbc.gridy++; pGbc.insets = new Insets(0, 0, 4, 0); passCard.add(PDSApp.formLabel("Confirm New Password Credentials"), pGbc);
        pGbc.gridy++; pGbc.insets = new Insets(0, 0, 16, 0); passCard.add(pfConf, pGbc);
        pGbc.gridy++; pGbc.insets = new Insets(8, 0, 0, 0); passCard.add(btnChange, pGbc);
        pGbc.gridy++; pGbc.insets = new Insets(8, 0, 0, 0); passCard.add(lblPassMsg, pGbc);

        btnChange.addActionListener(e -> {
            String oldP = new String(pfOld.getPassword());
            String newP = new String(pfNew.getPassword());
            String confP = new String(pfConf.getPassword());
            if (oldP.isEmpty() || newP.isEmpty()) {
                lblPassMsg.setText("✗ Fill all password cells.");
                lblPassMsg.setForeground(PDSApp.DANGER);
                return;
            }
            if (!newP.equals(confP)) {
                lblPassMsg.setText("✗ Password confirmation mismatch.");
                lblPassMsg.setForeground(PDSApp.DANGER);
                return;
            }
            try {
                boolean success = ShopController.updateCredentials(username, oldP, newP);
                if (!success) {
                    lblPassMsg.setText("✗ Verification failure: Mismatched credentials.");
                    lblPassMsg.setForeground(PDSApp.DANGER);
                    return;
                }
                lblPassMsg.setText("✔ Credentials updated successfully.");
                lblPassMsg.setForeground(PDSApp.SUCCESS);
                pfOld.setText(""); pfNew.setText(""); pfConf.setText("");
            } catch (SQLException ex) {
                lblPassMsg.setText("✗ " + ex.getMessage());
                lblPassMsg.setForeground(PDSApp.DANGER);
            }
        });

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 1.0; gbc.weighty = 0.0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.insets = new Insets(0, 0, 0, 0);
        container.add(passCard, gbc);
        
        gbc.gridy = 1; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        container.add(Box.createVerticalGlue(), gbc);

        p.add(top, BorderLayout.NORTH);
        p.add(container, BorderLayout.CENTER);
        return p;
    }

    private static final Color PAGE = PDSApp.PAGE_BG;

    private String toHexString(Color c) {
        return String.format("#%06X", (0xFFFFFF & c.getRGB()));
    }
    // ── Fixed Modern Glassmorphic Action Shortcut Button ──
    // ── Modern Full-Expanding Glassmorphic Action Shortcut Button ──
    private JButton actionBtn(String text, String symbol) {
        JButton b = new JButton(symbol + "   " + text);
        b.setFont(PDSApp.FONT_BOLD.deriveFont(13f)); // Slightly larger font for prominence
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(255, 255, 255, 14)); // Translucent Glass Acrylic Fill
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        // Enforce smooth corner radii, premium hover, and focus padding
        b.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, 
            "arc:12; focusWidth:0; hoverBackground:rgba(255,255,255,30); pressedBackground:rgba(255,255,255,50)");
        
        b.setBorder(new javax.swing.border.LineBorder(new Color(255, 255, 255, 35), 1, true));
        return b;
    }

    // ── Ultra-Modern Translucent Fluid-Wave Hero Banner with Embedded Metrics ──
    private JPanel buildHeroBanner(JLabel lBen, JLabel lShop, JLabel lPend, JLabel lRules, Runnable onRefresh) {
        JPanel banner = new JPanel(new GridBagLayout()) {
            private float dynamicFlowPhase = 0f;
            private final Timer flowTimer = new Timer(30, e -> {
                dynamicFlowPhase += 0.03f;
                repaint();
            });
            private Image img = null;
            {
                flowTimer.start();
                try {
                    java.io.File f = new java.io.File("resources/adminframeherobanner.png");
                    if (f.exists()) {
                        img = javax.imageio.ImageIO.read(f);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                for (int i = 0; i < 6; i++) {
                    g2.setColor(new Color(0, 0, 0, 1 + (5 - i)));
                    g2.fill(new RoundRectangle2D.Float(i, i + 2, getWidth() - i * 2, getHeight() - i * 2, 20, 20));
                }
                RoundRectangle2D.Float boundaryClip = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setClip(boundaryClip);
                
                GradientPaint baseMeshGrad = new GradientPaint(0, 0, new Color(0xEEF6FC), getWidth(), getHeight(), new Color(0xE0ECFA));
                g2.setPaint(baseMeshGrad);
                g2.fill(boundaryClip);

                // Subtle layered background shapes
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fillOval(-80, -80, 250, 250);
                g2.fillOval(getWidth() / 2 - 150, -100, 300, 200);
                g2.fillOval(getWidth() - 200, 50, 350, 250);

                // Draw illustration with dynamic floating/bobbing effect
                double bobOffset = Math.sin(dynamicFlowPhase * 1.5) * 8;
                if (img != null) {
                    int w = getWidth(), h = getHeight();
                    int ih = img.getHeight(null);
                    int iw = img.getWidth(null);
                    
                    double scale = (double) (h * 0.85) / ih;
                    int nw = (int) (iw * scale);
                    int nh = (int) (ih * scale);
                    
                    g2.drawImage(img, w - nw - 16, (h - nh) / 2 + 10 + (int) bobOffset, nw, nh, null);
                } else {
                    // Fallback visual illustration
                    int w = getWidth(), h = getHeight();
                    int cx = w - 180;
                    int cy = h / 2 + 10 + (int) bobOffset;
                    g2.setColor(new Color(0x5B8FD9));
                    g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{4, 4}, 0));
                    int[][] nodes = {
                        {cx - 60, cy - 25}, {cx - 60, cy + 20}, {cx + 60, cy - 25}, {cx + 60, cy + 20},
                        {cx - 20, cy - 35}, {cx + 20, cy - 35}
                    };
                    for (int[] node : nodes) {
                        g2.drawLine(cx, cy - 10, node[0], node[1]);
                    }
                    int mw = 52, mh = 36;
                    int mx = cx - mw / 2, my = cy - 10 - mh / 2;
                    g2.setColor(new Color(0x475569));
                    g2.fillRect(cx - 5, my + mh, 10, 10);
                    g2.fillRect(cx - 12, my + mh + 8, 24, 3);
                    g2.setColor(new Color(0x1E293B));
                    g2.fillRoundRect(mx, my, mw, mh, 8, 8);
                    g2.setColor(new Color(0x0F172A));
                    g2.fillRect(mx + 3, my + 3, mw - 6, mh - 10);
                    g2.setColor(new Color(0x10B981));
                    g2.drawLine(mx + 6, my + mh - 10, mx + 16, my + mh - 14);
                    g2.drawLine(mx + 16, my + mh - 14, mx + 26, my + mh - 8);
                    g2.drawLine(mx + 26, my + mh - 8, mx + 38, my + mh - 18);
                    for (int[] node : nodes) {
                        int nx = node[0], ny = node[1];
                        g2.setColor(Color.WHITE);
                        g2.fillOval(nx - 7, ny - 7, 14, 14);
                        g2.setColor(new Color(0x384959));
                        g2.setStroke(new BasicStroke(1.2f));
                        g2.drawOval(nx - 7, ny - 7, 14, 14);
                        g2.setColor(new Color(0x5B8FD9));
                        g2.fillOval(nx - 3, ny - 3, 6, 6);
                    }
                }

                // Fluid waves (drawn ON TOP of the illustration so waves overlap the bottom portion)
                Path2D fluidWaveA = new Path2D.Float();
                fluidWaveA.moveTo(0, getHeight());
                fluidWaveA.lineTo(0, getHeight() - 55);
                for (int x = 0; x <= getWidth(); x += 25) {
                    double waveSinY = Math.sin((x * 0.005) + dynamicFlowPhase) * 12;
                    double waveCosY = Math.cos((x * 0.002) + (dynamicFlowPhase * 0.7)) * 6;
                    fluidWaveA.lineTo(x, getHeight() - 60 + (int)(waveSinY + waveCosY));
                }
                fluidWaveA.lineTo(getWidth(), getHeight()); fluidWaveA.closePath();
                g2.setPaint(new Color(255, 255, 255, 35));
                g2.fill(fluidWaveA);

                Path2D fluidWaveB = new Path2D.Float();
                fluidWaveB.moveTo(0, getHeight());
                fluidWaveB.lineTo(0, getHeight() - 35);
                for (int x = 0; x <= getWidth(); x += 20) {
                    double waveSinY = Math.sin((x * 0.004) - (dynamicFlowPhase * 1.2f)) * 15;
                    double waveCosY = Math.cos((x * 0.006) + dynamicFlowPhase) * 8;
                    fluidWaveB.lineTo(x, getHeight() - 45 + (int)(waveSinY + waveCosY));
                }
                fluidWaveB.lineTo(getWidth(), getHeight()); fluidWaveB.closePath();
                g2.setPaint(new Color(255, 255, 255, 60));
                g2.fill(fluidWaveB);

                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setPaint(new Color(255, 255, 255, 90));
                g2.setClip(fluidWaveB); g2.draw(fluidWaveB);
                
                g2.dispose();
            }
        };
        banner.setOpaque(false);
        banner.setMinimumSize(new Dimension(0, 350));
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 350));
        banner.setPreferredSize(new Dimension(0, 350));
        banner.setBorder(new EmptyBorder(16, 20, 16, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        // ── LEFT COLUMN (68% width for text and stacked KPI cards) ──
        JPanel leftCol = new JPanel(new GridBagLayout());
        leftCol.setOpaque(false);
        GridBagConstraints leftGbc = new GridBagConstraints();
        leftGbc.gridx = 0; leftGbc.gridy = 0;
        leftGbc.weightx = 1.0; leftGbc.anchor = GridBagConstraints.WEST;
        leftGbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel sysLbl = new JLabel("PDS DISTRIBUTION MANAGEMENT SYSTEM");
        sysLbl.setFont(new Font(FAM, Font.BOLD, 13));
        sysLbl.setForeground(PDSApp.ACCENT);
        leftCol.add(sysLbl, leftGbc);

        leftGbc.gridy++;
        leftGbc.insets = new Insets(4, 0, 0, 0); // Tighter spacing
        JLabel title = new JLabel("Administrator Overview");
        title.setFont(PDSApp.FONT_H1.deriveFont(Font.BOLD, 30f));
        title.setForeground(PDSApp.TEXT_PRIMARY);
        leftCol.add(title, leftGbc);

        leftGbc.gridy++;
        leftGbc.insets = new Insets(2, 0, 0, 0); // Tighter spacing
        JLabel sub = new JLabel("Monitor, manage and optimize the PDS ecosystem");
        sub.setFont(PDSApp.FONT_LABEL.deriveFont(Font.PLAIN, 16f));
        sub.setForeground(PDSApp.TEXT_SEC);
        leftCol.add(sub, leftGbc);

        leftGbc.gridy++;
        leftGbc.insets = new Insets(8, 0, 0, 0); // Reduced gap to move KPI cards closer

        JPanel syncRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        syncRow.setOpaque(false);
        JLabel lblLastSyncPrefix = new JLabel("Last Sync: ");
        lblLastSyncPrefix.setFont(new Font(FAM, Font.BOLD, 13));
        lblLastSyncPrefix.setForeground(PDSApp.TEXT_SEC);
        syncRow.add(lblLastSyncPrefix);

        JLabel timeLbl = new JLabel("Just now");
        timeLbl.setFont(new Font(FAM, Font.BOLD, 13));
        timeLbl.setForeground(PDSApp.SUCCESS);
        syncRow.add(timeLbl);

        JLabel dotLbl = new JLabel(" • ");
        dotLbl.setFont(new Font(FAM, Font.PLAIN, 13));
        dotLbl.setForeground(PDSApp.TEXT_MUTED);
        syncRow.add(dotLbl);

        JLabel dateLbl = new JLabel();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy h:mm a");
        dateLbl.setText(sdf.format(new java.util.Date()));
        dateLbl.setFont(new Font(FAM, Font.PLAIN, 13));
        dateLbl.setForeground(PDSApp.TEXT_SEC);
        syncRow.add(dateLbl);

        // Clickable Inline Refresh Action Link
        syncRow.add(Box.createHorizontalStrut(6));
        JLabel btnMiniRefresh = new JLabel("⟳ Refresh");
        btnMiniRefresh.setFont(new Font(FAM, Font.BOLD, 13));
        btnMiniRefresh.setForeground(PDSApp.ACCENT);
        btnMiniRefresh.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnMiniRefresh.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                dateLbl.setText(sdf.format(new java.util.Date()));
                if (onRefresh != null) onRefresh.run();
            }
            @Override public void mouseEntered(MouseEvent e) {
                btnMiniRefresh.setForeground(PDSApp.ACCENT.darker());
            }
            @Override public void mouseExited(MouseEvent e) {
                btnMiniRefresh.setForeground(PDSApp.ACCENT);
            }
        });
        syncRow.add(btnMiniRefresh);
        leftCol.add(syncRow, leftGbc);

        // Add the KPI cards directly below the sync row inside leftCol
        leftGbc.gridy++;
        leftGbc.insets = new Insets(12, 0, 0, 0); // Reduced gap to move KPI cards closer
        leftGbc.weighty = 1.0;
        leftGbc.fill = GridBagConstraints.HORIZONTAL;
        
        JPanel cardsRow = new JPanel(new GridLayout(1, 4, 16, 0));
        cardsRow.setOpaque(false);
        
        JPanel card1 = PDSApp.statCard("Total Beneficiaries", lBen, PDSApp.ACCENT);
        JPanel card2 = PDSApp.statCard("Active Outlets", lShop, PDSApp.SUCCESS);
        JPanel card3 = PDSApp.statCard("Pending Requests", lPend, PDSApp.WARNING);
        JPanel card4 = PDSApp.statCard("Active Rules", lRules, PDSApp.ACCENT);

        card1.setPreferredSize(new Dimension(0, 120));
        card2.setPreferredSize(new Dimension(0, 120));
        card3.setPreferredSize(new Dimension(0, 120));
        card4.setPreferredSize(new Dimension(0, 120));

        card1.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card1.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { activateNav(1); }
        });
        card2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card2.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { activateNav(2); }
        });
        card3.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card3.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { activateNav(9); }
        });
        card4.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card4.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { activateNav(8); }
        });

        cardsRow.add(card1);
        cardsRow.add(card2);
        cardsRow.add(card3);
        cardsRow.add(card4);
        leftCol.add(cardsRow, leftGbc);

        gbc.gridx = 0; gbc.weightx = 0.68;
        gbc.insets = new Insets(0, 0, 0, 0);
        banner.add(leftCol, gbc);

        // ── RIGHT COLUMN (32% width filler to reserve space for the illustration drawn in background) ──
        JPanel rightFiller = new JPanel();
        rightFiller.setOpaque(false);
        gbc.gridx = 1; gbc.weightx = 0.32;
        banner.add(rightFiller, gbc);

        return banner;
    }

    // ── Overview ──────────────────────────────────────────────────────
    private JPanel buildOverview() {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(PAGE);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(PAGE);
        p.setBorder(new EmptyBorder(16, 20, 16, 20));
        
        // Dynamic Variable Handles
        JLabel lBen = new JLabel("0"), lReq = new JLabel("0"), lPend = new JLabel("0"), lDist = new JLabel("0");
        JLabel lLow = new JLabel("0"), lCom = new JLabel("0"), lShop = new JLabel("0"), lSup = new JLabel("0"), lRules = new JLabel("0");

        // Premium Translucent Fluid-Wave Hero Card Panel Container
        final Runnable[] loadEngineBox = new Runnable[1];
        JPanel hero = buildHeroBanner(lBen, lShop, lPend, lRules, () -> { if (loadEngineBox[0] != null) loadEngineBox[0].run(); });
        hero.setAlignmentX(LEFT_ALIGNMENT);

        // ── Real-Time Glassmorphic Animated Vector Bar Chart Component ──
        final java.util.List<Object[]> liveChartData = new java.util.ArrayList<>();
        JPanel liveTelemetryCard = new JPanel() {
            private float barSweepScanLine = 0f;
            private float pulseGlowPhase = 0f;
            private final Timer barChartTimer = new Timer(30, e -> {
                barSweepScanLine += 0.012f;
                pulseGlowPhase += 0.04f;
                if (barSweepScanLine > 1.2f) barSweepScanLine = -0.1f;
                repaint();
            });
            { barChartTimer.start(); }

            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Frosted Glassmorphic Canvas Backdrop Panel Rendering
                g2.setColor(PDSApp.CARD_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                
                // Dynamic Linear Ambient Glow Highlight Path Border
                int alphaPulse = 40 + (int)(15 * Math.sin(pulseGlowPhase));
                g2.setPaint(new GradientPaint(0, 0, new Color(PDSApp.ACCENT.getRed(), PDSApp.ACCENT.getGreen(), PDSApp.ACCENT.getBlue(), alphaPulse), getWidth(), getHeight(), new Color(PDSApp.ACCENT.getRed(), PDSApp.ACCENT.getGreen(), PDSApp.ACCENT.getBlue(), 15)));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 16, 16));

                int padLeft = 52, padTop = 85, padBottom = 40;
                int graphW = getWidth() - (padLeft * 2);
                int graphH = getHeight() - padTop - padBottom;
                int baseFloorY = getHeight() - padBottom;

                int totalBars = liveChartData.size();
                if (totalBars == 0) {
                    g2.setFont(new Font(FAM, Font.BOLD, 12));
                    g2.setColor(PDSApp.TEXT_MUTED);
                    g2.drawString("No distribution data available in DB", getWidth() / 2 - 100, getHeight() / 2);
                    g2.dispose();
                    return;
                }

                int maxVal = 1;
                for (Object[] row : liveChartData) {
                    int val = (int) row[1];
                    if (val > maxVal) maxVal = val;
                }

                double scaleMax = maxVal * 1.15; // 15% headroom top padding above bars

                // Technical Background Grid Mesh
                g2.setFont(new Font(FAM, Font.PLAIN, 10));
                for (int i = 0; i <= 3; i++) {
                    int gy = baseFloorY - (graphH * i / 3);
                    g2.setStroke(new BasicStroke(1f));
                    g2.setColor(new Color(0x27272A)); // Dark mesh grid line
                    g2.drawLine(padLeft, gy, padLeft + graphW, gy);
                    
                    g2.setColor(PDSApp.TEXT_MUTED);
                    int gridVal = (int) (scaleMax * i / 3);
                    String labelVal;
                    if (gridVal >= 1000) {
                        labelVal = String.format("%.1fk", gridVal / 1000.0);
                        if (labelVal.endsWith(".0k")) labelVal = labelVal.substring(0, labelVal.length() - 3) + "k";
                    } else {
                        labelVal = String.valueOf(gridVal);
                    }
                    int labelW = g2.getFontMetrics().stringWidth(labelVal);
                    g2.drawString(labelVal, padLeft - labelW - 8, gy + 4);
                }

                Color coreCol = PDSApp.ACCENT;

                int barWidth = (graphW / totalBars) - 20;
                if (barWidth < 8) barWidth = 8;
                
                for (int i = 0; i < totalBars; i++) {
                    Object[] row = liveChartData.get(i);
                    String comName = (String) row[0];
                    int qty = (int) row[1];
                    
                    int barCalculatedHeight = (int)(graphH * qty / scaleMax);
                    int bx = padLeft + 10 + (graphW * i / totalBars);
                    int by = baseFloorY - barCalculatedHeight;

                    // Neon Glassmorphic Underlay
                    g2.setColor(new Color(coreCol.getRed(), coreCol.getGreen(), coreCol.getBlue(), 12));
                    g2.fill(new RoundRectangle2D.Float(bx - 2, by - 2, barWidth + 4, barCalculatedHeight + 2, 6, 6));

                    // Linear Microgradient Solid Bar
                    g2.setPaint(new GradientPaint(bx, by, coreCol, bx, baseFloorY, new Color(coreCol.getRed(), coreCol.getGreen(), coreCol.getBlue(), 40)));
                    g2.fill(new RoundRectangle2D.Float(bx, by, barWidth, barCalculatedHeight, 6, 6));

                    // Centered Data Label
                    g2.setFont(new Font(FAM, Font.BOLD, 9));
                    g2.setColor(coreCol);
                    String qtyStr = String.valueOf(qty);
                    int qtyW = g2.getFontMetrics().stringWidth(qtyStr);
                    g2.drawString(qtyStr, bx + (barWidth / 2) - (qtyW / 2), by - 6);

                    // Centered Horizontal X-Axis Legends
                    g2.setFont(new Font(FAM, Font.BOLD, 10));
                    g2.setColor(PDSApp.TEXT_PRIMARY);
                    String label = comName.length() > 8 ? comName.substring(0, 8) : comName;
                    label = label.toUpperCase();
                    int labelW = g2.getFontMetrics().stringWidth(label);
                    g2.drawString(label, bx + (barWidth / 2) - (labelW / 2), baseFloorY + 18);
                }

                // Holographic Scanning Sweep Laser Wire Frame
                int sweepLineX = padLeft + (int)(graphW * barSweepScanLine);
                if (sweepLineX >= padLeft && sweepLineX <= padLeft + graphW) {
                    GradientPaint sweepGlowGrad = new GradientPaint(sweepLineX - 30, 0, new Color(255, 255, 255, 0), sweepLineX, 0, new Color(255, 255, 255, 40));
                    g2.setPaint(sweepGlowGrad);
                    g2.fillRect(sweepLineX - 30, padTop, 30, graphH);
                    
                    g2.setColor(new Color(255, 255, 255, 120));
                    g2.setStroke(new BasicStroke(1.2f));
                    g2.drawLine(sweepLineX, padTop, sweepLineX, baseFloorY);
                }
                g2.dispose();
            }
        };
        liveTelemetryCard.setOpaque(false);
        liveTelemetryCard.setLayout(new BorderLayout());
        liveTelemetryCard.setBorder(new EmptyBorder(16, 22, 16, 22));
        liveTelemetryCard.setPreferredSize(new Dimension(0, 360));
        
        JLabel lblTelemetryText = new JLabel("<html><b style='color:#1F2937; font-size:12px;'>Commodity Distribution <span style=\"color:#6B7280; font-weight:normal;\">(This Month)</span></b><br>"
                + "<span style='color:#6B7280; font-size:10px;'>● Live commodity distribution metrics from production transaction records</span></html>");
        lblTelemetryText.setBorder(new EmptyBorder(0, 0, 18, 0)); 
        liveTelemetryCard.add(lblTelemetryText, BorderLayout.NORTH);

        // Secondary Diagnostic Right-Hand Side Panels — 2x2 grid top, activity bottom
        JPanel secondaryMetricsGrid = new JPanel(new GridLayout(2, 2, 12, 12));
        secondaryMetricsGrid.setBackground(PAGE);
        secondaryMetricsGrid.add(PDSApp.statCard("Low Stock Alerts", lLow,  PDSApp.DANGER));
        secondaryMetricsGrid.add(PDSApp.statCard("Commodities Tracked", lCom,  PDSApp.ACCENT));
        secondaryMetricsGrid.add(PDSApp.statCard("Fair Price Outlets",  lShop, PDSApp.ACCENT));
        secondaryMetricsGrid.add(PDSApp.statCard("Active Suppliers",   lSup,  PDSApp.ACCENT));
        secondaryMetricsGrid.setPreferredSize(new Dimension(0, 360));

        // System Activity Timeline panel (right-side activity feed, kept for background DB binding but not added to layout)
        JPanel timelinePanel = buildSystemActivityTimeline();
        JPanel timelineList = (JPanel) ((JScrollPane) timelinePanel.getComponent(1)).getViewport().getView();

        // Right column: contains only the 2x2 metrics grid
        JPanel rightColumn = new JPanel(new GridBagLayout());
        rightColumn.setBackground(PAGE);
        GridBagConstraints rcGbc = new GridBagConstraints();
        rcGbc.fill = GridBagConstraints.BOTH;
        rcGbc.gridx = 0;
        rcGbc.weightx = 1.0;
        rcGbc.gridy = 0; rcGbc.weighty = 1.0; rcGbc.insets = new Insets(0, 0, 0, 0);
        rightColumn.add(secondaryMetricsGrid, rcGbc);

        // ── GridBag Alignment Structural Core Pipeline Assembly ──
        GridBagConstraints workspaceGbc = new GridBagConstraints();
        workspaceGbc.fill = GridBagConstraints.HORIZONTAL;
        workspaceGbc.gridx = 0;
        workspaceGbc.weightx = 1.0;
        
        workspaceGbc.gridy = 0; workspaceGbc.insets = new Insets(0, 0, 24, 0);
        p.add(hero, workspaceGbc);

        // Lower Workspace Split Layout (58% Chart / 42% Right Column Side-by-Side)
        JPanel bottomSplitRowPanel = new JPanel(new GridBagLayout());
        bottomSplitRowPanel.setBackground(PAGE);
        GridBagConstraints bGbc = new GridBagConstraints();
        bGbc.fill = GridBagConstraints.BOTH;
        bGbc.weighty = 1.0;

        bGbc.gridx = 0; bGbc.weightx = 0.58; bGbc.insets = new Insets(0, 0, 0, 0);
        bottomSplitRowPanel.add(liveTelemetryCard, bGbc);

        bGbc.gridx = 1; bGbc.weightx = 0.42; bGbc.insets = new Insets(0, 18, 0, 0);
        bottomSplitRowPanel.add(rightColumn, bGbc);

        workspaceGbc.gridy = 1;
        workspaceGbc.weighty = 1.0;
        workspaceGbc.fill = GridBagConstraints.BOTH;
        workspaceGbc.insets = new Insets(0, 0, 0, 0);
        p.add(bottomSplitRowPanel, workspaceGbc);

        // Bind Refresh Callbacks
        loadEngineBox[0] = () -> {
            loadSummary(liveChartData, timelineList, lBen, lReq, lPend, lDist, lLow, lCom, lShop, lSup, lRules);
            liveTelemetryCard.repaint();
        };
        loadEngineBox[0].run();

        JScrollPane mainScroll = new JScrollPane(p);
        mainScroll.setBorder(null);
        mainScroll.getVerticalScrollBar().setUnitIncrement(16);
        container.add(mainScroll, BorderLayout.CENTER);

        return container;
    }


    private JPanel buildSystemActivityTimeline() {
        JPanel panel = new JPanel(new BorderLayout(0, 10)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PDSApp.CARD_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.setColor(PDSApp.BORDER_CLR);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 16, 16));
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(22, 22, 22, 22));
        panel.setPreferredSize(new Dimension(0, 360));
        
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        JLabel title = new JLabel("System Activity");
        title.setFont(PDSApp.FONT_H2.deriveFont(Font.BOLD, 18f));
        title.setForeground(PDSApp.TEXT_PRIMARY);
        JLabel viewAll = new JLabel("<html><a href='' style='color:#5B8FD9;'>View all</a></html>");
        viewAll.setFont(PDSApp.FONT_SMALL);
        viewAll.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(viewAll, BorderLayout.EAST);
        panel.add(titleRow, BorderLayout.NORTH);
        
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);
        
        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void loadTimeline(JPanel listPanel) {
        listPanel.removeAll();
        try {
            java.util.List<Object[]> activities = AnalyticsController.getSystemActivityTimeline();
            if (activities.isEmpty()) {
                JLabel empty = new JLabel("No recent activity logged in AUDIT_LOG.");
                empty.setFont(PDSApp.FONT_SMALL);
                empty.setForeground(PDSApp.TEXT_MUTED);
                empty.setBorder(new EmptyBorder(10, 10, 10, 10));
                listPanel.add(empty);
            } else {
                for (int i = 0; i < activities.size(); i++) {
                    Object[] act = activities.get(i);
                    String time = (String) act[0];
                    String op = (String) act[1];
                    String entity = (String) act[2];
                    String user = (String) act[3];
                    
                    JPanel row = new JPanel(new BorderLayout(14, 0)) {
                        @Override protected void paintComponent(Graphics g) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(PDSApp.BORDER_CLR);
                            g2.setStroke(new BasicStroke(2f));
                            g2.drawLine(24, 0, 24, getHeight());
                            g2.setColor(PDSApp.ACCENT);
                            g2.fillOval(19, 12, 10, 10);
                            g2.dispose();
                        }
                    };
                    row.setOpaque(false);
                    row.setBorder(new EmptyBorder(8, 48, 8, 8));
                    
                    JLabel lblTime = new JLabel(time);
                    lblTime.setFont(PDSApp.FONT_MONO);
                    lblTime.setForeground(PDSApp.TEXT_MUTED);
                    lblTime.setPreferredSize(new Dimension(60, 20));
                    
                    JLabel lblDetails = new JLabel("<html><b>" + op + "</b> on " + entity + " by <span style='color:#818CF8'>" + user + "</span></html>");
                    lblDetails.setFont(PDSApp.FONT_LABEL);
                    lblDetails.setForeground(PDSApp.TEXT_PRIMARY);
                    
                    row.add(lblTime, BorderLayout.WEST);
                    row.add(lblDetails, BorderLayout.CENTER);
                    listPanel.add(row);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    private void loadSummary(java.util.List<Object[]> liveChartData, JPanel timelineList, JLabel... labels) {
        try {
            AdminController.SummaryMetrics m = AdminController.getSummaryMetrics();
            labels[0].setText(m.totalBeneficiaries);
            labels[1].setText(m.totalRequests);
            labels[2].setText(m.totalPendingClearances);
            labels[3].setText(m.totalTransactions);
            labels[4].setText(m.lowStockAlerts);
            labels[5].setText(m.totalCommodities);
            labels[6].setText(m.totalShops);
            labels[7].setText(m.totalSuppliers);
            if (labels.length > 8) {
                labels[8].setText(scalar(null, "SELECT COUNT(*) FROM ALLOCATION_RULE"));
            }

            liveChartData.clear();
            liveChartData.addAll(AnalyticsController.getAdminCommodityStats());

            loadTimeline(timelineList);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private String scalar(Statement st, String sql) throws SQLException {
        return AdminController.getScalarValue(sql);
    }

    // ── Beneficiaries ─────────────────────────────────────────────────
    // ── High-End Glassmorphic Beneficiaries Control Hub (Horizontal Auto-Stretch Fixed) ──
    private JPanel buildBeneficiaries() {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setBackground(PAGE); p.setBorder(new EmptyBorder(16, 20, 16, 20));
        
        JTable tbl = PDSApp.styledTable(new String[]{"ID","Name","Ration Card","DOB","Status","Category","Shop"});
        DefaultTableModel model = (DefaultTableModel) tbl.getModel();
        
        tbl.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                String val = (v != null) ? v.toString() : "INACTIVE";
                boolean ok = "ACTIVE".equals(val);
                JLabel pill = PDSApp.statusPill("  " + val + "  ", ok ? PDSApp.SUCCESS : PDSApp.DANGER, ok ? PDSApp.SUCCESS_BG : PDSApp.DANGER_BG);
                if (sel) { pill.setBackground(PDSApp.ACCENT_BG); pill.setForeground(PDSApp.ACCENT); }
                return pill;
            }
        });

        JLabel lblTotalBen = new JLabel("0"), lblActiveBen = new JLabel("0"), lblInactiveBen = new JLabel("0");
        JPanel kpiGrid = new JPanel(new GridLayout(1, 3, 16, 0));
        kpiGrid.setBackground(PAGE); kpiGrid.setPreferredSize(new Dimension(0, 120));
        kpiGrid.setMinimumSize(new Dimension(0, 120));
        kpiGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        kpiGrid.add(PDSApp.statCard("Total Registered", lblTotalBen, PDSApp.ACCENT));
        kpiGrid.add(PDSApp.statCard("Active Status Profile", lblActiveBen, PDSApp.SUCCESS));
        kpiGrid.add(PDSApp.statCard("Suspended Ledgers", lblInactiveBen, PDSApp.DANGER));

        java.util.List<Integer> catIds = new java.util.ArrayList<>(), shopIds = new java.util.ArrayList<>();
        JTextField tfName = PDSApp.inputField(14), tfCard = PDSApp.inputField(14), tfDob = PDSApp.inputField(14), tfUser = PDSApp.inputField(14), tfPass = PDSApp.inputField(14);
        tfDob.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "YYYY-MM-DD");
        JComboBox<String> cbCat = PDSApp.styledCombo(), cbShop = PDSApp.styledCombo();

        Runnable loadDropdowns = () -> {
            cbCat.removeAllItems(); catIds.clear(); cbShop.removeAllItems(); shopIds.clear();
            try {
                java.util.List<Object[]> cats = AdminController.getDropdownCategories();
                for (Object[] cat : cats) {
                    catIds.add((Integer) cat[0]);
                    cbCat.addItem((String) cat[1]);
                }
            } catch(SQLException ex){ex.printStackTrace();}
            try {
                java.util.List<Object[]> shops = AdminController.getDropdownShops();
                for (Object[] shop : shops) {
                    shopIds.add((Integer) shop[0]);
                    cbShop.addItem((String) shop[1]);
                }
            } catch(SQLException ex){ex.printStackTrace();}
        };
        loadDropdowns.run();

        // Left Column Workspace Setup
        JPanel leftColumn = new JPanel(new GridBagLayout());
        leftColumn.setBackground(PAGE);
        GridBagConstraints lGbc = new GridBagConstraints();
        lGbc.fill = GridBagConstraints.BOTH; // Changed to BOTH to allow full component canvas expansion
        lGbc.gridx = 0; lGbc.weightx = 1.0;
        
        JPanel searchToolbar = PDSApp.card();
        searchToolbar.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 8));
        JTextField tfSearch = PDSApp.inputField(14);
        tfSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Search matching profiles...");
        tfSearch.setPreferredSize(new Dimension(240, 36));
        searchToolbar.add(PDSApp.formLabel("🔍 Filter Engine: "));
        searchToolbar.add(tfSearch);
        
        final JTable finalTableReference = tbl;
        tfSearch.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                String term = tfSearch.getText().trim().toLowerCase();
                DefaultTableModel tblModel = (DefaultTableModel) finalTableReference.getModel();
                TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tblModel);
                finalTableReference.setRowSorter(sorter);
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + term));
            }
        });

        JScrollPane tblScroll = PDSApp.tableScroll(tbl);
        
        lGbc.gridy = 0; lGbc.weighty = 0.0; leftColumn.add(searchToolbar, lGbc);
        lGbc.gridy = 1; lGbc.weighty = 1.0; lGbc.fill = GridBagConstraints.BOTH; lGbc.insets = new Insets(12, 0, 0, 0);
        leftColumn.add(tblScroll, lGbc);

        // Right Column Outer Stack Card Panel
        JPanel totalRightColumnScrollStack = new JPanel(new GridBagLayout());
        totalRightColumnScrollStack.setBackground(PAGE);
        GridBagConstraints stackGbc = new GridBagConstraints();
        stackGbc.fill = GridBagConstraints.HORIZONTAL;
        stackGbc.weightx = 1.0;
        stackGbc.gridx = 0;
        
        JPanel formCard = PDSApp.card();
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints fGbc = new GridBagConstraints();
        fGbc.fill = GridBagConstraints.HORIZONTAL; fGbc.weightx = 1.0; fGbc.gridx = 0; fGbc.gridy = 0;
        
        fGbc.insets = new Insets(0, 0, 12, 0);
        formCard.add(PDSApp.sectionLabel("New Beneficiary Induction"), fGbc);
        
        String[] labels = {"Full Identity Name", "Ration Card Number", "Date of Birth", "Taxonomy Category", "Assigned Fair-Price Outlet", "Login Username Handle", "Security Password Nodes"};
        Component[] inputs = {tfName, tfCard, tfDob, cbCat, cbShop, tfUser, tfPass};
        for(int i = 0; i < labels.length; i++) {
            fGbc.gridy++; fGbc.insets = new Insets(0, 0, 4, 0); formCard.add(PDSApp.formLabel(labels[i]), fGbc);
            fGbc.gridy++; fGbc.insets = new Insets(0, 0, 12, 0); formCard.add(inputs[i], fGbc);
        }
        
        JButton btnCreate = PDSApp.primaryBtn("Commit Induction Profile");
        fGbc.gridy++; fGbc.insets = new Insets(8, 0, 0, 0); formCard.add(btnCreate, fGbc);

        JPanel manageCard = PDSApp.card();
        manageCard.setLayout(new GridBagLayout());
        manageCard.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints mGbc = new GridBagConstraints();
        mGbc.fill = GridBagConstraints.HORIZONTAL; mGbc.weightx = 1.0; mGbc.gridx = 0; mGbc.gridy = 0;
        
        mGbc.insets = new Insets(0, 0, 12, 0);
        manageCard.add(PDSApp.sectionLabel("Operational Ledger Commands"), mGbc);
        
        JComboBox<String> cbAssign = PDSApp.styledCombo();
        java.util.List<Integer> assignShopIds = new java.util.ArrayList<>();
        Runnable loadAssign = () -> {
            cbAssign.removeAllItems(); assignShopIds.clear();
            try {
                java.util.List<Object[]> shops = AdminController.getDropdownShops();
                for (Object[] shop : shops) {
                    assignShopIds.add((Integer) shop[0]);
                    cbAssign.addItem((String) shop[1]);
                }
            } catch(SQLException ex){ex.printStackTrace();}
        };
        loadAssign.run();

        mGbc.gridy++; mGbc.insets = new Insets(0, 0, 4, 0); manageCard.add(PDSApp.formLabel("Reassign Fair-Price Shop Link"), mGbc);
        mGbc.gridy++; mGbc.insets = new Insets(0, 0, 14, 0); manageCard.add(cbAssign, mGbc);
        
        JPanel rowBtns = new JPanel(new GridLayout(1, 3, 10, 0));
        rowBtns.setBackground(PDSApp.CARD_BG);
        JButton btnAssign = PDSApp.primaryBtn("Link Shop"), btnToggle = PDSApp.secondaryBtn("Toggle"), btnDelete = PDSApp.dangerBtn("Prune");
        rowBtns.add(btnAssign); rowBtns.add(btnToggle); rowBtns.add(btnDelete);
        mGbc.gridy++; mGbc.insets = new Insets(4, 0, 0, 0); manageCard.add(rowBtns, mGbc);

        stackGbc.gridy = 0; stackGbc.weighty = 0.0; stackGbc.insets = new Insets(0, 0, 0, 0);
        totalRightColumnScrollStack.add(formCard, stackGbc);
        
        stackGbc.gridy = 1; stackGbc.weighty = 0.0; stackGbc.insets = new Insets(16, 0, 0, 0);
        totalRightColumnScrollStack.add(manageCard, stackGbc);

        stackGbc.gridy = 2; stackGbc.weighty = 1.0; stackGbc.fill = GridBagConstraints.BOTH;
        totalRightColumnScrollStack.add(Box.createVerticalGlue(), stackGbc);

        // ── THE HORIZONTAL FLUSH ALIGNMENT FIX ──
        JScrollPane rightScrollPane = new JScrollPane(totalRightColumnScrollStack);
        rightScrollPane.setBorder(null);
        rightScrollPane.setOpaque(false);
        rightScrollPane.getViewport().setOpaque(false);
        rightScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        rightScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        
        // CRITICAL PROPERTY: Forces the scroll pane layout viewport to fill out 100% of the horizontal space allocated to it
        rightScrollPane.putClientProperty(com.formdev.flatlaf.FlatClientProperties.SCROLL_PANE_SMOOTH_SCROLLING, true);
        
        // Split Content Wrapper Construction Layout Configuration
        JPanel contentWrapper = new JPanel(new GridBagLayout());
        contentWrapper.setBackground(PAGE);
        GridBagConstraints wGbc = new GridBagConstraints();
        
        // Force explicit BOTH dimensional fills onto both parent grid sides
        wGbc.fill = GridBagConstraints.BOTH; 
        wGbc.weighty = 1.0;
        
        wGbc.gridx = 0; wGbc.weightx = 0.55; wGbc.insets = new Insets(0, 0, 0, 0);
        contentWrapper.add(leftColumn, wGbc);
        
        wGbc.gridx = 1; wGbc.weightx = 0.45; wGbc.insets = new Insets(0, 20, 0, 0);
        contentWrapper.add(rightScrollPane, wGbc);

        Runnable internalRefreshLoad = () -> {
            loadBeneficiaries(model); loadDropdowns.run(); loadAssign.run();
            int total = model.getRowCount(), active = 0;
            for(int i = 0; i < total; i++) if("ACTIVE".equals(model.getValueAt(i, 4).toString())) active++;
            lblTotalBen.setText(String.valueOf(total)); lblActiveBen.setText(String.valueOf(active)); lblInactiveBen.setText(String.valueOf(total - active));
        };

        btnCreate.addActionListener(e -> {
            String name = tfName.getText().trim(), card = tfCard.getText().trim(), dob = tfDob.getText().trim(), user = tfUser.getText().trim(), pass = tfPass.getText().trim();
            if(name.isEmpty()||card.isEmpty()||dob.isEmpty()||user.isEmpty()||pass.isEmpty()){PDSApp.showAlert(p,"Fill all fields.",true);return;}
            try {
                int catId = catIds.get(cbCat.getSelectedIndex());
                int shopId = shopIds.get(cbShop.getSelectedIndex());
                AdminController.addBeneficiary(name, card, dob, catId, shopId, user, pass);
                tfName.setText(""); tfCard.setText(""); tfDob.setText(""); tfUser.setText(""); tfPass.setText("");
                internalRefreshLoad.run(); PDSApp.showAlert(p,"Beneficiary onboarded safely.",false);
            } catch(SQLException ex){ PDSApp.showAlert(p,ex.getMessage(),true);}
        });

        btnAssign.addActionListener(e -> {
            int row = tbl.getSelectedRow(); if(row < 0){PDSApp.showAlert(p,"Select a beneficiary row first.",true);return;}
            int modelRow = tbl.convertRowIndexToModel(row);
            int benId = Integer.parseInt(model.getValueAt(modelRow,0).toString()); int sid = assignShopIds.get(cbAssign.getSelectedIndex());
            try {
                AdminController.updateBeneficiaryShop(benId, sid);
                internalRefreshLoad.run(); PDSApp.showAlert(p,"Shop assigned.",false);
            } catch(SQLException ex){PDSApp.showAlert(p,ex.getMessage(),true);}
        });

        btnToggle.addActionListener(e -> {
            int row = tbl.getSelectedRow(); if(row < 0){PDSApp.showAlert(p,"Select a beneficiary row first.",true);return;}
            int modelRow = tbl.convertRowIndexToModel(row);
            int id = Integer.parseInt(model.getValueAt(modelRow,0).toString()); String next = "ACTIVE".equals(model.getValueAt(modelRow,4).toString()) ? "INACTIVE" : "ACTIVE";
            try {
                AdminController.updateBeneficiaryStatus(id, next);
                internalRefreshLoad.run();
            } catch(SQLException ex){PDSApp.showAlert(p,ex.getMessage(),true);}
        });

        btnDelete.addActionListener(e -> {
            int row = tbl.getSelectedRow(); if(row < 0){PDSApp.showAlert(p,"Select a target profile to delete.",true);return;}
            int modelRow = tbl.convertRowIndexToModel(row);
            int benId = Integer.parseInt(model.getValueAt(modelRow,0).toString()); String benName = model.getValueAt(modelRow,1).toString();
            if(JOptionPane.showConfirmDialog(p,"Prune records for '"+benName+"'?","Critical Confirmation",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)!=JOptionPane.YES_OPTION)return;
            try {
                AdminController.deleteBeneficiary(benId);
                internalRefreshLoad.run(); PDSApp.showAlert(p,"Beneficiary safely deleted.",false);
            } catch(SQLException ex){ PDSApp.showAlert(p,ex.getMessage(),true);}
        });

        JButton btnTopRefresh = PDSApp.secondaryBtn("⟳  Refresh Deck");
        btnTopRefresh.addActionListener(e -> internalRefreshLoad.run());
        JPanel viewHeader = new JPanel(new BorderLayout()); viewHeader.setBackground(PAGE);
        viewHeader.add(PDSApp.pageHeader("Beneficiary Register"), BorderLayout.WEST); viewHeader.add(btnTopRefresh, BorderLayout.EAST);
        viewHeader.setBorder(new EmptyBorder(0, 0, 16, 0));

        JPanel masterStack = new JPanel(new GridBagLayout()); masterStack.setBackground(PAGE);
        GridBagConstraints mGbcLayout = new GridBagConstraints(); mGbcLayout.fill = GridBagConstraints.HORIZONTAL; mGbcLayout.gridx = 0; mGbcLayout.weightx = 1.0;
        mGbcLayout.gridy = 0; masterStack.add(viewHeader, mGbcLayout);
        mGbcLayout.gridy = 1; masterStack.add(kpiGrid, mGbcLayout);
        mGbcLayout.gridy = 2; mGbcLayout.weighty = 1.0; mGbcLayout.fill = GridBagConstraints.BOTH; mGbcLayout.insets = new Insets(20, 0, 0, 0); masterStack.add(contentWrapper, mGbcLayout);

        p.add(masterStack, BorderLayout.CENTER);
        internalRefreshLoad.run(); return p;
    }
    
    private void loadBeneficiaries(DefaultTableModel m) {
        m.setRowCount(0);
        String sql = "SELECT b.BeneficiaryID, b.Name, b.RationCardNo, b.DOB, b.Status, c.CategoryName, f.ShopName " +
                     "FROM BENEFICIARY b " +
                     "JOIN CATEGORY c ON c.CategoryID = b.CategoryID " +
                     "JOIN FAIR_PRICE_SHOP f ON f.ShopID = b.ShopID " +
                     "ORDER BY b.BeneficiaryID";
        try {
            java.util.List<Object[]> rows = AdminController.executeGenericQuery(sql);
            for (Object[] row : rows) {
                m.addRow(row);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // ── Shops ─────────────────────────────────────────────────────────
    // ── High-End Glassmorphic Shops Command Hub (Fixed Sizing Setup) ──
    private JPanel buildShops() {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setBackground(PAGE); p.setBorder(new EmptyBorder(16, 20, 16, 20));
        
        JTable tbl = PDSApp.styledTable(new String[]{"ID","Shop Name","Location","Capacity Bounds","Supply Warehouse"});
        DefaultTableModel model = (DefaultTableModel) tbl.getModel();
        
        JLabel lblTotalShops = new JLabel("0"), lblAggregateCap = new JLabel("0 Kg"), lblOptimalDepot = new JLabel("Optimal");
        JPanel kpiGrid = new JPanel(new GridLayout(1, 3, 16, 0));
        kpiGrid.setBackground(PAGE); kpiGrid.setPreferredSize(new Dimension(0, 120));
        kpiGrid.setMinimumSize(new Dimension(0, 120));
        kpiGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        kpiGrid.add(PDSApp.statCard("Total Active Outlets", lblTotalShops, PDSApp.ACCENT));
        kpiGrid.add(PDSApp.statCard("Aggregate Metric Volume", lblAggregateCap, PDSApp.ACCENT));
        kpiGrid.add(PDSApp.statCard("Operations Health Status", lblOptimalDepot, PDSApp.SUCCESS));

        JTextField tfName = PDSApp.inputField(14), tfLoc = PDSApp.inputField(14), tfCap = PDSApp.inputField(14), tfUser = PDSApp.inputField(14), tfPass = PDSApp.inputField(14);
        tfCap.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Storage metric weight max...");

        // Load warehouses dropdown data
        JComboBox<String> cbWarehouse = PDSApp.styledCombo();
        java.util.List<Integer> warehouseIds = new java.util.ArrayList<>();
        Runnable loadWarehouses = () -> {
            cbWarehouse.removeAllItems(); warehouseIds.clear();
            try {
                java.util.List<Object[]> whs = AdminController.executeGenericQuery("SELECT WarehouseID, Location FROM WAREHOUSE ORDER BY WarehouseID");
                for (Object[] wh : whs) {
                    warehouseIds.add((Integer) wh[0]);
                    cbWarehouse.addItem(wh[0] + " - " + wh[1]);
                }
            } catch (SQLException ex) { ex.printStackTrace(); }
        };
        loadWarehouses.run();

        JPanel contentWrapper = new JPanel(new GridBagLayout());
        contentWrapper.setBackground(PAGE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;

        JPanel leftColumn = new JPanel(new BorderLayout(0, 12)); leftColumn.setBackground(PAGE);
        JPanel searchToolbar = PDSApp.card(); searchToolbar.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 0));
        searchToolbar.setPreferredSize(new Dimension(0, 54)); searchToolbar.setBorder(new EmptyBorder(8, 12, 8, 12));
        JTextField tfSearch = PDSApp.inputField(14); tfSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Search operational outlets...");
        tfSearch.setPreferredSize(new Dimension(200, 36));
        searchToolbar.add(PDSApp.formLabel("🔍 Filter Engine: ")); searchToolbar.add(tfSearch);
        
        tfSearch.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                String term = tfSearch.getText().trim().toLowerCase();
                TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
                tbl.setRowSorter(sorter); sorter.setRowFilter(RowFilter.regexFilter("(?i)" + term));
            }
        });
        
        JScrollPane tblScroll = PDSApp.tableScroll(tbl);
        leftColumn.add(searchToolbar, BorderLayout.NORTH); leftColumn.add(tblScroll, BorderLayout.CENTER);

        JPanel rightColumnStack = new JPanel(new GridBagLayout()); rightColumnStack.setBackground(PAGE);
        GridBagConstraints stackGbc = new GridBagConstraints();
        stackGbc.fill = GridBagConstraints.HORIZONTAL; stackGbc.weightx = 1.0; stackGbc.gridx = 0;

        JPanel formCard = PDSApp.card(); formCard.setLayout(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints fGbc = new GridBagConstraints();
        fGbc.fill = GridBagConstraints.HORIZONTAL; fGbc.weightx = 1.0; fGbc.gridx = 0; fGbc.gridy = 0; fGbc.insets = new Insets(0, 0, 10, 0);
        
        formCard.add(PDSApp.sectionLabel("Establish New Retail Outlet"), fGbc);
        
        String[] labels = {"Fair-Price Shop Name", "Geographical Location Coordinates", "Max Storage Capacity Limit (Kg)", "Assigned Supply Warehouse", "Operator Account Username Handle", "Operator Security Credentials"};
        Component[] inputs = {tfName, tfLoc, tfCap, cbWarehouse, tfUser, tfPass};
        for(int i = 0; i < labels.length; i++) {
            fGbc.gridy++; fGbc.insets = new Insets(0, 0, 2, 0); formCard.add(PDSApp.formLabel(labels[i]), fGbc);
            // CRITICAL HEIGHT LOCK APPLIED BELOW
            fGbc.gridy++; fGbc.insets = new Insets(0, 0, 12, 0); 
            inputs[i].setPreferredSize(new Dimension(100, 36)); 
            inputs[i].setMinimumSize(new Dimension(100, 36));
            formCard.add(inputs[i], fGbc);
        }
        
        JPanel rowBtns = new JPanel(new GridLayout(1, 2, 10, 0)); rowBtns.setBackground(PDSApp.CARD_BG);
        JButton btnCreate = PDSApp.primaryBtn("Commit Outlet"); JButton btnDelete = PDSApp.dangerBtn("Prune Depot");
        rowBtns.add(btnCreate); rowBtns.add(btnDelete);
        fGbc.gridy++; fGbc.insets = new Insets(6, 0, 0, 0); formCard.add(rowBtns, fGbc);
        
        stackGbc.gridy = 0; stackGbc.weighty = 0.0;
        rightColumnStack.add(formCard, stackGbc);

        // Management Operations: Reassign Warehouse
        JPanel manageCard = PDSApp.card();
        manageCard.setLayout(new GridBagLayout());
        manageCard.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints mGbc = new GridBagConstraints();
        mGbc.fill = GridBagConstraints.HORIZONTAL; mGbc.weightx = 1.0; mGbc.gridx = 0; mGbc.gridy = 0;
        
        mGbc.insets = new Insets(0, 0, 12, 0);
        manageCard.add(PDSApp.sectionLabel("Reassign Depot Warehouse Link"), mGbc);

        JComboBox<String> cbAssignWarehouse = PDSApp.styledCombo();
        java.util.List<Integer> assignWarehouseIds = new java.util.ArrayList<>();
        Runnable loadAssignWarehouse = () -> {
            cbAssignWarehouse.removeAllItems(); assignWarehouseIds.clear();
            try {
                java.util.List<Object[]> whs = AdminController.executeGenericQuery("SELECT WarehouseID, Location FROM WAREHOUSE ORDER BY WarehouseID");
                for (Object[] wh : whs) {
                    assignWarehouseIds.add((Integer) wh[0]);
                    cbAssignWarehouse.addItem(wh[0] + " - " + wh[1]);
                }
            } catch(SQLException ex){ex.printStackTrace();}
        };
        loadAssignWarehouse.run();
        
        mGbc.gridy++; mGbc.insets = new Insets(0, 0, 4, 0); manageCard.add(PDSApp.formLabel("Select Supply Warehouse"), mGbc);
        mGbc.gridy++; mGbc.insets = new Insets(0, 0, 14, 0); manageCard.add(cbAssignWarehouse, mGbc);
        
        JButton btnReassign = PDSApp.primaryBtn("Link Warehouse");
        mGbc.gridy++; mGbc.insets = new Insets(4, 0, 0, 0); manageCard.add(btnReassign, mGbc);

        stackGbc.gridy = 1; stackGbc.weighty = 0.0; stackGbc.insets = new Insets(16, 0, 0, 0);
        rightColumnStack.add(manageCard, stackGbc);

        stackGbc.gridy = 2; stackGbc.weighty = 1.0; stackGbc.fill = GridBagConstraints.BOTH;
        rightColumnStack.add(Box.createVerticalGlue(), stackGbc);

        JScrollPane rightScrollPane = new JScrollPane(rightColumnStack);
        rightScrollPane.setBorder(null);
        rightScrollPane.setOpaque(false);
        rightScrollPane.getViewport().setOpaque(false);
        rightScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        rightScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        gbc.gridx = 0; gbc.weightx = 0.58; contentWrapper.add(leftColumn, gbc);
        gbc.gridx = 1; gbc.weightx = 0.42; gbc.insets = new Insets(0, 18, 0, 0); contentWrapper.add(rightScrollPane, gbc);

        Runnable loadShopsEngine = () -> {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                int runningTotalTemp = 0;
                long cumulativeCapTemp = 0;
                try {
                    java.util.List<Object[]> shops = AdminController.executeGenericQuery(
                        "SELECT s.ShopID, s.ShopName, s.Location, s.Capacity, w.Location " +
                        "FROM FAIR_PRICE_SHOP s LEFT JOIN WAREHOUSE w ON w.WarehouseID = s.WarehouseID " +
                        "ORDER BY s.ShopName"
                    );
                    java.util.List<Object[]> processed = new java.util.ArrayList<>();
                    for (Object[] row : shops) {
                        int capValue = (Integer) row[3]; runningTotalTemp++; cumulativeCapTemp += capValue;
                        String whLoc = row[4] != null ? row[4].toString() : "Not Linked";
                        processed.add(new Object[]{row[0], row[1], row[2], capValue + " Kg", whLoc});
                    }
                    final int finalRunningTotal = runningTotalTemp;
                    final long finalCumulativeCap = cumulativeCapTemp;
                    SwingUtilities.invokeLater(() -> {
                        model.setRowCount(0);
                        for (Object[] row : processed) {
                            model.addRow(row);
                        }
                        lblTotalShops.setText(String.valueOf(finalRunningTotal)); lblAggregateCap.setText(String.format("%,d Kg", finalCumulativeCap));
                    });
                } catch(SQLException ex){ex.printStackTrace();}
            });
        };

        btnCreate.addActionListener(e -> {
            String sn = tfName.getText().trim(), loc = tfLoc.getText().trim(), cap = tfCap.getText().trim(), user = tfUser.getText().trim(), pass = tfPass.getText().trim();
            if(sn.isEmpty()||loc.isEmpty()||cap.isEmpty()||user.isEmpty()||pass.isEmpty()){PDSApp.showAlert(p,"Fill all fields.",true);return;}
            int capacity; try{capacity = Integer.parseInt(cap); if(capacity <= 0) throw new NumberFormatException();} catch(NumberFormatException ex){PDSApp.showAlert(p,"Capacity must be a positive number.",true);return;}
            if (cbWarehouse.getSelectedIndex() < 0) { PDSApp.showAlert(p, "Select a supply warehouse first.", true); return; }
            int whId = warehouseIds.get(cbWarehouse.getSelectedIndex());
            try {
                AdminController.addShop(sn, loc, capacity, whId, user, pass);
                tfName.setText(""); tfLoc.setText(""); tfCap.setText(""); tfUser.setText(""); tfPass.setText("");
                loadShopsEngine.run(); PDSApp.showAlert(p,"Outlet registered successfully.",false);
            } catch(SQLException ex){ PDSApp.showAlert(p,ex.getMessage(),true);}
        });

        btnDelete.addActionListener(e -> {
            int row = tbl.getSelectedRow(); if(row < 0){PDSApp.showAlert(p,"Select shop first.",true);return;}
            int modelRow = tbl.convertRowIndexToModel(row);
            int shopId = Integer.parseInt(model.getValueAt(modelRow,0).toString()); String shopName = model.getValueAt(modelRow,1).toString();
            if(JOptionPane.showConfirmDialog(p,"Terminate outlet '"+shopName+"'?","Security Exception",JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE)!=JOptionPane.YES_OPTION)return;
            try {
                AdminController.deleteShop(shopId);
                loadShopsEngine.run(); PDSApp.showAlert(p,"Shop references dropped.",false);
            } catch(SQLException ex){ PDSApp.showAlert(p,ex.getMessage(),true);}
        });

        btnReassign.addActionListener(e -> {
            int row = tbl.getSelectedRow(); if (row < 0) { PDSApp.showAlert(p, "Select a shop first.", true); return; }
            int modelRow = tbl.convertRowIndexToModel(row);
            int shopId = Integer.parseInt(model.getValueAt(modelRow, 0).toString());
            if (cbAssignWarehouse.getSelectedIndex() < 0) { PDSApp.showAlert(p, "Select a warehouse first.", true); return; }
            int whId = assignWarehouseIds.get(cbAssignWarehouse.getSelectedIndex());
            try {
                AdminController.updateShopWarehouse(shopId, whId);
                loadShopsEngine.run(); PDSApp.showAlert(p, "Warehouse linkage updated successfully.", false);
            } catch (SQLException ex) { PDSApp.showAlert(p, ex.getMessage(), true); }
        });

        JButton btnTopRefresh = PDSApp.secondaryBtn("⟳  Refresh Deck");
        btnTopRefresh.addActionListener(e -> { loadWarehouses.run(); loadAssignWarehouse.run(); loadShopsEngine.run(); });
        JPanel viewHeader = new JPanel(new BorderLayout()); viewHeader.setBackground(PAGE);
        viewHeader.add(PDSApp.pageHeader("Retail Shops Registry"), BorderLayout.WEST); viewHeader.add(btnTopRefresh, BorderLayout.EAST);
        viewHeader.setBorder(new EmptyBorder(0, 0, 16, 0));

        JPanel masterStack = new JPanel(new GridBagLayout()); masterStack.setBackground(PAGE);
        GridBagConstraints mGbcLayout = new GridBagConstraints(); mGbcLayout.fill = GridBagConstraints.HORIZONTAL; mGbcLayout.gridx = 0; mGbcLayout.weightx = 1.0;
        mGbcLayout.gridy = 0; masterStack.add(viewHeader, mGbcLayout);
        mGbcLayout.gridy = 1; masterStack.add(kpiGrid, mGbcLayout);
        mGbcLayout.gridy = 2; mGbcLayout.weighty = 1.0; mGbcLayout.fill = GridBagConstraints.BOTH; mGbcLayout.insets = new Insets(16, 0, 0, 0); masterStack.add(contentWrapper, mGbcLayout);
        p.add(masterStack, BorderLayout.CENTER);
        loadShopsEngine.run(); return p;
    }

    // ── Redesigned CRUD Sections ──────────────────────────────────────
    private JPanel buildCategories() {
        String[] mockMetrics = {"Total Classes", "Allocated Rules", "Target Scopes", "Sync Status"};
        JLabel[] mockLabels = {new JLabel("4"), new JLabel("12"), new JLabel("Active"), new JLabel("100%")};
        Color[] mockColors = {PDSApp.ACCENT, PDSApp.ACCENT, PDSApp.ACCENT, PDSApp.SUCCESS};

        return buildEnterpriseWorkspace("Categories", new String[]{"ID", "Name", "Description"},
                "SELECT CategoryID,CategoryName,Description FROM CATEGORY ORDER BY CategoryID",
                new String[]{"Category Name", "Description"},
                vals -> {
                    try {
                        AdminController.addCategory(vals[0], vals[1]);
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    }
                },
                (model, row) -> {
                    int id = Integer.parseInt(model.getValueAt(row, 0).toString());
                    try {
                        AdminController.deleteCategory(id);
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex.getMessage().contains("foreign key") ? "Cannot delete: beneficiaries or rules are linked." : ex.getMessage());
                    }
                },
                mockMetrics, mockLabels, mockColors, "Structural Taxonomy Layout Matrix");
    }

    private JPanel buildCommodities() {
        String[] mockMetrics = {"Global Skus", "Standard Metrics", "Stock Buffers", "Hazmat Tracking"};
        JLabel[] mockLabels = {new JLabel("8"), new JLabel("Kg / Ltr"), new JLabel("Secure"), new JLabel("Disabled")};
        Color[] mockColors = {PDSApp.ACCENT, PDSApp.ACCENT, PDSApp.SUCCESS, PDSApp.TEXT_MUTED};

        return buildEnterpriseWorkspace("Commodities", new String[]{"ID", "Name", "Unit", "Expiry Tracking"},
                "SELECT CommodityID,CommodityName,Unit,ExpiryTracking FROM COMMODITY ORDER BY CommodityID",
                new String[]{"Commodity Name"},
                vals -> {
                    try {
                        AdminController.addCommodity(vals[0]);
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    }
                },
                (model, row) -> {
                    int id = Integer.parseInt(model.getValueAt(row, 0).toString());
                    try {
                        AdminController.deleteCommodity(id);
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex.getMessage().contains("foreign key") ? "Cannot delete: linked requests or rules." : ex.getMessage());
                    }
                },
                mockMetrics, mockLabels, mockColors, "Fulfillment Ledger Nodes");
    }

    private JPanel buildSuppliers() {
        String[] mockMetrics = {"Total Vendors", "Compliance Rate", "Active Chains", "Pending Audits"};
        JLabel[] mockLabels = {new JLabel("14"), new JLabel("98.4%"), new JLabel("Direct"), new JLabel("None")};
        Color[] mockColors = {PDSApp.ACCENT, PDSApp.SUCCESS, PDSApp.ACCENT, PDSApp.TEXT_MUTED};

        return buildEnterpriseWorkspace("Suppliers", new String[]{"ID", "Organization", "GST Number", "Status"},
                "SELECT SupplierID,OrganizationName,GSTNumber,Status FROM SUPPLIER ORDER BY SupplierID",
                new String[]{"Organization Name", "GST Number"},
                vals -> {
                    try {
                        AdminController.addSupplier(vals[0], vals[1]);
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    }
                }, (model, row) -> {
                    int id = Integer.parseInt(model.getValueAt(row, 0).toString());
                    try {
                        AdminController.deleteSupplier(id);
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex.getMessage().contains("foreign key") ? "Cannot delete: linked batches." : ex.getMessage());
                    }
                },
                mockMetrics, mockLabels, mockColors, "Vendor Registry Manifest Layer");
    }

    private JPanel buildWarehouses() {
        String[] mockMetrics = {"Total Depots", "Aggregate Space", "Utilization Index", "Safety Level"};
        JLabel[] mockLabels = {new JLabel("6"), new JLabel("84,500 Kg"), new JLabel("42%"), new JLabel("Optimal")};
        Color[] mockColors = {PDSApp.ACCENT, PDSApp.SUCCESS, PDSApp.ACCENT, PDSApp.SUCCESS};

        return buildEnterpriseWorkspace("Warehouses", new String[]{"ID", "Location", "Capacity"},
                "SELECT WarehouseID,Location,Capacity FROM WAREHOUSE ORDER BY WarehouseID",
                new String[]{"Location", "Capacity"},
                vals -> {
                    try {
                        int cap;
                        try {
                            cap = Integer.parseInt(vals[1]);
                        } catch (NumberFormatException e) {
                            throw new RuntimeException("Capacity must be a whole number.");
                        }
                        if (cap <= 0) {
                            throw new RuntimeException("Capacity must be >0.");
                        }
                        AdminController.addWarehouse(vals[0], cap);
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    }
                }, (model, row) -> {
                    int id = Integer.parseInt(model.getValueAt(row, 0).toString());
                    try {
                        AdminController.deleteWarehouse(id);
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex.getMessage().contains("foreign key") ? "Cannot delete: linked batches." : ex.getMessage());
                    }
                },
                mockMetrics, mockLabels, mockColors, "Logistics Operations Base Grid");
    }

    // ── Allocation Rules ──────────────────────────────────────────────
    // ── High-End Glassmorphic Allocation Rules Command Hub (Fixed Layout) ──
    // ── High-End Glassmorphic Allocation Rules Command Hub (Fixed Layout) ──
    // ── High-End Glassmorphic Allocation Rules Command Hub (Perfect Layout Edition) ──
    private JPanel buildRules() {
        JPanel p = new JPanel(new BorderLayout(0, 0));
        p.setBackground(PAGE);
        p.setBorder(new EmptyBorder(16, 20, 16, 20));
        
        JTable tbl = PDSApp.styledTable(new String[]{"Rule ID","Category Scope","Commodity Target","Monthly Quota Balance","Per-Visit Draw Cap"});
        DefaultTableModel model = (DefaultTableModel) tbl.getModel();
        
        JLabel lblActiveRules = new JLabel("0"), lblStrictCaps = new JLabel("Enforced"), lblSyncState = new JLabel("Synchronized");
        JPanel kpiGrid = new JPanel(new GridLayout(1, 3, 16, 0));
        kpiGrid.setBackground(PAGE);
        kpiGrid.setPreferredSize(new Dimension(0, 120));
        kpiGrid.setMinimumSize(new Dimension(0, 120));
        kpiGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        kpiGrid.add(PDSApp.statCard("Active Rule Trees", lblActiveRules, new Color(0x7C3AED)));
        kpiGrid.add(PDSApp.statCard("Quota Compliance Matrix", lblStrictCaps, PDSApp.WARNING));
        kpiGrid.add(PDSApp.statCard("Core System State", lblSyncState, PDSApp.SUCCESS));

        JComboBox<String> cbCat = PDSApp.styledCombo(), cbCom = PDSApp.styledCombo();
        JSpinner spM = PDSApp.styledSpinner(1, 9999), spV = PDSApp.styledSpinner(1, 9999);

        java.util.List<Integer> catIds = new java.util.ArrayList<>(), comIds = new java.util.ArrayList<>();
        Runnable loadDD = () -> {
            cbCat.removeAllItems(); catIds.clear(); cbCom.removeAllItems(); comIds.clear();
            try {
                java.util.List<Object[]> cats = AdminController.getDropdownCategories();
                for (Object[] cat : cats) {
                    catIds.add((Integer) cat[0]);
                    cbCat.addItem((String) cat[1]);
                }
            } catch(SQLException ex){ex.printStackTrace();}
            try {
                java.util.List<Object[]> coms = AdminController.executeGenericQuery("SELECT CommodityID, CommodityName FROM COMMODITY ORDER BY CommodityID");
                for (Object[] com : coms) {
                    comIds.add((Integer) com[0]);
                    cbCom.addItem((String) com[1]);
                }
            } catch(SQLException ex){ex.printStackTrace();}
        };
        loadDD.run();

        // Left Column Setup
        JPanel leftColumn = new JPanel(new GridBagLayout());
        leftColumn.setBackground(PAGE);
        GridBagConstraints lGbc = new GridBagConstraints();
        lGbc.fill = GridBagConstraints.HORIZONTAL;
        lGbc.gridx = 0; lGbc.weightx = 1.0;

        JPanel searchToolbar = PDSApp.card();
        searchToolbar.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 8));
        JTextField tfSearch = PDSApp.inputField(14);
        tfSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Filter matching parameters...");
        tfSearch.setPreferredSize(new Dimension(240, 36));
        searchToolbar.add(PDSApp.formLabel("🔍 Filter Engine: "));
        searchToolbar.add(tfSearch);
        
        tfSearch.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                String term = tfSearch.getText().trim().toLowerCase();
                TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
                tbl.setRowSorter(sorter);
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + term));
            }
        });

        JScrollPane tblScroll = PDSApp.tableScroll(tbl);
        
        lGbc.gridy = 0; lGbc.weighty = 0.0; leftColumn.add(searchToolbar, lGbc);
        lGbc.gridy = 1; lGbc.weighty = 1.0; lGbc.fill = GridBagConstraints.BOTH; lGbc.insets = new Insets(12, 0, 0, 0);
        leftColumn.add(tblScroll, lGbc);

        // Right Column Setup
        JPanel rightScrollContainer = new JPanel(new GridBagLayout());
        rightScrollContainer.setBackground(PAGE);
        GridBagConstraints rGbc = new GridBagConstraints();
        rGbc.fill = GridBagConstraints.HORIZONTAL;
        rGbc.gridx = 0; rGbc.weightx = 1.0;
        
        JPanel formCard = PDSApp.card();
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints fGbc = new GridBagConstraints();
        fGbc.fill = GridBagConstraints.HORIZONTAL; fGbc.weightx = 1.0; fGbc.gridx = 0; fGbc.gridy = 0;
        
        fGbc.insets = new Insets(0, 0, 12, 0);
        formCard.add(PDSApp.sectionLabel("New Allocation Restriction Rule"), fGbc);
        
        String[] labels = {"Beneficiary Category Target Map", "Commodity Entitlement SKU Target", "Maximum Monthly Allocation Quota", "Single-Visit Withdrawal Bounds"};
        Component[] inputs = {cbCat, cbCom, spM, spV};
        for(int i = 0; i < labels.length; i++) {
            fGbc.gridy++; fGbc.insets = new Insets(0, 0, 4, 0); formCard.add(PDSApp.formLabel(labels[i]), fGbc);
            fGbc.gridy++; fGbc.insets = new Insets(0, 0, 14, 0); formCard.add(inputs[i], fGbc);
        }
        
        JPanel rowBtns = new JPanel(new GridLayout(1, 2, 12, 0));
        rowBtns.setBackground(PDSApp.CARD_BG);
        JButton btnAdd = PDSApp.primaryBtn("Commit Rule");
        JButton btnDel = PDSApp.dangerBtn("Drop Rule");
        rowBtns.add(btnAdd); rowBtns.add(btnDel);
        fGbc.gridy++; fGbc.insets = new Insets(6, 0, 0, 0); formCard.add(rowBtns, fGbc);

        rGbc.gridy = 0; rGbc.weighty = 0.0; rGbc.insets = new Insets(0, 0, 0, 0);
        rightScrollContainer.add(formCard, rGbc);
        rGbc.gridy = 1; rGbc.weighty = 1.0; rGbc.fill = GridBagConstraints.BOTH;
        rightScrollContainer.add(Box.createVerticalGlue(), rGbc);

        JScrollPane rightScrollPane = new JScrollPane(rightScrollContainer);
        rightScrollPane.setBorder(null);
        rightScrollPane.setOpaque(false);
        rightScrollPane.getViewport().setOpaque(false);
        rightScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        rightScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Main Component Layout Core Split Linker Assembly
        JPanel contentWrapper = new JPanel(new GridBagLayout());
        contentWrapper.setBackground(PAGE);
        GridBagConstraints wGbc = new GridBagConstraints();
        wGbc.fill = GridBagConstraints.BOTH; wGbc.weighty = 1.0;
        
        wGbc.gridx = 0; wGbc.weightx = 0.55; wGbc.insets = new Insets(0, 0, 0, 0);
        contentWrapper.add(leftColumn, wGbc);
        wGbc.gridx = 1; wGbc.weightx = 0.45; wGbc.insets = new Insets(0, 20, 0, 0);
        contentWrapper.add(rightScrollPane, wGbc);

        Runnable loadRulesEngine = () -> {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    java.util.List<Object[]> rules = AdminController.executeGenericQuery("SELECT ar.RuleID,cat.CategoryName,c.CommodityName,ar.MonthlyQuota,ar.PerVisitLimit FROM ALLOCATION_RULE ar JOIN CATEGORY cat ON cat.CategoryID=ar.CategoryID JOIN COMMODITY c ON c.CommodityID=ar.CommodityID ORDER BY ar.RuleID");
                    final int totalRulesCalculated = rules.size();
                    SwingUtilities.invokeLater(() -> {
                        model.setRowCount(0);
                        for (Object[] row : rules) {
                            model.addRow(row);
                        }
                        lblActiveRules.setText(String.valueOf(totalRulesCalculated));
                    });
                } catch(SQLException ex){ex.printStackTrace();}
            });
        };

        btnAdd.addActionListener(e -> {
            if(catIds.isEmpty()||comIds.isEmpty()){PDSApp.showAlert(p,"No categories or commodities setup.",true);return;}
            try {
                int catId = catIds.get(cbCat.getSelectedIndex());
                int comId = comIds.get(cbCom.getSelectedIndex());
                AdminController.addAllocationRule(catId, comId, (int)spM.getValue(), (int)spV.getValue());
                loadRulesEngine.run(); PDSApp.showAlert(p,"Allocation bounds verified and deployed.",false);
            } catch(SQLException ex){PDSApp.showAlert(p,ex.getMessage().contains("Duplicate")?"Matrix constraint collision.":ex.getMessage(),true);}});

        btnDel.addActionListener(e -> {
            int row = tbl.getSelectedRow(); if(row < 0){PDSApp.showAlert(p,"Select a rule parameters node first.",true);return;}
            int modelRow = tbl.convertRowIndexToModel(row);
            int id = Integer.parseInt(model.getValueAt(modelRow,0).toString());
            try {
                AdminController.deleteAllocationRule(id);
                loadRulesEngine.run(); PDSApp.showAlert(p,"Entitlement tree deleted.",false);
            } catch(SQLException ex){PDSApp.showAlert(p,ex.getMessage(),true);}
        });

        JButton btnTopRefresh = PDSApp.secondaryBtn("⟳  Refresh Deck");
        btnTopRefresh.addActionListener(e -> { loadDD.run(); loadRulesEngine.run(); });
        JPanel viewHeader = new JPanel(new BorderLayout()); viewHeader.setBackground(PAGE);
        viewHeader.add(PDSApp.pageHeader("Allocation Enforcements Ledger"), BorderLayout.WEST); viewHeader.add(btnTopRefresh, BorderLayout.EAST);
        viewHeader.setBorder(new EmptyBorder(0, 0, 16, 0));

        JPanel masterStack = new JPanel(new GridBagLayout()); masterStack.setBackground(PAGE);
        GridBagConstraints mGbcLayout = new GridBagConstraints(); mGbcLayout.fill = GridBagConstraints.HORIZONTAL; mGbcLayout.gridx = 0; mGbcLayout.weightx = 1.0;
        mGbcLayout.gridy = 0; masterStack.add(viewHeader, mGbcLayout);
        mGbcLayout.gridy = 1; masterStack.add(kpiGrid, mGbcLayout);
        mGbcLayout.gridy = 2; mGbcLayout.weighty = 1.0; mGbcLayout.fill = GridBagConstraints.BOTH; mGbcLayout.insets = new Insets(20, 0, 0, 0); masterStack.add(contentWrapper, mGbcLayout);

        p.add(masterStack, BorderLayout.CENTER);
        loadRulesEngine.run(); return p;
    }

    // ── High-End SaaS Split Workspace Factory ─────────────────────────
    // ── High-End SaaS Split Workspace Factory (Fixed Alignment Edition) ──
    // ── High-End SaaS Split Workspace Factory (Animated Spline Edition) ──
    // ── High-End SaaS Split Workspace Factory (Live Data Spline Edition) ──
    // ── High-End SaaS Split Workspace Factory (Fixed Layout Constraints) ──
    private JPanel buildEnterpriseWorkspace(String title, String[] cols, String query, String[] fieldLabels, 
        AddAction onAdd, DelAction onDel, 
        String[] metricTitles, JLabel[] metricLabels, Color[] metricColors, 
        String illustrationLabel) {

JPanel outerPanel = new JPanel(new BorderLayout(0, 0));
outerPanel.setBackground(PAGE);
outerPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

// 1. Header Section
JPanel viewHeader = new JPanel(new BorderLayout());
viewHeader.setBackground(PAGE);
viewHeader.add(PDSApp.pageHeader(title + " Deck"), BorderLayout.WEST);

JButton btnRefDeck = PDSApp.secondaryBtn("⟳  Refresh Deck");
viewHeader.add(btnRefDeck, BorderLayout.EAST);
viewHeader.setBorder(new EmptyBorder(0, 0, 16, 0));

// 2. Dynamic KPI Layer
JPanel kpiGrid = new JPanel(new GridLayout(1, 4, 16, 0));
kpiGrid.setBackground(PAGE);
kpiGrid.setPreferredSize(new Dimension(0, 120));
kpiGrid.setMinimumSize(new Dimension(0, 120));
kpiGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
for (int i = 0; i < 4; i++) {
kpiGrid.add(PDSApp.statCard(metricTitles[i], metricLabels[i], metricColors[i]));
}

// 3. Main Split Content Panel
JPanel contentWrapper = new JPanel(new GridBagLayout());
contentWrapper.setBackground(PAGE);
GridBagConstraints gbc = new GridBagConstraints();

// LEFT COLUMN: Filter Toolbar & Scrollable Table Data Grid
JPanel leftColumn = new JPanel(new BorderLayout(0, 12));
leftColumn.setBackground(PAGE);

JPanel searchToolbar = PDSApp.card();
searchToolbar.setLayout(new GridBagLayout());
searchToolbar.setPreferredSize(new Dimension(0, 56));
searchToolbar.setMinimumSize(new Dimension(0, 56));
searchToolbar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
searchToolbar.setBorder(new EmptyBorder(0, 16, 0, 16));

GridBagConstraints tGbc = new GridBagConstraints();
tGbc.fill = GridBagConstraints.VERTICAL;
tGbc.anchor = GridBagConstraints.WEST;

tGbc.gridx = 0; tGbc.weightx = 0.0;
searchToolbar.add(PDSApp.formLabel("🔍 Filter Engine:  "), tGbc);

JTextField tfSearch = PDSApp.inputField(14);
tfSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Search records...");
tfSearch.setPreferredSize(new Dimension(180, 34));
tGbc.gridx = 1; tGbc.weightx = 0.5;
tGbc.insets = new Insets(0, 4, 0, 12);
searchToolbar.add(tfSearch, tGbc);

JComboBox<String> cbScopeFilter = PDSApp.styledCombo();
cbScopeFilter.addItem("Display All System Records");
cbScopeFilter.setPreferredSize(new Dimension(200, 34));
tGbc.gridx = 2; tGbc.weightx = 0.5;
tGbc.insets = new Insets(0, 0, 0, 0);
searchToolbar.add(cbScopeFilter, tGbc);

        JTable tbl = PDSApp.styledTable(cols);
        DefaultTableModel model = (DefaultTableModel) tbl.getModel();

        final java.util.List<Integer> liveGraphData = new java.util.ArrayList<>();

        Runnable loadEngine = () -> {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                int totalRowsCalculated = 0;
                java.util.List<Integer> tempGraphData = new java.util.ArrayList<>();
                java.util.List<Object[]> rows = null;
                
                String val1 = "";
                String val2 = "";
                java.util.List<Object[]> grs = null;
                
                try {
                    rows = AdminController.executeGenericQuery(query);
                    totalRowsCalculated = rows.size();
                    
                    if ("Categories".equals(title)) {
                        val1 = AdminController.getScalarValue("SELECT COUNT(*) FROM ALLOCATION_RULE");
                        for (int i = 0; i < totalRowsCalculated; i++) {
                            tempGraphData.add(25 + (i * 12) % 65);
                        }
                    } else if ("Commodities".equals(title)) {
                        for (int i = 0; i < totalRowsCalculated; i++) {
                            tempGraphData.add(30 + (i * 10) % 55);
                        }
                    } else if ("Suppliers".equals(title)) {
                        for (int i = 0; i < totalRowsCalculated; i++) {
                            tempGraphData.add(25 + (i * 15) % 60);
                        }
                    } else if ("Warehouses".equals(title)) {
                        val1 = AdminController.getScalarValue("SELECT COALESCE(SUM(Capacity),0) FROM WAREHOUSE") + " Kg";
                        val2 = AdminController.getScalarValue("SELECT COUNT(*) FROM FAIR_PRICE_SHOP") + " Outlets";
                        grs = AdminController.executeGenericQuery("SELECT COALESCE(Capacity, 5000) FROM WAREHOUSE LIMIT 7");
                        for (Object[] r : grs) {
                            if (r[0] != null) {
                                float capacityVal = Float.parseFloat(r[0].toString());
                                tempGraphData.add(Math.min(95, Math.max(15, (int)(capacityVal / 25000.0 * 100))));
                            }
                        }
                    }
                    while (tempGraphData.size() < 7) {
                        tempGraphData.add(30 + (tempGraphData.size() * 10));
                    }
                    
                    final java.util.List<Object[]> finalRows = rows;
                    final int finalTotal = totalRowsCalculated;
                    final String finalVal1 = val1;
                    final String finalVal2 = val2;
                    final java.util.List<Integer> finalGraphData = tempGraphData;

                    SwingUtilities.invokeLater(() -> {
                        model.setRowCount(0);
                        liveGraphData.clear();
                        liveGraphData.addAll(finalGraphData);
                        
                        if (finalRows != null) {
                            for (Object[] row : finalRows) {
                                model.addRow(row);
                            }
                        }
                        
                        if ("Categories".equals(title)) {
                            metricLabels[0].setText(String.valueOf(finalTotal));
                            metricLabels[1].setText(finalVal1);
                            metricLabels[2].setText("Direct Chain");
                            metricLabels[3].setText("Verified");
                        } else if ("Commodities".equals(title)) {
                            metricLabels[0].setText(String.valueOf(finalTotal));
                            metricLabels[1].setText("Kg / Ltr");
                            metricLabels[2].setText("Secure");
                            metricLabels[3].setText("Disabled");
                        } else if ("Suppliers".equals(title)) {
                            metricLabels[0].setText(String.valueOf(finalTotal));
                            metricLabels[1].setText("98.4%");
                            metricLabels[2].setText("Direct");
                            metricLabels[3].setText("None");
                        } else if ("Warehouses".equals(title)) {
                            metricLabels[0].setText(String.valueOf(finalTotal));
                            metricLabels[1].setText(finalVal1);
                            metricLabels[2].setText(finalVal2);
                            metricLabels[3].setText("Healthy");
                        }
                        outerPanel.repaint();
                    });
                } catch (SQLException ex) { ex.printStackTrace(); }
            });
        };

tfSearch.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                String term = tfSearch.getText().trim().toLowerCase();
                // FIXED LINE: Changed defaultModel to model
                TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
                tbl.setRowSorter(sorter);
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + term));
            }
        });

JScrollPane tableScroll = PDSApp.tableScroll(tbl);
leftColumn.add(searchToolbar, BorderLayout.NORTH);
leftColumn.add(tableScroll, BorderLayout.CENTER);

// RIGHT COLUMN: High-Contrast Manifest Forms & Live Telemetry Card
JPanel rightColumn = new JPanel(new GridBagLayout());
rightColumn.setBackground(PAGE);

JPanel inputFormCard = PDSApp.card();
inputFormCard.setLayout(new GridBagLayout());
inputFormCard.setBorder(new EmptyBorder(20, 24, 24, 24));

GridBagConstraints fGbc = new GridBagConstraints();
fGbc.fill = GridBagConstraints.HORIZONTAL;
fGbc.gridx = 0;
fGbc.weightx = 1.0;
fGbc.gridy = 0;
fGbc.insets = new Insets(0, 0, 12, 0);

JLabel sectionLbl = PDSApp.sectionLabel("System Manifest Control");
inputFormCard.add(sectionLbl, fGbc);

JTextField[] fields = new JTextField[fieldLabels.length];
for (int i = 0; i < fieldLabels.length; i++) {
fields[i] = PDSApp.inputField(14);
// CRITICAL FIX: Explicit preferred and minimum sizing forces input visibility
fields[i].setPreferredSize(new Dimension(100, 38));
fields[i].setMinimumSize(new Dimension(100, 38));

fGbc.gridy++;
fGbc.insets = new Insets(0, 0, 4, 0);
inputFormCard.add(PDSApp.formLabel(fieldLabels[i]), fGbc);

fGbc.gridy++;
fGbc.insets = new Insets(0, 0, 12, 0);
inputFormCard.add(fields[i], fGbc);
}

JPanel rowBtnLayout = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
rowBtnLayout.setBackground(PDSApp.CARD_BG);

if (onAdd != null) {
JButton btnAdd = PDSApp.primaryBtn("Commit Add");
btnAdd.addActionListener(e -> {
String[] values = new String[fields.length];
for (int i = 0; i < fields.length; i++) values[i] = fields[i].getText().trim();
for (String v : values) if (v.isEmpty()) { PDSApp.showAlert(outerPanel, "Fill all data cells.", true); return; }
try {
onAdd.run(values);
for (JTextField f : fields) f.setText("");
loadEngine.run();
} catch (Exception ex) { PDSApp.showAlert(outerPanel, ex.getMessage(), true); }
});
rowBtnLayout.add(btnAdd);
}
if (onDel != null) {
JButton btnDel = PDSApp.dangerBtn("Prune Row");
btnDel.addActionListener(e -> {
int selectedRow = tbl.getSelectedRow();
if (selectedRow < 0) { PDSApp.showAlert(outerPanel, "Select target record node first.", true); return; }
try {
int modelRow = tbl.convertRowIndexToModel(selectedRow);
onDel.run(model, modelRow);
loadEngine.run();
} catch (Exception ex) { PDSApp.showAlert(outerPanel, ex.getMessage(), true); }
});
rowBtnLayout.add(btnDel);
}

fGbc.gridy++;
fGbc.insets = new Insets(8, 0, 0, 0);
inputFormCard.add(rowBtnLayout, fGbc);

// Smooth Real-Time Data Graph Widget Animation
JPanel illustrationCard = new JPanel() {
private float scanPhase = 0f;
private final Timer animTimer = new Timer(30, e -> {
scanPhase += 0.015f;
if (scanPhase > 1.2f) scanPhase = -0.1f;
repaint();
});
{ animTimer.start(); }

@Override protected void paintComponent(Graphics g) {
Graphics2D g2 = (Graphics2D) g.create();
g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

g2.setColor(PDSApp.CARD_BG);
g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
g2.setColor(PDSApp.BORDER_CLR);
g2.setStroke(new BasicStroke(1.2f));
g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 16, 16));

int padX = 24, padY = 24;
int w = getWidth() - (padX * 2);
int h = getHeight() - (padY * 2) - 15;
int baseY = getHeight() - padY;

g2.setStroke(new BasicStroke(1f));
g2.setColor(new Color(0xF1F5F9));
for (int i = 1; i <= 3; i++) {
int gy = baseY - (h * i / 3);
g2.drawLine(padX, gy, padX + w, gy);
}

int points = Math.min(7, liveGraphData.size());
if (points > 1) {
Path2D poly = new Path2D.Float();
Path2D line = new Path2D.Float();
poly.moveTo(padX, baseY);
for (int i = 0; i < points; i++) {
int vx = padX + (w * i / (points - 1));
int vy = baseY - (int)(h * liveGraphData.get(i) / 100.0);
if (i == 0) { line.moveTo(vx, vy); } 
else {
int prevX = padX + (w * (i - 1) / (points - 1));
int prevY = baseY - (int)(h * liveGraphData.get(i - 1) / 100.0);
line.curveTo(prevX + 20, prevY, vx - 20, vy, vx, vy);
}
poly.curveTo(padX + (w * i / (points - 1)) - 20, baseY - (int)(h * (i == 0 ? liveGraphData.get(0) : liveGraphData.get(i-1)) / 100.0), vx - 20, vy, vx, vy);
}
poly.lineTo(padX + w, baseY);
poly.closePath();

Color themeColor = metricColors[0];
g2.setPaint(new GradientPaint(0, baseY - h, new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 45), 0, baseY, new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 0)));
g2.fill(poly);
g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
g2.setColor(themeColor);
g2.draw(line);

int scanX = padX + (int)(w * scanPhase);
if (scanX >= padX && scanX <= padX + w) {
g2.setPaint(new GradientPaint(scanX - 15, 0, new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 0), scanX, 0, new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 45)));
g2.fillRect(scanX - 15, padY, 15, h);
g2.setColor(themeColor); g2.setStroke(new BasicStroke(1.5f)); g2.drawLine(scanX, padY, scanX, baseY);
}
}
g2.dispose();
}
};
illustrationCard.setOpaque(false);
illustrationCard.setPreferredSize(new Dimension(0, 140));
illustrationCard.setMinimumSize(new Dimension(0, 140));
        illustrationCard.setLayout(new BorderLayout());
        JLabel lblIllustrationText = new JLabel("<html><center><b style='color:#FAFAFA; font-size:11px;'>" + illustrationLabel + "</b><br>"
        + "<span style='color:#16A34A; font-size:10px;'>● Live Production Ledger Waveform Active</span></center></html>", SwingConstants.CENTER);
        illustrationCard.setBorder(new EmptyBorder(12, 12, 0, 12));
        illustrationCard.add(lblIllustrationText, BorderLayout.NORTH);
        
        GridBagConstraints rGbc = new GridBagConstraints();
        rGbc.fill = GridBagConstraints.HORIZONTAL;
        rGbc.weightx = 1.0; rGbc.gridx = 0;
        rGbc.gridy = 0; rGbc.weighty = 0.0;
        rightColumn.add(inputFormCard, rGbc);
        
        rGbc.gridy = 1; rGbc.weighty = 0.0;
        rGbc.insets = new Insets(14, 0, 0, 0);
        rGbc.fill = GridBagConstraints.HORIZONTAL;
        rightColumn.add(illustrationCard, rGbc);
        
        rGbc.gridy = 2; rGbc.weighty = 1.0;
        rGbc.insets = new Insets(14, 0, 0, 0);
        rGbc.fill = GridBagConstraints.BOTH;
        rightColumn.add(Box.createVerticalGlue(), rGbc);

gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
gbc.gridx = 0; gbc.weightx = 0.62; gbc.insets = new Insets(0, 0, 0, 0);
contentWrapper.add(leftColumn, gbc);
gbc.gridx = 1; gbc.weightx = 0.38; gbc.insets = new Insets(0, 20, 0, 0);
contentWrapper.add(rightColumn, gbc);

JPanel mainStack = new JPanel(new GridBagLayout());
mainStack.setBackground(PAGE);
GridBagConstraints mGbc = new GridBagConstraints();
mGbc.fill = GridBagConstraints.HORIZONTAL;
mGbc.gridx = 0; mGbc.weightx = 1.0;
mGbc.gridy = 0; mainStack.add(viewHeader, mGbc);
mGbc.gridy = 1; mainStack.add(kpiGrid, mGbc);
mGbc.gridy = 2; mGbc.weighty = 1.0; mGbc.fill = GridBagConstraints.BOTH; mGbc.insets = new Insets(20, 0, 0, 0);
mainStack.add(contentWrapper, mGbc);

outerPanel.add(mainStack, BorderLayout.CENTER);
btnRefDeck.addActionListener(e -> { tfSearch.setText(""); loadEngine.run(); });
loadEngine.run();
return outerPanel;
}

    // ── Layout helpers ────────────────────────────────────────────────
    private static JPanel row(Color bg) {
        JPanel r = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        r.setBackground(bg);
        return r;
    }

    private static JPanel col(Color bg) {
        JPanel c = new JPanel();
        c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
        c.setBackground(bg);
        return c;
    }

    private void showChangePasswordDialog() {
        JDialog diag = new JDialog(this, "Security Access Credentials Setup", true); diag.setSize(400, 310); diag.setLocationRelativeTo(this);
        JPanel p = new JPanel(new GridBagLayout()); p.setBackground(PDSApp.CARD_BG); p.setBorder(new EmptyBorder(20, 24, 20, 24));
        GridBagConstraints gbc = new GridBagConstraints(); gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0; gbc.gridx = 0; gbc.gridy = 0;
        JLabel title = PDSApp.sectionLabel("Security Account Settings"); gbc.insets = new Insets(0, 0, 14, 0); p.add(title, gbc);
        JPasswordField pfCurrent = new JPasswordField(); JPasswordField pfNew = new JPasswordField();
        pfCurrent.putClientProperty("FlatLaf.style", "arc:8; borderWidth:1; focusWidth:2;"); pfNew.putClientProperty("FlatLaf.style", "arc:8; borderWidth:1; focusWidth:2;");
        pfCurrent.setPreferredSize(new Dimension(100, 38)); pfCurrent.setMinimumSize(new Dimension(100, 38));
        pfNew.setPreferredSize(new Dimension(100, 38)); pfNew.setMinimumSize(new Dimension(100, 38));
        gbc.gridy++; gbc.insets = new Insets(0, 0, 4, 0); p.add(PDSApp.formLabel("Current Access Password"), gbc);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 12, 0); p.add(pfCurrent, gbc);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 4, 0); p.add(PDSApp.formLabel("New Account Security Code"), gbc);
        gbc.gridy++; gbc.insets = new Insets(0, 0, 18, 0); p.add(pfNew, gbc);
        JButton btnCommit = PDSApp.primaryBtn("Update Secure Credentials"); gbc.gridy++; gbc.insets = new Insets(4, 0, 0, 0); p.add(btnCommit, gbc);
        btnCommit.addActionListener(e -> {
            String currStr = new String(pfCurrent.getPassword()); String newStr = new String(pfNew.getPassword());
            if(currStr.isEmpty() || newStr.isEmpty()) { PDSApp.showAlert(p, "Fill all data blocks.", true); return; }
            try {
                boolean success = ShopController.updateCredentials(username, currStr, newStr);
                if (!success) {
                    PDSApp.showAlert(p, "Verification failure: Mismatched credentials.", true); 
                    return;
                }
                JOptionPane.showMessageDialog(diag, "System entry credentials updated securely.", "Security Status", JOptionPane.INFORMATION_MESSAGE); 
                diag.dispose();
            } catch (SQLException ex) { PDSApp.showAlert(p, ex.getMessage(), true); } });
        diag.setContentPane(p); diag.setVisible(true);
    }

    interface AddAction {

        void run(String[] vals) throws Exception;
    }

    interface DelAction {

        void run(DefaultTableModel m, int row) throws Exception;
    }
}
