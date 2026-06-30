import com.formdev.flatlaf.FlatClientProperties;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.sql.*;

public class ShopFrame extends JFrame {

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

    private static final Color PAGE = PDSApp.PAGE_BG;

    private final int    shopId;
    private final String username;
    private CardLayout cards   = new CardLayout();
    private JPanel     content = new JPanel(cards);
    private PDSApp.AnimatedSidebarButton[] navBtns;
    private JLabel     breadcrumb = new JLabel();
    private int        currentNav = -1;

    private String[] navCards  = {"DASHBOARD", "STOCK", "REQUESTS", "BENEFICIARIES", "ABOUT", "ORDERS", "DISTRIBUTE", "PASSWORD"};
    private String[] navLabels = {"Dashboard", "Stock Inventory", "Requests", "Beneficiaries", "Shop Info", "Transactions", "Distribution", "Change Password"};
    private String[] navIconPaths = {
        "/resources/icons/overview.png",
        "/resources/icons/shops.png",
        "/resources/icons/rules.png",
        "/resources/icons/beneficiaries.png",
        "/resources/icons/overview.png",
        "/resources/icons/audit.png",
        "/resources/icons/categories.png",
        "/resources/icons/categories.png"
    };

    public ShopFrame(String username, int shopId) {
        super("PDS — Shop Operator  |  " + username);
        this.shopId = shopId; this.username = username;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1340, 860);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null); setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);
        JPanel main = new JPanel(new BorderLayout());
        main.add(buildTopBar(),  BorderLayout.NORTH);
        main.add(buildContent(), BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);
        activateNav(0); setVisible(true);
        SwingUtilities.invokeLater(this::animateSidebarIn);
    }

    // ── Sidebar (100% Consistent with AdminFrame) ─────────────────────
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
        } catch (Exception e) {}

        JPanel brandText = new JPanel();
        brandText.setOpaque(false);
        brandText.setLayout(new BoxLayout(brandText, BoxLayout.Y_AXIS));

        JLabel logo = new JLabel("PDS Portal");
        logo.setFont(new Font(FAM, Font.BOLD, 17));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(LEFT_ALIGNMENT);
        JLabel sub = new JLabel("Fair Price Shop Outlet");
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

        side.add(sideLabel("SHOP OPERATIONS"));
        navBtns = new PDSApp.AnimatedSidebarButton[navCards.length];
        String[] groups = {"SHOP OPERATIONS", "SHOP OPERATIONS", "SHOP OPERATIONS", "SHOP OPERATIONS", "SHOP OPERATIONS", "REPORTS", "REPORTS", "REPORTS"};
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
            } catch (Exception ex) {}
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

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, PDSApp.SIDEBAR_SEC), new EmptyBorder(14, 16, 14, 16)));
        footer.setMaximumSize(new Dimension(240, 52));
        footer.setAlignmentX(LEFT_ALIGNMENT);
        PDSApp.RippleButton btnLogout = new PDSApp.RippleButton("  ⤢  Sign Out", PDSApp.SIDEBAR_BG, PDSApp.SIDEBAR_HOV, PDSApp.SIDEBAR_LBL);
        btnLogout.setFont(new Font(FAM, Font.BOLD, 12));
        btnLogout.setHorizontalAlignment(SwingConstants.LEFT);
        btnLogout.setMaximumSize(new Dimension(240, 52));
        btnLogout.setPreferredSize(new Dimension(240, 52));
        btnLogout.setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, PDSApp.SIDEBAR_SEC), new EmptyBorder(0, 0, 0, 0)));
        btnLogout.addActionListener(e -> { dispose(); new LoginFrame(); });
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
        if (currentNav == idx) return;
        currentNav = idx;
        for (int i = 0; i < navBtns.length; i++) navBtns[i].setActive(i == idx);
        cards.show(content, navCards[idx]);
        breadcrumb.setText("Shop  ›  " + navLabels[idx]);
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

    // ── Top Bar (100% Consistent with AdminFrame) ──────────────────────
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
        left.add(ico); left.add(breadcrumb);
        bar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        JLabel role = new JLabel("Shop Operator");
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
        right.add(role); right.add(av);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildContent() {
        content.setBackground(PAGE);
        content.add(buildDashboard(),    "DASHBOARD");
        content.add(buildStock(),        "STOCK");
        content.add(buildRequests(),     "REQUESTS");
        content.add(buildBeneficiaries(),"BENEFICIARIES");
        content.add(buildAboutPanel(),   "ABOUT");
        content.add(buildOrdersPanel(),  "ORDERS");
        content.add(new DistributePanel(),"DISTRIBUTE");
        content.add(buildPassword(),     "PASSWORD");
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

    private JPanel buildHeroBanner(JLabel lBen, JLabel lPend, JLabel lFulfilled, JLabel lDist, Runnable onRefresh) {
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
                    java.io.File f = new java.io.File("resources/shop_hero_banner.png");
                    if (!f.exists()) {
                        f = new java.io.File("resources/shopherobanner.png");
                    }
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

                // Draw illustration with dynamic floating/bobbing effect in background
                double bobOffset = Math.sin(dynamicFlowPhase * 1.5) * 8;
                if (img != null) {
                    int w = getWidth(), h = getHeight();
                    int ih = img.getHeight(null);
                    int iw = img.getWidth(null);
                    
                    double scale = (double) (h * 0.98) / ih;
                    int nw = (int) (iw * scale);
                    int nh = (int) (ih * scale);
                    
                    g2.drawImage(img, w - nw - 16, (h - nh) / 2 + 15 + (int) bobOffset, nw, nh, null);
                } else {
                    // Fallback visual illustration
                    int w = getWidth(), h = getHeight();
                    int cx = w - 180;
                    int cy = h / 2 + 10 + (int) bobOffset;
                    int groundY = cy + 30;
                    g2.setColor(new Color(0x384959));
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawLine(cx - 80, groundY, cx + 80, groundY);
                    
                    g2.setColor(new Color(0x78350F));
                    g2.fillRect(cx - 50, groundY - 30, 5, 30);
                    g2.setColor(new Color(0x15803D));
                    g2.fillOval(cx - 65, groundY - 50, 30, 30);
                    
                    int sx = cx - 10, sy = groundY - 50, sw = 70, sh = 50;
                    g2.setColor(Color.WHITE);
                    g2.fillRect(sx, sy, sw, sh);
                    g2.setColor(new Color(0x384959));
                    g2.drawRect(sx, sy, sw, sh);
                    g2.setColor(new Color(0x475569));
                    g2.fillRect(sx + 10, sy + 18, 15, 32);
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

        // ── LEFT COLUMN (68% width for texts and stacked KPI cards) ──
        JPanel leftCol = new JPanel(new GridBagLayout());
        leftCol.setOpaque(false);
        GridBagConstraints leftGbc = new GridBagConstraints();
        leftGbc.gridx = 0; leftGbc.gridy = 0;
        leftGbc.weightx = 1.0; leftGbc.anchor = GridBagConstraints.WEST;
        leftGbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel sysLbl = new JLabel("FPS DISTRIBUTION MANAGEMENT SYSTEM");
        sysLbl.setFont(new Font(FAM, Font.BOLD, 13));
        sysLbl.setForeground(PDSApp.ACCENT);
        leftCol.add(sysLbl, leftGbc);

        leftGbc.gridy++;
        leftGbc.insets = new Insets(4, 0, 0, 0);
        JLabel title = new JLabel("Shop Outlet Dashboard");
        title.setFont(PDSApp.FONT_H1.deriveFont(Font.BOLD, 30f));
        title.setForeground(PDSApp.TEXT_PRIMARY);
        leftCol.add(title, leftGbc);

        leftGbc.gridy++;
        leftGbc.insets = new Insets(2, 0, 0, 0);
        JLabel sub = new JLabel("Manage stock, fulfill requests and serve beneficiaries");
        sub.setFont(PDSApp.FONT_LABEL.deriveFont(Font.PLAIN, 16f));
        sub.setForeground(PDSApp.TEXT_SEC);
        leftCol.add(sub, leftGbc);

        leftGbc.gridy++;
        leftGbc.insets = new Insets(8, 0, 0, 0);

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

        // Add 4 large KPI cards below the sync row inside leftCol
        leftGbc.gridy++;
        leftGbc.insets = new Insets(12, 0, 0, 0);
        leftGbc.weighty = 1.0;
        leftGbc.fill = GridBagConstraints.HORIZONTAL;
        
        JPanel cardsRow = new JPanel(new GridLayout(1, 4, 16, 0));
        cardsRow.setOpaque(false);

        JPanel card1 = PDSApp.statCard("Assigned Beneficiaries", lBen, PDSApp.ACCENT);
        JPanel card2 = PDSApp.statCard("Pending Requests", lPend, PDSApp.WARNING);
        JPanel card3 = PDSApp.statCard("Fulfilled Requests", lFulfilled, PDSApp.SUCCESS);
        JPanel card4 = PDSApp.statCard("Total Distributions", lDist, PDSApp.ACCENT);

        card1.setPreferredSize(new Dimension(0, 120));
        card2.setPreferredSize(new Dimension(0, 120));
        card3.setPreferredSize(new Dimension(0, 120));
        card4.setPreferredSize(new Dimension(0, 120));

        card1.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card1.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { activateNav(3); } // Assigned Beneficiaries
        });
        card2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card2.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { activateNav(2); } // Incoming Requests
        });
        card3.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card3.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { activateNav(5); } // Transaction History
        });
        card4.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card4.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { activateNav(6); } // Issue Distribution
        });

        cardsRow.add(card1);
        cardsRow.add(card2);
        cardsRow.add(card3);
        cardsRow.add(card4);
        leftCol.add(cardsRow, leftGbc);

        gbc.gridx = 0; gbc.weightx = 0.68;
        gbc.insets = new Insets(0, 0, 0, 0);
        banner.add(leftCol, gbc);

        // ── RIGHT COLUMN (32% width filler to reserve space for background illustration) ──
        JPanel rightFiller = new JPanel();
        rightFiller.setOpaque(false);
        gbc.gridx = 1; gbc.weightx = 0.32;
        banner.add(rightFiller, gbc);

        return banner;
    }

    private JPanel buildDashboard() {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(PAGE);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(PAGE);
        p.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel lBen = new JLabel("0"), lPend = new JLabel("0"), lFulfilled = new JLabel("0"), lDist = new JLabel("0");
        JLabel lLow = new JLabel("0"), lTotal = new JLabel("0");

        final Runnable[] loadEngineBox = new Runnable[1];
        JPanel hero = buildHeroBanner(lBen, lPend, lFulfilled, lDist, () -> { if (loadEngineBox[0] != null) loadEngineBox[0].run(); });
        hero.setAlignmentX(LEFT_ALIGNMENT);

        final java.util.List<Object[]> liveChartData = new java.util.ArrayList<>();
        JPanel liveTelemetryCard = new JPanel() {
            private float barSweepScanLine = 0f;
            private float pulseGlowPhase = 0f;
            private final Timer barChartTimer = new Timer(30, e -> {
                barSweepScanLine += 0.012f; pulseGlowPhase += 0.04f;
                if (barSweepScanLine > 1.2f) barSweepScanLine = -0.1f;
                repaint();
            });
            { barChartTimer.start(); }

            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PDSApp.CARD_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));

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
                    g2.drawString("No stock data available in DB", getWidth() / 2 - 100, getHeight() / 2);
                    g2.dispose();
                    return;
                }

                int maxVal = 1;
                for (Object[] row : liveChartData) {
                    int val = (int) row[1];
                    if (val > maxVal) maxVal = val;
                }

                double scaleMax = maxVal * 1.15; // 15% headroom top padding above bars

                g2.setFont(new Font(FAM, Font.PLAIN, 10));
                for (int i = 0; i <= 3; i++) {
                    int gy = baseFloorY - (graphH * i / 3);
                    g2.setStroke(new BasicStroke(1f)); 
                    g2.setColor(new Color(0x27272A)); 
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

                Color coreCol = PDSApp.SUCCESS;

                int barWidth = (graphW / totalBars) - 20;
                if (barWidth < 8) barWidth = 8;
                
                for (int i = 0; i < totalBars; i++) {
                    Object[] row = liveChartData.get(i);
                    String comName = (String) row[0];
                    int qty = (int) row[1];
                    
                    int barCalculatedHeight = (int)(graphH * qty / scaleMax);
                    int bx = padLeft + 10 + (graphW * i / totalBars); 
                    int by = baseFloorY - barCalculatedHeight;
                    g2.setColor(new Color(coreCol.getRed(), coreCol.getGreen(), coreCol.getBlue(), 12));
                    g2.fill(new RoundRectangle2D.Float(bx - 2, by - 2, barWidth + 4, barCalculatedHeight + 2, 6, 6));

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

                int sweepLineX = padLeft + (int)(graphW * barSweepScanLine);
                if (sweepLineX >= padLeft && sweepLineX <= padLeft + graphW) {
                    g2.setPaint(new GradientPaint(sweepLineX - 40, 0, new Color(coreCol.getRed(), coreCol.getGreen(), coreCol.getBlue(), 0), sweepLineX, 0, new Color(coreCol.getRed(), coreCol.getGreen(), coreCol.getBlue(), 25)));
                    g2.fillRect(sweepLineX - 40, padTop, 40, graphH);
                    g2.setColor(new Color(coreCol.getRed(), coreCol.getGreen(), coreCol.getBlue(), 80));
                    g2.drawLine(sweepLineX, padTop, sweepLineX, baseFloorY);
                }
                g2.dispose();
            }
        };
        liveTelemetryCard.setOpaque(false); liveTelemetryCard.setLayout(new BorderLayout()); liveTelemetryCard.setBorder(new EmptyBorder(16, 22, 16, 22));
        liveTelemetryCard.setPreferredSize(new Dimension(0, 380));
        JLabel lblTelemetryText = new JLabel("<html><b style='color:#1F2937; font-size:12px;'>Stock Availability <span style=\"color:#6B7280; font-weight:normal;\">(MT)</span></b><br><span style='color:#6B7280; font-size:10px;'>&#9679; Live local stock inventory quantities tracked automatically</span></html>");
        lblTelemetryText.setBorder(new EmptyBorder(0, 0, 18, 0)); liveTelemetryCard.add(lblTelemetryText, BorderLayout.NORTH);

        JPanel secondaryMetricsGrid = new JPanel(new GridBagLayout());
        secondaryMetricsGrid.setBackground(PAGE);
        GridBagConstraints sgbc = new GridBagConstraints();
        sgbc.fill = GridBagConstraints.BOTH;
        sgbc.gridx = 0; sgbc.weightx = 1.0;

        // Top 4 metric cards in 2x2
        JPanel metricsGrid = new JPanel(new GridLayout(2, 2, 12, 12));
        metricsGrid.setBackground(PAGE);
        metricsGrid.add(PDSApp.statCard("Low Stock Items", lLow, PDSApp.DANGER));
        metricsGrid.add(PDSApp.statCard("Managed SKUs", lTotal, PDSApp.ACCENT));
        JLabel lTransCount = new JLabel("—");
        JLabel lBenCount = new JLabel("—");
        metricsGrid.add(PDSApp.statCard("Today's Issues", lTransCount, PDSApp.SUCCESS));
        metricsGrid.add(PDSApp.statCard("Active Beneficiaries", lBenCount, PDSApp.ACCENT));
        sgbc.gridy = 0; sgbc.weighty = 1.0; sgbc.insets = new Insets(0, 0, 0, 0);
        secondaryMetricsGrid.add(metricsGrid, sgbc);

        sgbc.gridy = 1; sgbc.weighty = 0.0; sgbc.insets = new Insets(12, 0, 0, 0);
        JPanel recentCard = new JPanel(new BorderLayout(0, 6)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PDSApp.CARD_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.setColor(PDSApp.BORDER_CLR);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-1, getHeight()-1, 16, 16));
                g2.dispose();
            }
        };
        recentCard.setOpaque(false);
        recentCard.setBorder(new EmptyBorder(8, 16, 8, 16));
        recentCard.setPreferredSize(new Dimension(0, 110));
        recentCard.setMinimumSize(new Dimension(0, 110));
        JPanel rcTop = new JPanel(new BorderLayout());
        rcTop.setOpaque(false);
        JLabel rcTitle = new JLabel("Recent Activity");
        rcTitle.setFont(PDSApp.FONT_H2);
        rcTitle.setForeground(PDSApp.TEXT_PRIMARY);
        JLabel rcViewAll = new JLabel("<html><a href='' style='color:#5B8FD9;'>View all</a></html>");
        rcViewAll.setFont(PDSApp.FONT_SMALL);
        rcViewAll.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        rcTop.add(rcTitle, BorderLayout.WEST);
        rcTop.add(rcViewAll, BorderLayout.EAST);
        recentCard.add(rcTop, BorderLayout.NORTH);
        String[] recentItems = {"Distributed 5 ration(s)", "Stock updated RICE", "New request received"};
        String[] recentTimes = {"10:38 AM", "10:10 AM", "09:45 AM"};
        JPanel rcList = new JPanel();
        rcList.setLayout(new BoxLayout(rcList, BoxLayout.Y_AXIS));
        rcList.setOpaque(false);
        for (int ri = 0; ri < recentItems.length; ri++) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            row.setBorder(new EmptyBorder(2, 0, 2, 0));
            JLabel dot = new JLabel("●");
            dot.setFont(new Font(FAM, Font.PLAIN, 8));
            dot.setForeground(PDSApp.ACCENT);
            JLabel txt = new JLabel("<html><span style='color:#1F2937;font-size:11px;'>" + recentItems[ri] + "</span></html>");
            txt.setFont(PDSApp.FONT_SMALL);
            JLabel time = new JLabel(recentTimes[ri]);
            time.setFont(new Font(FAM, Font.PLAIN, 10));
            time.setForeground(PDSApp.TEXT_MUTED);
            row.add(dot, BorderLayout.WEST);
            row.add(txt, BorderLayout.CENTER);
            row.add(time, BorderLayout.EAST);
            rcList.add(row);
        }
        recentCard.add(rcList, BorderLayout.CENTER);
        secondaryMetricsGrid.add(recentCard, sgbc);

        GridBagConstraints workspaceGbc = new GridBagConstraints();
        workspaceGbc.fill = GridBagConstraints.HORIZONTAL; workspaceGbc.gridx = 0; workspaceGbc.weightx = 1.0;
        workspaceGbc.gridy = 0; workspaceGbc.insets = new Insets(0, 0, 24, 0); p.add(hero, workspaceGbc);

        JPanel bottomSplitRowPanel = new JPanel(new GridBagLayout()); bottomSplitRowPanel.setBackground(PAGE);
        GridBagConstraints bGbc = new GridBagConstraints(); bGbc.fill = GridBagConstraints.BOTH; bGbc.weighty = 1.0;
        bGbc.gridx = 0; bGbc.weightx = 0.70; bGbc.insets = new Insets(0, 0, 0, 0); bottomSplitRowPanel.add(liveTelemetryCard, bGbc);
        bGbc.gridx = 1; bGbc.weightx = 0.30; bGbc.insets = new Insets(0, 18, 0, 0); bottomSplitRowPanel.add(secondaryMetricsGrid, bGbc);

        workspaceGbc.gridy = 1; workspaceGbc.weighty = 1.0; workspaceGbc.fill = GridBagConstraints.BOTH; workspaceGbc.insets = new Insets(0, 0, 0, 0); p.add(bottomSplitRowPanel, workspaceGbc);

        Runnable loadEngine = () -> {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    ShopController.SummaryMetrics m = ShopController.getSummaryMetrics(shopId);

                    String todayIssues = "0";
                    try (PreparedStatement ps = DB.getConnection().prepareStatement(
                            "SELECT COUNT(*) FROM DISTRIBUTION_TRANSACTION dt " +
                            "JOIN REQUEST r ON r.RequestID=dt.RequestID " +
                            "WHERE r.ShopID=? AND r.RequestDate=CURDATE()")) {
                        ps.setInt(1, shopId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                todayIssues = rs.getString(1);
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    java.util.List<Object[]> stats = AnalyticsController.getShopStockStats(shopId);
                    final String finalTodayIssues = todayIssues;

                    SwingUtilities.invokeLater(() -> {
                        lBen.setText(m.totalBeneficiaries);
                        lPend.setText(m.pendingRequests);
                        lFulfilled.setText(m.fulfilledRequests);
                        lDist.setText(m.totalTransactions);
                        lLow.setText(m.lowStockItems);
                        lTotal.setText(m.totalStockItems);

                        lTransCount.setText(finalTodayIssues);
                        lBenCount.setText(m.totalBeneficiaries);

                        liveChartData.clear();
                        liveChartData.addAll(stats);
                        liveTelemetryCard.repaint();
                    });
                } catch (SQLException ex) { ex.printStackTrace(); }
            });
        };

        loadEngineBox[0] = loadEngine;
        loadEngine.run();

        JScrollPane mainScroll = new JScrollPane(p); mainScroll.setBorder(null); mainScroll.getVerticalScrollBar().setUnitIncrement(16);
        container.add(mainScroll, BorderLayout.CENTER);
        return container;
    }

    // ── Stock Management (62-38 Advanced Split Workspace Layout with Live Waveform Animation) ──
    private JPanel buildStock() {
        JPanel p = new JPanel(new GridBagLayout()); p.setBackground(PAGE); p.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints gbc = new GridBagConstraints(); gbc.fill = GridBagConstraints.BOTH;

        JTable tbl = PDSApp.styledTable(new String[]{"Stock ID", "Commodity Item Scope", "Unit Registry", "Quantity Available", "System Status"});
        DefaultTableModel model = (DefaultTableModel) tbl.getModel();
        tbl.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                String val = (v != null) ? v.toString() : "";
                boolean low = "LOW".equals(val);
                JLabel pill = PDSApp.statusPill("  " + val + "  ", 
                    low ? PDSApp.DANGER : PDSApp.SUCCESS, 
                    low ? PDSApp.DANGER_BG : PDSApp.SUCCESS_BG);
                if (sel) {
                    pill.setBackground(PDSApp.ACCENT_BG);
                    pill.setForeground(PDSApp.ACCENT);
                }
                return pill;
            }
        });

        // Metrics Labels
        JLabel lShopTotalStock = new JLabel("0 Kg");
        lShopTotalStock.setFont(new Font(FAM, Font.BOLD, 18));
        lShopTotalStock.setForeground(PDSApp.TEXT_PRIMARY);

        JLabel lLowStockCount = new JLabel("0");
        lLowStockCount.setFont(new Font(FAM, Font.BOLD, 18));
        lLowStockCount.setForeground(PDSApp.DANGER);

        JLabel lDepotSupplyStock = new JLabel("0 Kg");
        lDepotSupplyStock.setFont(new Font(FAM, Font.BOLD, 18));
        lDepotSupplyStock.setForeground(PDSApp.SUCCESS);

        JLabel lSkuCount = new JLabel("0");
        lSkuCount.setFont(new Font(FAM, Font.BOLD, 18));
        lSkuCount.setForeground(PDSApp.INFO);

        JPanel metricsGrid = new JPanel(new GridLayout(2, 2, 10, 10));
        metricsGrid.setBackground(PAGE);
        metricsGrid.add(PDSApp.statCard("Local Stock", lShopTotalStock, PDSApp.ACCENT));
        metricsGrid.add(PDSApp.statCard("Low Stock", lLowStockCount, PDSApp.DANGER));
        metricsGrid.add(PDSApp.statCard("Depot Stock", lDepotSupplyStock, PDSApp.SUCCESS));
        metricsGrid.add(PDSApp.statCard("Active SKUs", lSkuCount, PDSApp.INFO));

        final java.util.List<Integer> liveWaveData = new java.util.ArrayList<>();
        JSpinner spQty = PDSApp.styledSpinner(0, 99999);
        JButton btnUpdate = PDSApp.primaryBtn("Commit Log Metric Adjustments");
        JButton btnRefresh = PDSApp.secondaryBtn("⟳  Refresh Roster");
        
        JLabel lblWhInfo = PDSApp.formLabel("Supply Depot: Loading...");
        JLabel lblWhStock = PDSApp.formLabel("Available Pool: Loading...");
        JButton btnOrder = PDSApp.primaryBtn("Request Warehouse Supply");

        Runnable load = () -> {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    java.util.List<Object[]> roster = ShopController.getStockRoster(shopId);
                    ShopController.OutletInfo info = ShopController.getOutletInfo(shopId);
                    
                    int shopTotalTemp = 0;
                    int lowCountTemp = 0;
                    java.util.List<Integer> waveDataTemp = new java.util.ArrayList<>();
                    for (Object[] row : roster) {
                        int qty = (int) row[3];
                        String status = (String) row[4];
                        shopTotalTemp += qty;
                        if ("LOW".equals(status)) {
                            lowCountTemp++;
                        }
                        waveDataTemp.add(Math.min(95, Math.max(15, (int)(qty / 5.0))));
                    }
                    while (waveDataTemp.size() < 7) { waveDataTemp.add(40 + (waveDataTemp.size() * 8)); }
                    
                    final int finalShopTotal = shopTotalTemp;
                    final int finalLowCount = lowCountTemp;
                    final int finalSkuCount = roster.size();
                    
                    SwingUtilities.invokeLater(() -> {
                        model.setRowCount(0);
                        liveWaveData.clear();
                        for (Object[] row : roster) {
                            model.addRow(row);
                        }
                        liveWaveData.addAll(waveDataTemp);
                        
                        lShopTotalStock.setText(String.format("%,d Kg", finalShopTotal));
                        lLowStockCount.setText(String.valueOf(finalLowCount));
                        lSkuCount.setText(String.valueOf(finalSkuCount));

                        if (info != null) {
                            lblWhInfo.setText("Supply Depot: " + info.warehouseLocation);
                            lblWhStock.setText("Available Pool: " + String.format("%,d Kg", info.warehouseStock));
                            lDepotSupplyStock.setText(String.format("%,d Kg", info.warehouseStock));
                        }
                        p.repaint();
                    });
                } catch (SQLException ex) { ex.printStackTrace(); }
            });
        };

        btnUpdate.addActionListener(e -> {
            int row = tbl.getSelectedRow(); if (row < 0) { PDSApp.showAlert(p, "Select a valid table row node configuration.", true); return; }
            int modelRow = tbl.convertRowIndexToModel(row);
            int sid = Integer.parseInt(model.getValueAt(modelRow, 0).toString()); int qty = (int) spQty.getValue();
            try {
                ShopController.updateStockQty(sid, qty);
                PDSApp.showAlert(p, "Database ledger synchronized.", false); 
                load.run();
            } catch (SQLException ex) { PDSApp.showAlert(p, ex.getMessage(), true); }});

        btnOrder.addActionListener(e -> {
            int row = tbl.getSelectedRow(); if (row < 0) { PDSApp.showAlert(p, "Select a commodity row first.", true); return; }
            int modelRow = tbl.convertRowIndexToModel(row);
            int sid = Integer.parseInt(model.getValueAt(modelRow, 0).toString()); int qty = (int) spQty.getValue();
            if (qty <= 0) { PDSApp.showAlert(p, "Quantity must be positive.", true); return; }
            try {
                ShopController.orderStock(shopId, sid, qty);
                PDSApp.showAlert(p, "Warehouse supply requested and delivered successfully.", false);
                load.run();
            } catch (SQLException ex) { PDSApp.showAlert(p, ex.getMessage(), true); }});

        btnRefresh.addActionListener(e -> load.run());

        JPanel header = new JPanel(new BorderLayout()); header.setBackground(PAGE); header.add(PDSApp.pageHeader("Stock Inventory Registry Deck"), BorderLayout.WEST); header.add(btnRefresh, BorderLayout.EAST);

        JPanel leftCard = PDSApp.card();
        leftCard.setLayout(new BorderLayout(0, 12));
        leftCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JPanel tblHeader = new JPanel(new BorderLayout());
        tblHeader.setOpaque(false);
        JLabel tblTitle = new JLabel("Outlet Stock Ledger");
        tblTitle.setFont(new Font(FAM, Font.BOLD, 15));
        tblTitle.setForeground(PDSApp.TEXT_PRIMARY);
        JLabel tblSub = new JLabel("Real-time inventory mapping and threshold diagnostics");
        tblSub.setFont(new Font(FAM, Font.PLAIN, 11));
        tblSub.setForeground(PDSApp.TEXT_SEC);
        JPanel titleGroup = new JPanel(new GridLayout(2, 1, 2, 0));
        titleGroup.setOpaque(false);
        titleGroup.add(tblTitle);
        titleGroup.add(tblSub);
        tblHeader.add(titleGroup, BorderLayout.WEST);
        
        leftCard.add(tblHeader, BorderLayout.NORTH);
        leftCard.add(PDSApp.tableScroll(tbl), BorderLayout.CENTER);
        
        // Form Configuration Right Container Stack Panel
        JPanel rightColumnStack = new JPanel(new GridBagLayout()); rightColumnStack.setBackground(PAGE);
        GridBagConstraints stackGbc = new GridBagConstraints(); stackGbc.fill = GridBagConstraints.HORIZONTAL; stackGbc.weightx = 1.0; stackGbc.gridx = 0;

        JPanel formCard = PDSApp.card(); formCard.setLayout(new GridBagLayout()); formCard.setBorder(new EmptyBorder(20, 24, 24, 24));
        GridBagConstraints fGbc = new GridBagConstraints(); fGbc.fill = GridBagConstraints.HORIZONTAL; fGbc.weightx = 1.0; fGbc.gridx = 0; fGbc.gridy = 0; fGbc.insets = new Insets(0, 0, 12, 0);
        formCard.add(PDSApp.sectionLabel("Manifest Operational Controls"), fGbc);

        // Warehouse Info sub-section
        fGbc.gridy++; fGbc.insets = new Insets(0, 0, 4, 0); formCard.add(lblWhInfo, fGbc);
        fGbc.gridy++; fGbc.insets = new Insets(0, 0, 16, 0); formCard.add(lblWhStock, fGbc);

        fGbc.gridy++; fGbc.insets = new Insets(0, 0, 4, 0); formCard.add(PDSApp.formLabel("Adjust Selected Inventory Bounds"), fGbc);
        fGbc.gridy++; fGbc.insets = new Insets(0, 0, 16, 0); spQty.setPreferredSize(new Dimension(100, 38)); formCard.add(spQty, fGbc);
        
        JPanel formBtns = new JPanel(new GridLayout(2, 1, 0, 8)); formBtns.setOpaque(false);
        formBtns.add(btnOrder); formBtns.add(btnUpdate);
        fGbc.gridy++; fGbc.insets = new Insets(4, 0, 0, 0); formCard.add(formBtns, fGbc);

        // Continuous Real-Time Vector Waveform Display
        JPanel waveCard = new JPanel() {
            private float wavePhase = 0f;
            private final Timer waveTimer = new Timer(30, e -> { wavePhase += 0.015f; if (wavePhase > 1.2f) wavePhase = -0.1f; repaint(); });
            { waveTimer.start(); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PDSApp.CARD_BG); g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.setColor(PDSApp.BORDER_CLR); g2.setStroke(new BasicStroke(1.2f)); g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 16, 16));

                int padX = 24, padY = 24; int w = getWidth() - (padX * 2); int h = getHeight() - (padY * 2) - 15; int baseY = getHeight() - padY;
                g2.setStroke(new BasicStroke(1f)); g2.setColor(new Color(0xF1F5F9));
                for (int i = 1; i <= 3; i++) { int gy = baseY - (h * i / 3); g2.drawLine(padX, gy, padX + w, gy); }

                int points = Math.min(7, liveWaveData.size());
                if (points > 1) {
                    Path2D poly = new Path2D.Float(); Path2D line = new Path2D.Float(); poly.moveTo(padX, baseY);
                    for (int i = 0; i < points; i++) {
                        int vx = padX + (w * i / (points - 1)); int vy = baseY - (int)(h * liveWaveData.get(i) / 100.0);
                        if (i == 0) { line.moveTo(vx, vy); } 
                        else {
                            int prevX = padX + (w * (i - 1) / (points - 1)); int prevY = baseY - (int)(h * liveWaveData.get(i - 1) / 100.0);
                            line.curveTo(prevX + 20, prevY, vx - 20, vy, vx, vy);
                        }
                        poly.curveTo(padX + (w * i / (points - 1)) - 20, baseY - (int)(h * (i == 0 ? liveWaveData.get(0) : liveWaveData.get(i-1)) / 100.0), vx - 20, vy, vx, vy);
                    }
                    poly.lineTo(padX + w, baseY); poly.closePath();
                    Color themeColor = PDSApp.SUCCESS;
                    g2.setPaint(new GradientPaint(0, baseY - h, new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 45), 0, baseY, new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 0)));
                    g2.fill(poly); g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)); g2.setColor(themeColor); g2.draw(line);

                    int scanX = padX + (int)(w * wavePhase);
                    if (scanX >= padX && scanX <= padX + w) {
                        g2.setPaint(new GradientPaint(scanX - 15, 0, new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 0), scanX, 0, new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 45)));
                        g2.fillRect(scanX - 15, padY, 15, h); g2.setColor(themeColor); g2.setStroke(new BasicStroke(1.5f)); g2.drawLine(scanX, padY, scanX, baseY);
                    }
                }
                g2.dispose();
            }
        };
        waveCard.setOpaque(false); waveCard.setPreferredSize(new Dimension(0, 160)); waveCard.setMinimumSize(new Dimension(0, 160)); waveCard.setLayout(new BorderLayout());
        JLabel lblWaveText = new JLabel("<html><center><b style='color:#0F172A; font-size:11px;'>Live Storage Volumetric Core Spectrum</b><br><span style='color:#16A34A; font-size:10px;'>● Waveform Tracking Active</span></center></html>", SwingConstants.CENTER);
        waveCard.setBorder(new EmptyBorder(12, 12, 0, 12)); waveCard.add(lblWaveText, BorderLayout.NORTH);

        stackGbc.gridy = 0; stackGbc.insets = new Insets(0,0,0,0); rightColumnStack.add(metricsGrid, stackGbc);
        stackGbc.gridy = 1; stackGbc.insets = new Insets(14,0,0,0); rightColumnStack.add(formCard, stackGbc);
        stackGbc.gridy = 2; stackGbc.insets = new Insets(14,0,0,0); rightColumnStack.add(waveCard, stackGbc);
        
        // Spacer to push everything to top
        stackGbc.gridy = 3; stackGbc.weighty = 1.0; stackGbc.fill = GridBagConstraints.BOTH;
        JPanel spacer = new JPanel(); spacer.setOpaque(false);
        rightColumnStack.add(spacer, stackGbc);

        // Add to main panel p
        // Left column
        gbc.gridx = 0; gbc.gridwidth = 1; gbc.weightx = 0.70;
        gbc.gridy = 0; gbc.weighty = 0.0; gbc.insets = new Insets(0, 0, 16, 0);
        p.add(header, gbc);
        
        gbc.gridy = 1; gbc.weighty = 1.0; gbc.insets = new Insets(0, 0, 0, 0);
        p.add(leftCard, gbc);

        // Right column
        gbc.gridx = 1; gbc.gridwidth = 1; gbc.weightx = 0.30;
        gbc.gridy = 0; gbc.gridheight = 2; gbc.weighty = 1.0; gbc.insets = new Insets(0, 18, 0, 0);
        p.add(rightColumnStack, gbc);

        load.run(); return p;
    }

    // ── Incoming Requests (Full-Width Responsive Grid Setup) ──
    private JPanel buildRequests() {
        JPanel p = new JPanel(new BorderLayout(0, 16)); p.setBackground(PAGE); p.setBorder(new EmptyBorder(16, 20, 16, 20));
        JTable tbl = PDSApp.styledTable(new String[]{"Req ID", "Beneficiary Handle Reference", "Ration Card String ID", "Date Processed", "Slot Date Mapping", "Opening Hour", "Closing Hour", "System Status State"});
        DefaultTableModel model = (DefaultTableModel) tbl.getModel();
        tbl.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                String val = (v != null) ? v.toString() : "";
                boolean ok = "FULFILLED".equals(val);
                JLabel pill = PDSApp.statusPill("  " + val + "  ", 
                    ok ? PDSApp.SUCCESS : PDSApp.ACCENT, 
                    ok ? PDSApp.SUCCESS_BG : PDSApp.ACCENT_BG);
                if (sel) {
                    pill.setBackground(PDSApp.ACCENT_BG);
                    pill.setForeground(PDSApp.ACCENT);
                }
                return pill;
            }
        });

        JButton btnDistribute = PDSApp.primaryBtn("✔  Execute Active Distribution Bounds");
        JButton btnRefresh = PDSApp.secondaryBtn("⟳  Refresh Roster Stack");

        Runnable load = () -> {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    java.util.List<Object[]> list = ShopController.getIncomingRequests(shopId);
                    SwingUtilities.invokeLater(() -> {
                        model.setRowCount(0);
                        for (Object[] row : list) {
                            model.addRow(row);
                        }
                    });
                } catch (SQLException ex) { ex.printStackTrace(); }
            });
        };

        btnDistribute.addActionListener(e -> {
            int selectedRow = tbl.getSelectedRow(); if (selectedRow < 0) { PDSApp.showAlert(p, "Select an input structural row coordinate first.", true); return; }
            int modelRow = tbl.convertRowIndexToModel(selectedRow);
            int reqId = Integer.parseInt(model.getValueAt(modelRow, 0).toString()); String status = model.getValueAt(modelRow, 7).toString();
            if ("FULFILLED".equals(status)) { PDSApp.showAlert(p, "Operational warning: Node is already closed.", true); return; }
            if (JOptionPane.showConfirmDialog(p, "Execute distribution for Request #" + reqId + "?", "Verify Operation Pipeline", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                try {
                    ShopController.executeRequestDistribution(reqId);
                    model.setValueAt("FULFILLED", modelRow, 7);
                    PDSApp.showAlert(p, "Allocation cleared successfully.", false);
                    Timer delayRefresh = new Timer(300, evt -> { load.run(); });
                    delayRefresh.setRepeats(false);
                    delayRefresh.start();
                } catch (SQLException ex) {
                    PDSApp.showAlert(p, "SQL System Fail: " + ex.getMessage(), true);
                }
            }});

        btnRefresh.addActionListener(e -> load.run());
        JPanel header = new JPanel(new BorderLayout()); header.setBackground(PAGE); header.add(PDSApp.pageHeader("Incoming Customer Requests"), BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0)); actions.setOpaque(false); actions.add(btnDistribute); actions.add(btnRefresh); header.add(actions, BorderLayout.EAST);

        JPanel mainCard = PDSApp.card(); mainCard.setLayout(new BorderLayout()); mainCard.add(PDSApp.tableScroll(tbl), BorderLayout.CENTER);
        p.add(header, BorderLayout.NORTH); p.add(mainCard, BorderLayout.CENTER);
        load.run(); return p;
    }

    // ── Assigned Beneficiaries (70-30 Side-by-Side Cockpit Design) ──
    private JPanel buildBeneficiaries() {
        JPanel p = new JPanel(new GridBagLayout()); p.setBackground(PAGE); p.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints gbc = new GridBagConstraints(); gbc.fill = GridBagConstraints.BOTH;

        JTable tbl = PDSApp.styledTable(new String[]{"ID", "Full Identity Surname Name", "Ration Verification Code", "Birth Timestamp", "Taxonomy Classification Status"});
        DefaultTableModel model = (DefaultTableModel) tbl.getModel();
        tbl.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                String val = (v != null) ? v.toString() : "INACTIVE"; boolean active = "ACTIVE".equals(val);
                JLabel pill = PDSApp.statusPill("  " + val + "  ", active ? PDSApp.SUCCESS : PDSApp.DANGER, active ? PDSApp.SUCCESS_BG : PDSApp.DANGER_BG);
                if (sel) { pill.setBackground(PDSApp.ACCENT_BG); pill.setForeground(PDSApp.ACCENT); } return pill; }});

        // Metrics Labels
        JLabel lTotalBen = new JLabel("0");
        lTotalBen.setFont(new Font(FAM, Font.BOLD, 18));
        lTotalBen.setForeground(PDSApp.TEXT_PRIMARY);

        JLabel lActiveBen = new JLabel("0");
        lActiveBen.setFont(new Font(FAM, Font.BOLD, 18));
        lActiveBen.setForeground(PDSApp.SUCCESS);

        JLabel lInactiveBen = new JLabel("0");
        lInactiveBen.setFont(new Font(FAM, Font.BOLD, 18));
        lInactiveBen.setForeground(PDSApp.DANGER);

        JLabel lCategoryCount = new JLabel("0");
        lCategoryCount.setFont(new Font(FAM, Font.BOLD, 18));
        lCategoryCount.setForeground(PDSApp.INFO);

        JPanel metricsGrid = new JPanel(new GridLayout(2, 2, 10, 10));
        metricsGrid.setBackground(PAGE);
        metricsGrid.add(PDSApp.statCard("Total Assigned", lTotalBen, PDSApp.ACCENT));
        metricsGrid.add(PDSApp.statCard("Active Profile", lActiveBen, PDSApp.SUCCESS));
        metricsGrid.add(PDSApp.statCard("Inactive Status", lInactiveBen, PDSApp.DANGER));
        metricsGrid.add(PDSApp.statCard("BPL & AAY Mix", lCategoryCount, PDSApp.INFO));

        CategoryBar barApl = new CategoryBar("APL (Above Poverty Line)", PDSApp.ACCENT);
        CategoryBar barBpl = new CategoryBar("BPL (Below Poverty Line)", PDSApp.SUCCESS);
        CategoryBar barPhh = new CategoryBar("PHH (Priority Household)", PDSApp.WARNING);
        CategoryBar barAay = new CategoryBar("AAY (Antyodaya Anna Yojana)", PDSApp.DANGER);

        JButton btnTopRefresh = PDSApp.secondaryBtn("⟳  Refresh Deck");

        Runnable load = () -> {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                int totalTemp = 0;
                int activeTemp = 0;
                int inactiveTemp = 0;
                int bplAayTemp = 0;
                int aplCountTemp = 0, bplCountTemp = 0, phhCountTemp = 0, aayCountTemp = 0;
                try {
                    java.util.List<Object[]> list = ShopController.getShopBeneficiaries(shopId);
                    for (Object[] row : list) {
                        totalTemp++;
                        String status = (row[4] != null) ? row[4].toString() : "INACTIVE";
                        if ("ACTIVE".equalsIgnoreCase(status)) {
                            activeTemp++;
                        } else {
                            inactiveTemp++;
                        }
                    }
                    Connection conn = DB.getConnection();
                    try (PreparedStatement ps = conn.prepareStatement(
                            "SELECT CategoryID, COUNT(*) FROM BENEFICIARY WHERE ShopID = ? GROUP BY CategoryID")) {
                        ps.setInt(1, shopId);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                int cid = rs.getInt(1);
                                int count = rs.getInt(2);
                                if (cid == 1) aplCountTemp = count;
                                else if (cid == 2) bplCountTemp = count;
                                else if (cid == 3) phhCountTemp = count;
                                else if (cid == 4) aayCountTemp = count;
                            }
                        }
                    }
                    bplAayTemp = bplCountTemp + aayCountTemp;
                    
                    final int finalTotal = totalTemp;
                    final int finalActive = activeTemp;
                    final int finalInactive = inactiveTemp;
                    final int finalBplAay = bplAayTemp;
                    final int finalApl = aplCountTemp;
                    final int finalBpl = bplCountTemp;
                    final int finalPhh = phhCountTemp;
                    final int finalAay = aayCountTemp;

                    SwingUtilities.invokeLater(() -> {
                        model.setRowCount(0);
                        for (Object[] row : list) {
                            model.addRow(row);
                        }
                        lTotalBen.setText(String.valueOf(finalTotal));
                        lActiveBen.setText(String.valueOf(finalActive));
                        lInactiveBen.setText(String.valueOf(finalInactive));
                        lCategoryCount.setText(String.valueOf(finalBplAay));
                        
                        barApl.update(finalApl, finalTotal);
                        barBpl.update(finalBpl, finalTotal);
                        barPhh.update(finalPhh, finalTotal);
                        barAay.update(finalAay, finalTotal);
                        p.repaint();
                    });
                } catch (SQLException ex) { ex.printStackTrace(); }
            });
        };

        btnTopRefresh.addActionListener(e -> load.run());

        JPanel header = new JPanel(new BorderLayout()); header.setBackground(PAGE);
        header.add(PDSApp.pageHeader("Assigned Beneficiary Register System"), BorderLayout.WEST);
        header.add(btnTopRefresh, BorderLayout.EAST);

        JPanel leftCard = PDSApp.card();
        leftCard.setLayout(new BorderLayout(0, 12));
        leftCard.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel tblHeader = new JPanel(new BorderLayout(12, 0));
        tblHeader.setOpaque(false);
        JLabel tblTitle = new JLabel("Outlet Beneficiary Registry");
        tblTitle.setFont(new Font(FAM, Font.BOLD, 15));
        tblTitle.setForeground(PDSApp.TEXT_PRIMARY);
        JLabel tblSub = new JLabel("Search and filter assigned cardholders");
        tblSub.setFont(new Font(FAM, Font.PLAIN, 11));
        tblSub.setForeground(PDSApp.TEXT_SEC);
        JPanel titleGroup = new JPanel(new GridLayout(2, 1, 2, 0));
        titleGroup.setOpaque(false);
        titleGroup.add(tblTitle);
        titleGroup.add(tblSub);
        tblHeader.add(titleGroup, BorderLayout.WEST);

        // Search input inside table header
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        searchPanel.setOpaque(false);
        JTextField tfSearch = PDSApp.inputField(14);
        tfSearch.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "🔍 Search beneficiaries...");
        tfSearch.setPreferredSize(new Dimension(220, 36));
        searchPanel.add(tfSearch);
        tblHeader.add(searchPanel, BorderLayout.EAST);

        tfSearch.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                String term = tfSearch.getText().trim().toLowerCase();
                TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(model);
                tbl.setRowSorter(sorter);
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + term));
            }
        });

        leftCard.add(tblHeader, BorderLayout.NORTH);
        leftCard.add(PDSApp.tableScroll(tbl), BorderLayout.CENTER);

        JPanel rightColumnStack = new JPanel(new GridBagLayout()); rightColumnStack.setBackground(PAGE);
        GridBagConstraints stackGbc = new GridBagConstraints(); stackGbc.fill = GridBagConstraints.HORIZONTAL; stackGbc.weightx = 1.0; stackGbc.gridx = 0;

        JPanel formCard = PDSApp.card(); formCard.setLayout(new GridBagLayout()); formCard.setBorder(new EmptyBorder(20, 24, 24, 24));
        GridBagConstraints fGbc = new GridBagConstraints(); fGbc.fill = GridBagConstraints.HORIZONTAL; fGbc.weightx = 1.0; fGbc.gridx = 0; fGbc.gridy = 0;
        formCard.add(PDSApp.sectionLabel("Active Management Node Info"), fGbc);
        fGbc.gridy++; fGbc.insets = new Insets(10, 0, 4, 0);
        JTextArea infoText = new JTextArea("Authorized registry profiles allocation node handles. Modifications must bypass administrative district channels.");
        infoText.setEditable(false); infoText.setLineWrap(true); infoText.setWrapStyleWord(true); infoText.setBackground(PDSApp.CARD_BG); infoText.setFont(PDSApp.FONT_LABEL);
        infoText.setForeground(PDSApp.TEXT_SEC);
        formCard.add(infoText, fGbc);

        // Visual Category allocation breakdown card
        JPanel categoryCard = PDSApp.card(); categoryCard.setLayout(new GridBagLayout()); categoryCard.setBorder(new EmptyBorder(20, 24, 24, 24));
        GridBagConstraints cGbc = new GridBagConstraints(); cGbc.fill = GridBagConstraints.HORIZONTAL; cGbc.weightx = 1.0; cGbc.gridx = 0; cGbc.gridy = 0;
        categoryCard.add(PDSApp.sectionLabel("Taxonomy Category Mix"), cGbc);
        cGbc.insets = new Insets(10, 0, 0, 0);
        cGbc.gridy++; categoryCard.add(barApl, cGbc);
        cGbc.gridy++; cGbc.insets = new Insets(6, 0, 0, 0); categoryCard.add(barBpl, cGbc);
        cGbc.gridy++; categoryCard.add(barPhh, cGbc);
        cGbc.gridy++; categoryCard.add(barAay, cGbc);

        stackGbc.gridy = 0; stackGbc.insets = new Insets(0,0,0,0); rightColumnStack.add(metricsGrid, stackGbc);
        stackGbc.gridy = 1; stackGbc.insets = new Insets(14,0,0,0); rightColumnStack.add(categoryCard, stackGbc);
        stackGbc.gridy = 2; stackGbc.insets = new Insets(14,0,0,0); rightColumnStack.add(formCard, stackGbc);

        // Spacer to push everything to top
        stackGbc.gridy = 3; stackGbc.weighty = 1.0; stackGbc.fill = GridBagConstraints.BOTH;
        JPanel spacer = new JPanel(); spacer.setOpaque(false);
        rightColumnStack.add(spacer, stackGbc);

        // Add to main panel p
        // Left column (70% width)
        gbc.gridx = 0; gbc.gridwidth = 1; gbc.weightx = 0.70;
        gbc.gridy = 0; gbc.weighty = 0.0; gbc.insets = new Insets(0, 0, 16, 0);
        p.add(header, gbc);

        gbc.gridy = 1; gbc.weighty = 1.0; gbc.insets = new Insets(0, 0, 0, 0);
        p.add(leftCard, gbc);

        // Right column (30% width)
        gbc.gridx = 1; gbc.gridwidth = 1; gbc.weightx = 0.30;
        gbc.gridy = 0; gbc.gridheight = 2; gbc.weighty = 1.0; gbc.insets = new Insets(0, 18, 0, 0);
        p.add(rightColumnStack, gbc);

        load.run(); return p;
    }

    // Custom Category Allocation Bar component
    private static class CategoryBar extends JPanel {
        private final String label;
        private final Color barColor;
        private int count = 0;
        private int max = 1;

        CategoryBar(String label, Color barColor) {
            this.label = label;
            this.barColor = barColor;
            setOpaque(false);
            setPreferredSize(new Dimension(0, 38));
        }

        void update(int count, int total) {
            this.count = count;
            this.max = Math.max(1, total);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setFont(PDSApp.FONT_LABEL);
            g2.setColor(PDSApp.TEXT_PRIMARY);
            g2.drawString(label, 0, 14);

            String valStr = String.valueOf(count);
            g2.setFont(new Font(FAM, Font.BOLD, 12));
            g2.setColor(PDSApp.TEXT_PRIMARY);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(valStr, getWidth() - fm.stringWidth(valStr), 14);

            int barY = 22;
            int barH = 6;
            g2.setColor(PDSApp.SOFT_BG);
            g2.fill(new RoundRectangle2D.Float(0, barY, getWidth(), barH, 3, 3));

            float pct = (float) count / max;
            int fillW = (int) (getWidth() * pct);
            if (fillW > 0) {
                g2.setColor(barColor);
                g2.fill(new RoundRectangle2D.Float(0, barY, fillW, barH, 3, 3));
            }
            g2.dispose();
        }
    }

    private JPanel coloredInfoTile(String label, JLabel valLbl, Color bgCol, Color borderCol) {
        JPanel p = new JPanel(new BorderLayout(0, 6)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgCol);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(borderCol);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 12, 12));
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(18, 20, 18, 20));
        JLabel lbl = new JLabel(label.toUpperCase());
        lbl.setFont(new Font(FAM, Font.BOLD, 10));
        lbl.setForeground(PDSApp.TEXT_SEC);
        valLbl.setFont(new Font(FAM, Font.BOLD, 15));
        valLbl.setForeground(PDSApp.TEXT_PRIMARY);
        p.add(lbl,    BorderLayout.NORTH);
        p.add(valLbl, BorderLayout.CENTER);
        return p;
    }

    // ── About My Shop Layout Profile (Double-Column Labeled Corporate Config) ──
    private JPanel buildAboutPanel() {
        JPanel p = new JPanel(new GridBagLayout()); p.setBackground(PAGE); p.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints gbc = new GridBagConstraints(); gbc.fill = GridBagConstraints.BOTH;

        // Info labels
        JLabel lblShopName = new JLabel("—"), lblLocation = new JLabel("—"), lblMaxCap = new JLabel("—");
        JLabel lblGst = new JLabel("—"), lblLicense = new JLabel("—"), lblOfficer = new JLabel("—"), lblEmail = new JLabel("—");
        JLabel lblWarehouseLoc = new JLabel("—"), lblWarehouseStock = new JLabel("—");

        // Image Banner Card loading resources/shop_hero_banner.png (Stretched vertically to 220px)
        JPanel bannerCard = new JPanel() {
            private Image img = null;
            {
                try {
                    java.io.File f = new java.io.File("resources/shop_hero_banner.png");
                    if (!f.exists()) {
                        f = new java.io.File("resources/shopherobanner.png");
                    }
                    if (f.exists()) {
                        img = javax.imageio.ImageIO.read(f);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                
                RoundRectangle2D.Float clip = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setClip(clip);
                if (img != null) {
                    g2.drawImage(img, 0, 0, getWidth(), getHeight(), null);
                } else {
                    g2.setPaint(new GradientPaint(0, 0, PDSApp.ACCENT, getWidth(), getHeight(), PDSApp.INFO));
                    g2.fill(clip);
                }
                
                // Dark overlay to make elements stand out
                g2.setPaint(new GradientPaint(0, 0, new Color(0, 0, 0, 40), 0, getHeight(), new Color(0, 0, 0, 110)));
                g2.fill(clip);
                
                g2.setClip(null);
                g2.setColor(PDSApp.BORDER_CLR);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 16, 16));
                g2.dispose();
            }
        };
        bannerCard.setOpaque(false);
        bannerCard.setPreferredSize(new Dimension(0, 220));
        bannerCard.setMinimumSize(new Dimension(0, 220));

        // Colored Tiles Row (Using custom colored cards)
        JPanel tilesRow = new JPanel(new GridLayout(1, 3, 16, 0)); tilesRow.setBackground(PAGE);
        tilesRow.add(coloredInfoTile("Shop Name / Outlet ID", lblShopName, new Color(0xE0ECFA), new Color(0xACC7EC)));
        tilesRow.add(coloredInfoTile("Geographical Location", lblLocation, new Color(0xDCFCE7), new Color(0x86EFAC)));
        tilesRow.add(coloredInfoTile("Max Volume Capacity", lblMaxCap, new Color(0xF3E8FF), new Color(0xD8B4FE)));

        // Details Card (Soft slate gradient background)
        JPanel gridCard = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(0xF8FAFC), getWidth(), getHeight(), new Color(0xF1F5F9)));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.setColor(PDSApp.BORDER_CLR);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 16, 16));
                g2.dispose();
            }
        };
        gridCard.setOpaque(false);
        gridCard.setLayout(new GridBagLayout()); gridCard.setBorder(new EmptyBorder(20, 24, 20, 24));
        GridBagConstraints gc = new GridBagConstraints(); gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 0.5; gc.insets = new Insets(6, 12, 14, 12);
        gc.gridx = 0; gc.gridy = 0; gridCard.add(PDSApp.formLabel("Registered GSTIN Identification Number"), gc);
        gc.gridy = 1; lblGst.setFont(new Font(FAM, Font.BOLD, 14)); lblGst.setForeground(PDSApp.TEXT_PRIMARY); gridCard.add(lblGst, gc);
        gc.gridy = 2; gridCard.add(PDSApp.formLabel("Nodal Distribution Officer"), gc);
        gc.gridy = 3; lblOfficer.setFont(new Font(FAM, Font.BOLD, 14)); lblOfficer.setForeground(PDSApp.TEXT_PRIMARY); gridCard.add(lblOfficer, gc);
        
        gc.gridy = 4; gridCard.add(PDSApp.formLabel("Assigned Supply Warehouse Location"), gc);
        gc.gridy = 5; lblWarehouseLoc.setFont(new Font(FAM, Font.BOLD, 14)); lblWarehouseLoc.setForeground(PDSApp.TEXT_PRIMARY); gridCard.add(lblWarehouseLoc, gc);

        gc.gridx = 1; gc.gridy = 0; gridCard.add(PDSApp.formLabel("Departmental License Registry Code"), gc);
        gc.gridy = 1; lblLicense.setFont(new Font(FAM, Font.BOLD, 14)); lblLicense.setForeground(PDSApp.TEXT_PRIMARY); gridCard.add(lblLicense, gc);
        gc.gridy = 2; gridCard.add(PDSApp.formLabel("Official Portal Communications Email"), gc);
        gc.gridy = 3; lblEmail.setFont(new Font(FAM, Font.BOLD, 14)); lblEmail.setForeground(PDSApp.TEXT_PRIMARY); gridCard.add(lblEmail, gc);
        
        gc.gridy = 4; gridCard.add(PDSApp.formLabel("Warehouse Available Stock Pool"), gc);
        gc.gridy = 5; lblWarehouseStock.setFont(new Font(FAM, Font.BOLD, 14)); lblWarehouseStock.setForeground(PDSApp.TEXT_PRIMARY); gridCard.add(lblWarehouseStock, gc);

        CapacityGaugePanel gaugePanel = new CapacityGaugePanel();
        
        // Right side stack
        JPanel rightColumnStack = new JPanel(new GridBagLayout()); rightColumnStack.setBackground(PAGE);
        GridBagConstraints rcGbc = new GridBagConstraints(); rcGbc.fill = GridBagConstraints.HORIZONTAL; rcGbc.weightx = 1.0; rcGbc.gridx = 0; rcGbc.gridy = 0;

        // Utilization Card (Soft slate gradient background)
        JPanel utilCard = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(0xF8FAFC), getWidth(), getHeight(), new Color(0xF1F5F9)));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.setColor(PDSApp.BORDER_CLR);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 16, 16));
                g2.dispose();
            }
        };
        utilCard.setOpaque(false);
        utilCard.setLayout(new BorderLayout()); utilCard.setBorder(new EmptyBorder(20, 24, 24, 24));
        utilCard.add(PDSApp.sectionLabel("Outlet Storage Volumetric Load"), BorderLayout.NORTH);
        utilCard.add(gaugePanel, BorderLayout.CENTER);

        rightColumnStack.add(utilCard, rcGbc);

        // Spacer to push right column up
        rcGbc.gridy = 1; rcGbc.weighty = 1.0; rcGbc.fill = GridBagConstraints.BOTH;
        JPanel spacer = new JPanel(); spacer.setOpaque(false);
        rightColumnStack.add(spacer, rcGbc);

        // Left column layout panel
        JPanel leftColumnStack = new JPanel(new GridBagLayout()); leftColumnStack.setBackground(PAGE);
        GridBagConstraints lcGbc = new GridBagConstraints(); lcGbc.fill = GridBagConstraints.HORIZONTAL; lcGbc.weightx = 1.0; lcGbc.gridx = 0; lcGbc.gridy = 0;
        leftColumnStack.add(bannerCard, lcGbc);
        lcGbc.gridy = 1; lcGbc.insets = new Insets(16, 0, 0, 0);
        leftColumnStack.add(PDSApp.sectionLabel("Outlet Technical Registration Overview"), lcGbc);
        lcGbc.gridy = 2; lcGbc.insets = new Insets(8, 0, 18, 0); leftColumnStack.add(tilesRow, lcGbc);
        lcGbc.gridy = 3; lcGbc.insets = new Insets(0, 0, 0, 0); leftColumnStack.add(PDSApp.sectionLabel("Detailed Identification Records"), lcGbc);
        lcGbc.gridy = 4; lcGbc.insets = new Insets(8, 0, 0, 0); leftColumnStack.add(gridCard, lcGbc);

        // Spacer for left column
        lcGbc.gridy = 5; lcGbc.weighty = 1.0; lcGbc.fill = GridBagConstraints.BOTH;
        JPanel leftSpacer = new JPanel(); leftSpacer.setOpaque(false);
        leftColumnStack.add(leftSpacer, lcGbc);

        // Refresh action
        Runnable load = () -> {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    ShopController.OutletInfo info = ShopController.getOutletInfo(shopId);
                    if (info != null) {
                        // Calculate local occupied storage
                        int occupiedTemp = 0;
                        Connection conn = DB.getConnection();
                        try (PreparedStatement ps = conn.prepareStatement(
                                "SELECT COALESCE(SUM(QuantityAvailable), 0) FROM STOCK WHERE ShopID = ?")) {
                            ps.setInt(1, shopId);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    occupiedTemp = rs.getInt(1);
                                }
                            }
                        }
                        
                        final int finalOccupied = occupiedTemp;
                        SwingUtilities.invokeLater(() -> {
                            lblShopName.setText(info.shopName + " (FPS #" + shopId + ")");
                            lblLocation.setText(info.location);
                            lblMaxCap.setText("10,000 Kg");
                            lblGst.setText("07AAAAC" + (shopId * 13) + "A1Z" + (shopId % 9));
                            lblLicense.setText("PDS-FPS-REG-" + (shopId + 4200));
                            lblOfficer.setText("Inspector Corp. Node " + (shopId + 12));
                            lblEmail.setText("fps" + shopId + "@pds.gov.in");
                            lblWarehouseLoc.setText(info.warehouseLocation);
                            lblWarehouseStock.setText(String.format("%,d Kg", info.warehouseStock));
                            gaugePanel.updateValue(finalOccupied, 10000);
                            p.repaint();
                        });
                    }
                } catch (SQLException ex) { ex.printStackTrace(); }
            });
        };

        JButton btnSync = PDSApp.secondaryBtn("⟳  Sync Outlet Meta");
        btnSync.addActionListener(e -> load.run());

        JButton btnPass = PDSApp.secondaryBtn("🔑  Change Password");
        btnPass.addActionListener(e -> showChangePasswordDialog());
        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightBtns.setOpaque(false);
        rightBtns.add(btnPass);
        rightBtns.add(btnSync);

        JPanel header = new JPanel(new BorderLayout()); header.setBackground(PAGE);
        header.add(PDSApp.pageHeader("Fair-Price Shop Blueprint Profile"), BorderLayout.WEST);
        header.add(rightBtns, BorderLayout.EAST);
        header.setBorder(new EmptyBorder(0, 0, 16, 0));

        // Add to main panel p (60-40 screen split)
        // Left Column (60% width)
        gbc.gridx = 0; gbc.gridwidth = 1; gbc.weightx = 0.60;
        gbc.gridy = 0; gbc.weighty = 0.0; gbc.insets = new Insets(0, 0, 16, 0);
        p.add(header, gbc);

        gbc.gridy = 1; gbc.weighty = 1.0; gbc.insets = new Insets(0, 0, 0, 0);
        p.add(leftColumnStack, gbc);

        // Right Column (40% width)
        gbc.gridx = 1; gbc.gridwidth = 1; gbc.weightx = 0.40;
        gbc.gridy = 0; gbc.gridheight = 2; gbc.weighty = 1.0; gbc.insets = new Insets(0, 18, 0, 0);
        p.add(rightColumnStack, gbc);

        load.run(); return p;
    }

    // Capacity Gauge Panel showing real occupied storage vs total capacity
    private static class CapacityGaugePanel extends JPanel {
        private double pct = 0.0;
        private double targetPct = 0.0;
        private int capacity = 100;
        private int occupied = 0;
        private final Timer animTimer = new Timer(20, e -> {
            if (pct < targetPct) {
                pct += 0.01;
                if (pct > targetPct) pct = targetPct;
                repaint();
            } else if (pct > targetPct) {
                pct -= 0.01;
                if (pct < targetPct) pct = targetPct;
                repaint();
            }
        });

        CapacityGaugePanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(0, 180));
            animTimer.start();
        }

        void updateValue(int occupied, int capacity) {
            this.occupied = occupied;
            this.capacity = Math.max(1, capacity);
            this.targetPct = (double) occupied / this.capacity;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int size = Math.min(getWidth(), getHeight()) - 40;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2 + 10;

            // Background circle track
            g2.setStroke(new BasicStroke(12, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(PDSApp.SOFT_BG);
            g2.drawOval(x, y, size, size);

            // Foreground progress arc with warning colors (Green -> Amber -> Red)
            int angle = (int) (pct * 360);
            Color progressColor;
            if (pct < 0.5) {
                progressColor = PDSApp.SUCCESS;
            } else if (pct < 0.85) {
                progressColor = PDSApp.WARNING;
            } else {
                progressColor = PDSApp.DANGER;
            }
            g2.setColor(progressColor);
            g2.drawArc(x, y, size, size, 90, -angle);

            // Text inside
            String pctStr = String.format("%.1f%%", pct * 100);
            g2.setFont(new Font(FAM, Font.BOLD, 18));
            g2.setColor(PDSApp.TEXT_PRIMARY);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(pctStr, (getWidth() - fm.stringWidth(pctStr)) / 2, y + size / 2 + 6);

            String label = String.format("%,d / %,d Kg", occupied, capacity);
            g2.setFont(new Font(FAM, Font.PLAIN, 11));
            g2.setColor(PDSApp.TEXT_SEC);
            FontMetrics fm2 = g2.getFontMetrics();
            g2.drawString(label, (getWidth() - fm2.stringWidth(label)) / 2, y + size / 2 + 24);

            g2.dispose();
        }
    }

    // Storefront Visual Card with interactive hover highlight
    private static class StorefrontVisualPanel extends JPanel {
        private boolean isMouseOver = false;
        private float hoverFactor = 0f;
        private final Timer hoverTimer = new Timer(20, e -> {
            if (isMouseOver) {
                if (hoverFactor < 1.0f) { hoverFactor += 0.05f; repaint(); }
            } else {
                if (hoverFactor > 0.0f) { hoverFactor -= 0.05f; repaint(); }
            }
        });

        StorefrontVisualPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(0, 160));
            hoverTimer.start();
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { isMouseOver = true; }
                @Override public void mouseExited(MouseEvent e) { isMouseOver = false; }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int cx = getWidth() / 2;
            int cy = getHeight() - 25;

            // Draw base line
            g2.setColor(PDSApp.BORDER_CLR);
            g2.setStroke(new BasicStroke(3));
            g2.drawLine(cx - 70, cy, cx + 70, cy);

            // Draw shop building backing
            int sh = 50;
            int sw = 100;
            int sy = cy - sh;
            int sx = cx - sw / 2;
            
            // Draw building wall
            g2.setColor(new Color(0xF8FAFC));
            g2.fill(new RoundRectangle2D.Float(sx, sy, sw, sh, 4, 4));
            g2.setColor(PDSApp.BORDER_CLR);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(new RoundRectangle2D.Float(sx, sy, sw, sh, 4, 4));

            // Draw door with a glowing hover effect
            int dw = 24;
            int dh = 36;
            int dx = cx - dw / 2;
            int dy = cy - dh;
            Color doorColor = new Color(0x3B82F6); // Blue
            if (hoverFactor > 0f) {
                int glowAlpha = (int)(45 * hoverFactor);
                g2.setPaint(new RadialGradientPaint(cx, cy - 15, 30f, new float[]{0f, 1f}, new Color[]{new Color(0x3B82F6), new Color(59, 130, 246, 0)}));
                g2.fill(new RoundRectangle2D.Float(dx - 6, dy - 6, dw + 12, dh + 6, 4, 4));
            }
            g2.setColor(doorColor);
            g2.fill(new RoundRectangle2D.Float(dx, dy, dw, dh, 4, 4));

            // Draw shop roof/awning
            int ry = sy - 10;
            int rh = 14;
            int rw = sw + 12;
            int rx = cx - rw / 2;
            
            // Draw awning stripes
            g2.setColor(new Color(0x1E293B));
            g2.fill(new RoundRectangle2D.Float(rx, ry, rw, rh, 4, 4));
            
            g2.setColor(PDSApp.ACCENT);
            for (int i = rx + 6; i < rx + rw - 6; i += 18) {
                g2.fill(new RoundRectangle2D.Float(i, ry, 8, rh, 2, 2));
            }

            // Draw sign board at the top
            int sby = ry - 14;
            int sbw = sw - 16;
            int sbh = 12;
            int sbx = cx - sbw / 2;
            g2.setColor(new Color(0x0F172A));
            g2.fill(new RoundRectangle2D.Float(sbx, sby, sbw, sbh, 3, 3));

            g2.setFont(new Font(FAM, Font.BOLD, 8));
            g2.setColor(Color.WHITE);
            String sign = "FAIR PRICE SHOP";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(sign, cx - fm.stringWidth(sign) / 2, sby + 9);

            g2.dispose();
        }
    }

    // ── Order & Supply Audit Logs (Full-Width Polished Ledger View) ──
    private JPanel buildOrdersPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 16)); p.setBackground(PAGE); p.setBorder(new EmptyBorder(16, 20, 16, 20));
        JTable tbl = PDSApp.styledTable(new String[]{"Log ID", "Allocated Commodity Item SKU", "Operation Modality Status", "System Timestamp"});
        DefaultTableModel model = (DefaultTableModel) tbl.getModel();
        tbl.getColumnModel().getColumn(0).setMaxWidth(90); tbl.getColumnModel().getColumn(3).setPreferredWidth(180);
        tbl.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                String val = (v != null) ? v.toString() : "UNKNOWN"; JLabel pill;
                if ("📦 DISBURSED".equals(val)) pill = PDSApp.statusPill("  " + val + "  ", PDSApp.ACCENT, PDSApp.ACCENT_BG);
                else if ("📥 REPLENISHED".equals(val)) pill = PDSApp.statusPill("  " + val + "  ", PDSApp.SUCCESS, PDSApp.SUCCESS_BG);
                else pill = PDSApp.statusPill("  " + val + "  ", PDSApp.TEXT_MUTED, PDSApp.SOFT_BG);
                if (sel) { pill.setBackground(PDSApp.ACCENT_BG); pill.setForeground(PDSApp.ACCENT); } return pill; }});

        JButton btnSync = PDSApp.secondaryBtn("⟳  Refresh Logs");

        Runnable load = () -> {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    java.util.List<Object[]> logs = ShopController.getAuditLogs();
                    SwingUtilities.invokeLater(() -> {
                        model.setRowCount(0);
                        for (Object[] row : logs) {
                            model.addRow(row);
                        }
                        if (logs.isEmpty()) {
                            model.addRow(new Object[]{"—", "No logged records found", "—", "—"});
                        }
                        p.repaint();
                    });
                } catch (SQLException ex) { ex.printStackTrace(); }
            });
        };

        btnSync.addActionListener(e -> load.run());
        JPanel header = new JPanel(new BorderLayout()); header.setBackground(PAGE);
        header.add(PDSApp.pageHeader("Inventory Supply & Replenishment Audit"), BorderLayout.WEST);
        header.add(btnSync, BorderLayout.EAST);

        JPanel mainCard = PDSApp.card();
        mainCard.setLayout(new BorderLayout(0, 12));
        mainCard.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel tblHeader = new JPanel(new BorderLayout());
        tblHeader.setOpaque(false);
        JLabel tblTitle = new JLabel("Ledger Activity Log");
        tblTitle.setFont(new Font(FAM, Font.BOLD, 15));
        tblTitle.setForeground(PDSApp.TEXT_PRIMARY);
        JLabel tblSub = new JLabel("Historical ledger of stock replenishments and distribution disbursements");
        tblSub.setFont(new Font(FAM, Font.PLAIN, 11));
        tblSub.setForeground(PDSApp.TEXT_SEC);
        JPanel titleGroup = new JPanel(new GridLayout(2, 1, 2, 0));
        titleGroup.setOpaque(false);
        titleGroup.add(tblTitle);
        titleGroup.add(tblSub);
        tblHeader.add(titleGroup, BorderLayout.WEST);

        mainCard.add(tblHeader, BorderLayout.NORTH);
        mainCard.add(PDSApp.tableScroll(tbl), BorderLayout.CENTER);

        p.add(header, BorderLayout.NORTH);
        p.add(mainCard, BorderLayout.CENTER);

        load.run(); return p;
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

    private String scalar(Statement st, String sql) throws SQLException {
        ResultSet rs = st.executeQuery(sql);
        return rs.next() ? rs.getString(1) : "0";
    }
}