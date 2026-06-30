import com.formdev.flatlaf.FlatClientProperties;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BeneficiaryFrame extends JFrame {

    private static final String FAM = pickFamily();
    private static String pickFamily() {
        String[] p={"Inter","SF Pro Text","Segoe UI","Helvetica Neue","Arial"};
        java.util.Set<String> a=new java.util.HashSet<>();
        for(String f:GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames())a.add(f);
        for(String x:p)if(a.contains(x))return x; return "SansSerif";
    }

    private final int benId;
    private final String username;
    private int shopId = -1;

    private CardLayout cards   = new CardLayout();
    private JPanel     content = new JPanel(cards);
    private PDSApp.AnimatedSidebarButton[] navBtns;
    private JLabel     breadcrumb = new JLabel();
    private JLabel     lblTotalAlloc = new JLabel("— items");
    private JLabel     lblExhausted  = new JLabel("— items");
    private JLabel     lblAvailable  = new JLabel("— items");
    private JLabel     lblReqShopName = new JLabel("—");
    private JLabel     lblReqCategory = new JLabel("—");
    private JLabel     lblReqTotalVisits = new JLabel("—");
    private JLabel     lblReqPendingVisits = new JLabel("—");
    private JLabel     lblReqFulfilledVisits = new JLabel("—");
    private JLabel     lblReqTotalItems = new JLabel("—");
    private JLabel     lblDashboardShopLocation = new JLabel("📍  Shop Depot: —");
    private int        currentNav = -1;
    private final Runnable[] pageLoaders = new Runnable[7];
    private JLabel     avLabel;

    private String[] navCards  = {"PROFILE","QUOTA","REQUEST","HISTORY","ABOUT","TRANSFER","PASSWORD"};
    private String[] navLabels = {"Dashboard", "Entitlements", "New Request", "Transactions", "Card Details", "Transfer Shop", "Change Password"};
    private String[] navIconPaths = {
        "/resources/icons/overview.png",
        "/resources/icons/commodities.png",
        "/resources/icons/rules.png",
        "/resources/icons/audit.png",
        "/resources/icons/profile.png",
        "/resources/icons/shops.png",
        "/resources/icons/categories.png"
    };

    public BeneficiaryFrame(String username, int benId) {
        super("PDS — Beneficiary Portal  |  " + username);
        this.benId = benId; this.username = username;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1140, 720); setMinimumSize(new Dimension(960, 620));
        setLocationRelativeTo(null); setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);
        JPanel main = new JPanel(new BorderLayout());
        main.add(buildTopBar(),   BorderLayout.NORTH);
        main.add(buildContent(),  BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);
        activateNav(0); setVisible(true);
        SwingUtilities.invokeLater(this::animateSidebarIn);
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PDSApp.SIDEBAR_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(240, 0));
        side.setBorder(new MatteBorder(0, 0, 0, 1, PDSApp.SIDEBAR_SEC));

        // Brand
        JPanel brand = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
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
        JLabel sub = new JLabel("Beneficiary Services");
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
        
        navBtns = new PDSApp.AnimatedSidebarButton[navCards.length];
        String[] groups = {
            "BENEFICIARY SERVICES",
            "BENEFICIARY SERVICES",
            "BENEFICIARY SERVICES",
            "BENEFICIARY SERVICES",
            "BENEFICIARY SERVICES",
            "BENEFICIARY SERVICES",
            "BENEFICIARY SERVICES"
        };
        String lastGroup = "";
        for (int i = 0; i < navCards.length; i++) {
            if (!groups[i].equals(lastGroup)) {
                if (!lastGroup.isEmpty()) {
                    side.add(Box.createVerticalStrut(12));
                }
                side.add(sideLabel(groups[i]));
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

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(PDSApp.CARD_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(PDSApp.BORDER_CLR);
                g2.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                g2.dispose();
            }
        };
        bar.setOpaque(false); bar.setBorder(new EmptyBorder(12, 24, 12, 24)); bar.setPreferredSize(new Dimension(0, 56));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0)); left.setOpaque(false);
        JLabel ico = new JLabel("⌂"); ico.setFont(new Font(FAM, Font.PLAIN, 14)); ico.setForeground(PDSApp.TEXT_MUTED);
        breadcrumb.setFont(new Font(FAM, Font.PLAIN, 13)); breadcrumb.setForeground(PDSApp.TEXT_SEC);
        left.add(ico); left.add(breadcrumb); bar.add(left, BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)); right.setOpaque(false);
        JLabel role = new JLabel("Beneficiary");
        role.setFont(new Font(FAM, Font.BOLD, 11));
        role.setForeground(PDSApp.SUCCESS);
        role.setBackground(PDSApp.SUCCESS_BG);
        role.setOpaque(true);
        role.setBorder(new EmptyBorder(5, 12, 5, 12));
        avLabel = new JLabel(username.substring(0, Math.min(2, username.length())).toUpperCase()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PDSApp.SUCCESS);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avLabel.setFont(new Font(FAM, Font.BOLD, 11)); avLabel.setForeground(Color.WHITE); avLabel.setPreferredSize(new Dimension(32, 32)); avLabel.setHorizontalAlignment(SwingConstants.CENTER); avLabel.setOpaque(false);
        right.add(role); right.add(avLabel); bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private void activateNav(int idx) {
        if (currentNav == idx) {
            if (pageLoaders[idx] != null) {
                pageLoaders[idx].run();
            }
            return;
        }
        currentNav = idx;
        for (int i = 0; i < navBtns.length; i++) navBtns[i].setActive(i == idx);
        cards.show(content, navCards[idx]);
        breadcrumb.setText("Beneficiary  ›  " + navLabels[idx]);
        if (pageLoaders[idx] != null) {
            pageLoaders[idx].run();
        }
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

    private static final Color PAGE = PDSApp.PAGE_BG;

    private JPanel buildContent() {
        content.setBackground(PAGE);
        try {
            shopId = BeneficiaryController.getShopId(benId);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        content.add(buildProfile(),"PROFILE");
        content.add(buildQuota(),  "QUOTA");
        content.add(buildRequest(),"REQUEST");
        content.add(buildHistory(),"HISTORY");
        content.add(buildAbout(),  "ABOUT");
        content.add(buildTransfer(),"TRANSFER");
        content.add(buildPassword(),"PASSWORD");
        return content;
    }

    private JPanel buildProfile() {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setBackground(PAGE); p.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Profile info tiles
        JLabel lName=new JLabel("—"),lCard=new JLabel("—"),lDob=new JLabel("—"),lCat=new JLabel("—"),lShop=new JLabel("—"),lStat=new JLabel("—"),lLoc=new JLabel("—"),lCap=new JLabel("—");
        JPanel tiles = new JPanel(new GridLayout(2, 2, 14, 14));
        tiles.setBackground(PAGE);
        tiles.add(PDSApp.infoTile("Name",         lName));
        tiles.add(PDSApp.infoTile("Date of Birth",lDob));
        tiles.add(PDSApp.infoTile("Shop Location",lLoc));
        tiles.add(PDSApp.infoTile("Shop Capacity",lCap));

        // Activity KPIs
        JLabel lReqs=new JLabel("—"), lFulfilled=new JLabel("—"), lPending=new JLabel("—"), lItems=new JLabel("—");
        JPanel kpis = new JPanel(new GridLayout(1, 4, 14, 0));
        kpis.setBackground(PAGE);
        kpis.add(PDSApp.statCard("Total Requests",   lReqs,     PDSApp.ACCENT));
        kpis.add(PDSApp.statCard("Fulfilled",         lFulfilled,PDSApp.SUCCESS));
        kpis.add(PDSApp.statCard("Pending",           lPending,  PDSApp.WARNING));
        kpis.add(PDSApp.statCard("Items Received",    lItems,    PDSApp.ACCENT));

        // Recent activity table
        JTable tblRecent = PDSApp.styledTable(new String[]{"Req ID","Date","Items","Status"});
        DefaultTableModel recentModel = (DefaultTableModel) tblRecent.getModel();
        tblRecent.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                String val = (v != null) ? v.toString() : "";
                JLabel pill;
                if ("FULFILLED".equals(val)) {
                    pill = PDSApp.statusPill("  FULFILLED  ", PDSApp.SUCCESS, PDSApp.SUCCESS_BG);
                } else if ("PLACED".equals(val)) {
                    pill = PDSApp.statusPill("  PLACED  ", PDSApp.ACCENT, PDSApp.ACCENT_BG);
                } else {
                    pill = PDSApp.statusPill("  " + val + "  ", PDSApp.TEXT_MUTED, PDSApp.SOFT_BG);
                }
                if (sel) {
                    pill.setBackground(PDSApp.ACCENT_BG);
                    pill.setForeground(PDSApp.ACCENT);
                }
                return pill;
            }
        });
        JScrollPane recentScroll = PDSApp.tableScroll(tblRecent);

        class QuotaGaugePanel extends JPanel {
            private float animProgress = 0f;
            private int quotaUsagePct = 0;
            private Timer animTimer;
            
            public QuotaGaugePanel() {
                setLayout(new BorderLayout());
                setOpaque(false);
                setBorder(new EmptyBorder(16, 16, 16, 16));
                
                JLabel lblTitle = new JLabel("Quota Usage Telemetry", SwingConstants.CENTER);
                lblTitle.setFont(PDSApp.FONT_SBOLD);
                lblTitle.setForeground(PDSApp.TEXT_SEC);
                lblTitle.setBorder(new EmptyBorder(0, 0, 4, 0));
                add(lblTitle, BorderLayout.NORTH);
                
                refreshData();
            }
            
            public void refreshData() {
                java.util.concurrent.CompletableFuture.runAsync(() -> {
                    int pct = 0;
                    try {
                        pct = AnalyticsController.getBeneficiaryQuotaUsagePct(benId);
                    } catch (SQLException ignore) {}
                    final int finalPct = pct;
                    SwingUtilities.invokeLater(() -> {
                        quotaUsagePct = finalPct;
                        if (animTimer != null) animTimer.stop();
                        animProgress = 0f;
                        animTimer = new Timer(16, new ActionListener() {
                            @Override public void actionPerformed(ActionEvent e) {
                                animProgress += (quotaUsagePct - animProgress) * 0.1f;
                                if (Math.abs(animProgress - quotaUsagePct) < 0.2f) {
                                    animProgress = quotaUsagePct;
                                    animTimer.stop();
                                }
                                repaint();
                            }
                        });
                        animTimer.start();
                    });
                });
            }

            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                g2.setColor(PDSApp.CARD_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.setColor(PDSApp.BORDER_CLR);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 16, 16));

                int titleHeight = 35;
                int size = Math.min(getWidth(), getHeight() - titleHeight) - 60;
                if (size < 60) size = 60;
                int cx = (getWidth() - size) / 2;
                int cy = titleHeight + (getHeight() - titleHeight - size) / 2 - 5;

                g2.setStroke(new BasicStroke(12f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(PDSApp.BORDER_CLR);
                g2.drawArc(cx, cy, size, size, -225, 270);

                g2.setColor(PDSApp.ACCENT);
                float angle = (animProgress / 100f) * 270f;
                g2.drawArc(cx, cy, size, size, -225, -(int)angle);

                g2.setFont(PDSApp.FONT_NUM.deriveFont(Font.BOLD, 22f));
                g2.setColor(PDSApp.TEXT_PRIMARY);
                String pctStr = String.format("%d%%", Math.round(animProgress));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(pctStr)) / 2;
                int ty = cy + (size / 2) + (fm.getAscent() / 2) - 4;
                g2.drawString(pctStr, tx, ty);

                g2.setFont(PDSApp.FONT_LABEL.deriveFont(Font.BOLD, 10f));
                g2.setColor(PDSApp.TEXT_MUTED);
                String descStr = "QUOTA UTILIZED";
                FontMetrics fmDesc = g2.getFontMetrics();
                int dx = (getWidth() - fmDesc.stringWidth(descStr)) / 2;
                g2.drawString(descStr, dx, ty + 18);

                g2.dispose();
            }
        }

        QuotaGaugePanel quotaGauge = new QuotaGaugePanel();

        JPanel bannerContainer = new JPanel(new BorderLayout());
        bannerContainer.setBackground(PAGE);

        JLabel title = new JLabel("Beneficiary Dashboard");

        Runnable load = () -> {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    BeneficiaryController.ProfileDetails details = BeneficiaryController.getProfileDetails(benId);
                    java.util.List<Object[]> remaining = BeneficiaryController.getRemainingQuotas(benId);

                    final String initials;
                    String[] parts = details.name.split("\\s+");
                    String initialsTemp = "";
                    if (parts.length > 0 && !parts[0].isEmpty()) {
                        initialsTemp += parts[0].substring(0, 1).toUpperCase();
                    }
                    if (parts.length > 1 && !parts[1].isEmpty()) {
                        initialsTemp += parts[1].substring(0, 1).toUpperCase();
                    }
                    if (initialsTemp.isEmpty()) {
                        initialsTemp = username.substring(0, Math.min(2, username.length())).toUpperCase();
                    }
                    initials = initialsTemp;

                    SwingUtilities.invokeLater(() -> {
                        title.setText("Welcome back, " + details.name);
                        lblDashboardShopLocation.setText("📍 Shop Depot: " + details.shopLocation);
                        lName.setText(details.name);
                        lCard.setText(details.rationCardNo);
                        lDob.setText(details.dob);
                        lStat.setText(details.status);
                        lCat.setText(details.categoryName);
                        lShop.setText(details.shopName);
                        lLoc.setText(details.shopLocation);
                        lCap.setText(details.shopCapacity > 0 ? details.shopCapacity + " Kg" : "—");
                        shopId = details.shopId;
                        lStat.setForeground("ACTIVE".equals(details.status) ? PDSApp.SUCCESS : PDSApp.DANGER);

                        lReqs.setText(String.valueOf(details.totalRequests));
                        lFulfilled.setText(String.valueOf(details.fulfilledRequests));
                        lPending.setText(String.valueOf(details.pendingRequests));
                        lItems.setText(String.valueOf(details.totalItemsReceived));

                        recentModel.setRowCount(0);
                        for (Object[] row : details.recentRequests) {
                            recentModel.addRow(row);
                        }
                        if (recentModel.getRowCount() == 0) {
                            recentModel.addRow(new Object[]{"—", "No requests yet", "—", "—"});
                        }

                        bannerContainer.removeAll();
                        bannerContainer.add(buildQuotaAlertBanner(remaining));
                        bannerContainer.revalidate();
                        bannerContainer.repaint();

                        quotaGauge.refreshData();

                        if (avLabel != null) {
                            avLabel.setText(initials);
                        }
                    });
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            });
        };
        pageLoaders[0] = load;
        load.run();

        JPanel topWrapper = new JPanel(new GridBagLayout());
        topWrapper.setBackground(PAGE);
        GridBagConstraints twGbc = new GridBagConstraints();
        twGbc.fill = GridBagConstraints.HORIZONTAL;
        twGbc.weightx = 1.0;
        twGbc.gridx = 0;
        twGbc.gridy = 0;
        topWrapper.add(buildHeroBanner(title, lName, lCard, lCat, lShop, lStat, load), twGbc);
        twGbc.gridy = 1;
        twGbc.insets = new Insets(14, 0, 0, 0);
        topWrapper.add(bannerContainer, twGbc);

        JPanel centerCol = new JPanel(new GridBagLayout());
        centerCol.setBackground(PAGE);
        GridBagConstraints cGbc = new GridBagConstraints();
        cGbc.fill = GridBagConstraints.BOTH;
        cGbc.weightx = 1.0;
        cGbc.gridx = 0;
        
        cGbc.gridy = 0; cGbc.weighty = 0.62; cGbc.insets = new Insets(0, 0, 14, 0);
        centerCol.add(quotaGauge, cGbc);
        
        cGbc.gridy = 1; cGbc.weighty = 0.38; cGbc.insets = new Insets(0, 0, 0, 0);
        centerCol.add(buildHelpdeskWidget(), cGbc);

        JPanel bottomRow = new JPanel(new GridLayout(1,3,16,0));
        bottomRow.setBackground(PAGE);
        JPanel leftCol = new JPanel(new BorderLayout(0,6)); leftCol.setBackground(PAGE);
        leftCol.add(PDSApp.sectionLabel("Profile Details"), BorderLayout.NORTH);
        leftCol.add(tiles, BorderLayout.CENTER);
        JPanel rightCol = new JPanel(new BorderLayout(0,6)); rightCol.setBackground(PAGE);
        rightCol.add(PDSApp.sectionLabel("Recent Requests"), BorderLayout.NORTH);
        rightCol.add(recentScroll, BorderLayout.CENTER);
        
        bottomRow.add(leftCol); 
        bottomRow.add(centerCol);
        bottomRow.add(rightCol);

        kpis.setPreferredSize(new Dimension(0, 120));
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.setBackground(PAGE);
        northPanel.add(topWrapper);
        northPanel.add(Box.createVerticalStrut(16));
        northPanel.add(kpis);

        p.add(northPanel, BorderLayout.NORTH);
        p.add(bottomRow, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildHelpdeskWidget() {
        JPanel card = new JPanel(new BorderLayout(0, 6)) {
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
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        
        JLabel lblTitle = new JLabel("SUPPORT HELPDESK");
        lblTitle.setFont(PDSApp.FONT_SBOLD.deriveFont(10f));
        lblTitle.setForeground(PDSApp.TEXT_MUTED);
        
        JLabel lblPhone = new JLabel("📞  1800-11-4900 (Toll Free)");
        lblPhone.setFont(PDSApp.FONT_LABEL.deriveFont(Font.BOLD, 12f));
        lblPhone.setForeground(PDSApp.ACCENT);
        
        JLabel lblGps = lblDashboardShopLocation;
        lblGps.setFont(PDSApp.FONT_SMALL);
        lblGps.setForeground(PDSApp.TEXT_SEC);
        
        card.add(lblTitle, BorderLayout.NORTH);
        JPanel body = new JPanel(new GridLayout(2, 1, 4, 4));
        body.setOpaque(false);
        body.add(lblPhone);
        body.add(lblGps);
        card.add(body, BorderLayout.CENTER);
        
        return card;
    }

    private JButton actionBtn(String text, String symbol) {
        JButton b = new JButton(symbol + "   " + text);
        b.setFont(PDSApp.FONT_BOLD.deriveFont(13f));
        b.setForeground(Color.WHITE);
        b.setBackground(new Color(255, 255, 255, 14));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBorder(new javax.swing.border.LineBorder(new Color(255, 255, 35), 1, true));
        return b;
    }

    private JPanel buildHeroBanner(JLabel title, JLabel lName, JLabel lCard, JLabel lCat, JLabel lShop, JLabel lStat, Runnable onRefresh) {
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
                    java.io.File f = new java.io.File("resources/beneficiarherbanner.png");
                    if (!f.exists()) {
                        f = new java.io.File("resources/beneficiaryherobanner.png");
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

                // Draw illustration with dynamic floating/bobbing effect
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
                    
                    g2.setColor(new Color(0xE7E0D5));
                    g2.fillRoundRect(cx + 25, cy - 10, 25, 30, 8, 8);
                    g2.setColor(new Color(0xA8A29E));
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(cx + 25, cy - 10, 25, 30, 8, 8);
                    g2.setColor(new Color(0x78350F));
                    g2.drawLine(cx + 25, cy - 4, cx + 50, cy - 4);
                    
                    g2.setColor(new Color(0xFDBA74));
                    g2.fillOval(cx - 25, cy - 25, 20, 20);
                    g2.setColor(new Color(0xEA580C));
                    g2.fillRoundRect(cx - 33, cy - 3, 36, 25, 8, 8);
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

        JLabel sysLbl = new JLabel("NATIONAL FOOD SECURITY PORTAL");
        sysLbl.setFont(new Font(FAM, Font.BOLD, 13));
        sysLbl.setForeground(PDSApp.ACCENT);
        leftCol.add(sysLbl, leftGbc);

        leftGbc.gridy++;
        leftGbc.insets = new Insets(4, 0, 0, 0);
        title.setFont(PDSApp.FONT_H1.deriveFont(Font.BOLD, 30f));
        title.setForeground(PDSApp.TEXT_PRIMARY);
        leftCol.add(title, leftGbc);

        leftGbc.gridy++;
        leftGbc.insets = new Insets(2, 0, 0, 0);
        JLabel sub = new JLabel("View your ration card details and entitlement information");
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

        JPanel card1 = PDSApp.statCard("Ration Card No.", lCard, PDSApp.ACCENT, false);
        JPanel card2 = PDSApp.statCard("Category", lCat, PDSApp.WARNING, false);
        JPanel card3 = PDSApp.statCard("Assigned Shop", lShop, PDSApp.SUCCESS, false);
        JPanel card4 = PDSApp.statCard("Card Status", lStat, PDSApp.ACCENT, false);

        card1.setPreferredSize(new Dimension(0, 120));
        card2.setPreferredSize(new Dimension(0, 120));
        card3.setPreferredSize(new Dimension(0, 120));
        card4.setPreferredSize(new Dimension(0, 120));

        card1.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card1.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { activateNav(4); }
        });
        card2.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card2.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { activateNav(1); }
        });
        card3.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card3.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { activateNav(4); }
        });
        card4.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card4.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { activateNav(4); }
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

    private JPanel buildQuota() {
        JPanel p=new JPanel(new BorderLayout(0,14));p.setBackground(PAGE);p.setBorder(new EmptyBorder(16,20,16,20));
        String[] cols={"Commodity","Unit","Month","Year","Max Allowed","Received","Remaining","Usage %"};
        JTable tbl=PDSApp.styledTable(cols);DefaultTableModel model=(DefaultTableModel)tbl.getModel();
        tbl.setRowHeight(38);

        tbl.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                l.setFont(PDSApp.FONT_LABEL);
                l.setBorder(new EmptyBorder(0, 10, 0, 10));
                if (col > 0 && col < 6) {
                    l.setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    l.setHorizontalAlignment(SwingConstants.LEFT);
                }
                if (!sel) {
                    try {
                        Object remVal = model.getValueAt(row, 6);
                        if (remVal != null && !"—".equals(remVal.toString())) {
                            int rem = Integer.parseInt(remVal.toString().trim());
                            l.setBackground(rem <= 0 ? PDSApp.DANGER_BG : PDSApp.SUCCESS_BG);
                        } else {
                            l.setBackground(row % 2 == 0 ? PDSApp.CARD_BG : PDSApp.SOFT_BG);
                        }
                    } catch (Exception ignore) {
                        l.setBackground(row % 2 == 0 ? PDSApp.CARD_BG : PDSApp.SOFT_BG);
                    }
                    l.setForeground(PDSApp.TEXT_PRIMARY);
                }
                return l;
            }
        });

        tbl.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setFont(PDSApp.FONT_BOLD);
                if (sel) {
                    l.setBackground(PDSApp.ACCENT_BG);
                    l.setForeground(PDSApp.ACCENT);
                } else {
                    try {
                        int rem = Integer.parseInt(v.toString());
                        l.setForeground(rem <= 0 ? PDSApp.DANGER : rem <= 2 ? PDSApp.WARNING : PDSApp.SUCCESS);
                        l.setBackground(rem <= 0 ? PDSApp.DANGER_BG : PDSApp.SUCCESS_BG);
                    } catch (Exception ignored) {
                        l.setBackground(row % 2 == 0 ? PDSApp.CARD_BG : PDSApp.SOFT_BG);
                    }
                }
                return l;
            }
        });
        tbl.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer(){
            public Component getTableCellRendererComponent(JTable t,Object v,boolean sel,boolean foc,int row,int col){
                int val=0;try{val=Integer.parseInt(v.toString());}catch(Exception ignored){}
                JProgressBar bar=new JProgressBar(0,100);bar.setValue(val);
                bar.setForeground(val>=100?PDSApp.DANGER:val>=70?PDSApp.WARNING:PDSApp.SUCCESS);
                bar.setBackground(PDSApp.BORDER_CLR);bar.setBorderPainted(false);
                bar.setStringPainted(true);bar.setString(val+"%");bar.setFont(PDSApp.FONT_SBOLD);
                return bar;}});
        
        JPanel bannerContainer = new JPanel(new BorderLayout());
        bannerContainer.setBackground(PAGE);

        // Stat tiles (stock-style dashboard KPIs)
        JPanel tiles = new JPanel(new GridLayout(1, 3, 14, 0));
        tiles.setBackground(PAGE);
        tiles.setPreferredSize(new Dimension(0, 110));
        tiles.setMinimumSize(new Dimension(0, 110));
        tiles.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        tiles.add(PDSApp.statCard("Total Allocated Items", lblTotalAlloc, PDSApp.ACCENT));
        tiles.add(PDSApp.statCard("LOW / Exhausted Quota",   lblExhausted,  PDSApp.DANGER));
        tiles.add(PDSApp.statCard("Available Quotas",        lblAvailable,  PDSApp.SUCCESS));

        Runnable load = () -> {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    List<Object[]> records = BeneficiaryController.getQuotaRecords(benId);
                    List<Object[]> remaining = BeneficiaryController.getRemainingQuotas(benId);
                    
                    int totalTemp = 0;
                    int exhaustedTemp = 0;
                    int availableTemp = 0;
                    for (Object[] row : records) {
                        if (row[6] != null) {
                            try {
                                int rem = Integer.parseInt(row[6].toString().trim());
                                totalTemp++;
                                if (rem <= 0) {
                                    exhaustedTemp++;
                                } else {
                                    availableTemp++;
                                }
                            } catch (Exception ignore) {}
                        }
                    }
                    final int finalTotal = totalTemp;
                    final int finalExhausted = exhaustedTemp;
                    final int finalAvailable = availableTemp;
                    
                    SwingUtilities.invokeLater(() -> {
                        model.setRowCount(0);
                        for (Object[] row : records) {
                            model.addRow(row);
                        }
                        lblTotalAlloc.setText(finalTotal + " items");
                        lblExhausted.setText(finalExhausted + " items");
                        lblAvailable.setText(finalAvailable + " items");

                        if (model.getRowCount() == 0) {
                            model.addRow(new Object[]{"No quota records", "—", "—", "—", "—", "—", "—", 0});
                            lblTotalAlloc.setText("0 items");
                            lblExhausted.setText("0 items");
                            lblAvailable.setText("0 items");
                        }
                        bannerContainer.removeAll();
                        bannerContainer.add(buildQuotaAlertBanner(remaining));
                        bannerContainer.revalidate();
                        bannerContainer.repaint();
                    });
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            });
        };
        JButton btnRef=PDSApp.secondaryBtn("⟳  Refresh");btnRef.addActionListener(e->load.run());
        JPanel top=new JPanel(new BorderLayout());top.setBackground(PAGE);top.add(PDSApp.pageHeader("My Monthly Quota"),BorderLayout.WEST);top.add(btnRef,BorderLayout.EAST);top.setBorder(new EmptyBorder(0,0,14,0));
        
        JPanel topWrapper = new JPanel(new GridBagLayout());
        topWrapper.setBackground(PAGE);
        GridBagConstraints twGbc = new GridBagConstraints();
        twGbc.fill = GridBagConstraints.HORIZONTAL;
        twGbc.weightx = 1.0;
        twGbc.gridx = 0;
        
        twGbc.gridy = 0;
        twGbc.insets = new Insets(0, 0, 14, 0);
        topWrapper.add(top, twGbc);
        
        twGbc.gridy = 1;
        twGbc.insets = new Insets(0, 0, 16, 0);
        topWrapper.add(tiles, twGbc);
        
        twGbc.gridy = 2;
        twGbc.insets = new Insets(0, 0, 0, 0);
        topWrapper.add(bannerContainer, twGbc);

        pageLoaders[1] = load;
        p.add(topWrapper,BorderLayout.NORTH);p.add(PDSApp.tableScroll(tbl),BorderLayout.CENTER);return p;
    }

    private void showSuccessDialog(int reqId) {
        JDialog dialog = new JDialog(this, "Success", true);
        dialog.setUndecorated(true);
        dialog.setSize(420, 260);
        dialog.setLocationRelativeTo(this);

        JPanel p = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xDCFCE7)); // soft green bg
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 24, 24));
                g2.setColor(PDSApp.SUCCESS);
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 24, 24));
                g2.dispose();
            }
        };
        p.setBorder(new EmptyBorder(24, 24, 24, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel lblCheck = new JLabel("✔", SwingConstants.CENTER);
        lblCheck.setFont(new Font("SansSerif", Font.BOLD, 52));
        lblCheck.setForeground(PDSApp.SUCCESS);
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 0, 10, 0);
        p.add(lblCheck, gbc);

        JLabel lblTitle = new JLabel("Request Placed Successfully!", SwingConstants.CENTER);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblTitle.setForeground(PDSApp.TEXT_PRIMARY);
        gbc.gridy = 1;
        gbc.insets = new Insets(5, 0, 5, 0);
        p.add(lblTitle, gbc);

        JLabel lblDesc = new JLabel("Request Ticket ID: #" + reqId, SwingConstants.CENTER);
        lblDesc.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblDesc.setForeground(PDSApp.ACCENT);
        gbc.gridy = 2;
        gbc.insets = new Insets(5, 0, 15, 0);
        p.add(lblDesc, gbc);

        JButton btnClose = PDSApp.primaryBtn("Dismiss");
        btnClose.setBackground(PDSApp.SUCCESS);
        btnClose.addActionListener(e -> dialog.dispose());
        gbc.gridy = 3;
        gbc.insets = new Insets(10, 80, 10, 80);
        p.add(btnClose, gbc);

        dialog.setContentPane(p);
        dialog.setBackground(new Color(0, 0, 0, 0));
        dialog.setVisible(true);
    }

    private JPanel buildRequest() {
        JPanel p=new JPanel(new BorderLayout(0,14));p.setBackground(PAGE);p.setBorder(new EmptyBorder(16,20,16,20));
        JComboBox<String> cbSlot=PDSApp.styledCombo();cbSlot.setPreferredSize(new Dimension(280,36));
        List<Integer> slotIds=new ArrayList<>(),comIds=new ArrayList<>();List<String> comNames=new ArrayList<>(),comUnits=new ArrayList<>();List<JSpinner> spinners=new ArrayList<>();
        JPanel comPanel=new JPanel();comPanel.setLayout(new BoxLayout(comPanel,BoxLayout.Y_AXIS));comPanel.setBackground(PDSApp.CARD_BG);
        JScrollPane spCom=new JScrollPane(comPanel);spCom.setBorder(new LineBorder(PDSApp.BORDER_CLR,1,true));spCom.setPreferredSize(new Dimension(0,220));
        JLabel lblMsg=new JLabel(" ");lblMsg.setFont(PDSApp.FONT_LABEL);
        JButton btnSubmit=PDSApp.primaryBtn("Submit Request");

        QuotaUsageChart chart = new QuotaUsageChart();

        Runnable reload = () -> {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                BeneficiaryController.ProfileDetails profile = null;
                try {
                    profile = BeneficiaryController.getProfileDetails(benId);
                } catch (Exception ignore) {}

                boolean exhausted = false;
                try {
                    exhausted = BeneficiaryController.isQuotaExhausted(benId);
                } catch (Exception ignore) {}

                BeneficiaryController.FormData data = null;
                List<Object[]> remaining = null;
                if (!exhausted) {
                    try {
                        data = BeneficiaryController.getFormData();
                        remaining = BeneficiaryController.getRemainingQuotas(benId);
                    } catch (Exception ignore) {}
                }

                final BeneficiaryController.ProfileDetails finalProfile = profile;
                final boolean finalExhausted = exhausted;
                final BeneficiaryController.FormData finalData = data;
                final List<Object[]> finalRemaining = remaining;

                SwingUtilities.invokeLater(() -> {
                    cbSlot.removeAllItems();
                    slotIds.clear();
                    comIds.clear();
                    comNames.clear();
                    comUnits.clear();
                    spinners.clear();
                    comPanel.removeAll();

                    if (finalProfile != null) {
                        lblReqShopName.setText(finalProfile.shopName);
                        lblReqCategory.setText(finalProfile.categoryName);
                        lblReqTotalVisits.setText(String.valueOf(finalProfile.totalRequests));
                        lblReqPendingVisits.setText(String.valueOf(finalProfile.pendingRequests));
                        lblReqFulfilledVisits.setText(String.valueOf(finalProfile.fulfilledRequests));
                        lblReqTotalItems.setText(String.valueOf(finalProfile.totalItemsReceived));
                    }

                    if (finalExhausted) {
                        lblMsg.setText("✗ Quota exhausted.");
                        lblMsg.setForeground(PDSApp.DANGER);
                        btnSubmit.setEnabled(false);
                        cbSlot.setEnabled(false);
                        
                        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 20));
                        row.setBackground(PDSApp.CARD_BG);
                        JLabel warn = new JLabel("Quota exhausted. It will be refilled next month on the 1st.");
                        warn.setFont(PDSApp.FONT_BOLD);
                        warn.setForeground(PDSApp.DANGER);
                        row.add(warn);
                        comPanel.add(row);
                        chart.setData(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
                    } else {
                        lblMsg.setText(" ");
                        lblMsg.setForeground(PDSApp.TEXT_PRIMARY);
                        btnSubmit.setEnabled(true);
                        cbSlot.setEnabled(true);
                        
                        if (finalData != null && finalRemaining != null) {
                            for (int i = 0; i < finalData.slotIds.size(); i++) {
                                slotIds.add(finalData.slotIds.get(i));
                                cbSlot.addItem(finalData.slotLabels.get(i));
                            }
                            
                            List<String> chartNames = new ArrayList<>();
                            List<Integer> chartMaxes = new ArrayList<>();
                            List<Integer> chartRemaining = new ArrayList<>();

                            for (int i = 0; i < finalData.comIds.size(); i++) {
                                int comId = finalData.comIds.get(i);
                                String name = finalData.comNames.get(i);
                                String unit = finalData.comUnits.get(i);
                                comIds.add(comId);
                                comNames.add(name);
                                comUnits.add(unit);
                                
                                int rem = -1;
                                int maxQty = 0;
                                for (Object[] r : finalRemaining) {
                                    if ((int) r[0] == comId) {
                                        maxQty = (int) r[3];
                                        rem = (int) r[5];
                                        break;
                                    }
                                }
                                
                                chartNames.add(name);
                                chartMaxes.add(maxQty);
                                chartRemaining.add(rem);
                                
                                int spinnerMax = (rem == -1) ? 99 : rem;
                                JSpinner sp = PDSApp.styledSpinner(0, spinnerMax);
                                spinners.add(sp);
                                sp.addChangeListener(ce -> chart.repaint());

                                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
                                row.setBackground(PDSApp.CARD_BG);
                                String quotaStr = (rem == -1) ? "[No Limit]" : "[Remaining Quota: " + rem + " " + unit + "]";
                                JLabel lbl = new JLabel(name + "  (" + unit + ")   —   " + quotaStr);
                                lbl.setFont(PDSApp.FONT_LABEL);
                                lbl.setPreferredSize(new Dimension(380, 28));
                                row.add(lbl);
                                row.add(sp);
                                comPanel.add(row);
                            }
                            chart.setData(chartNames, chartMaxes, chartRemaining, spinners);
                        }
                    }
                    comPanel.revalidate();
                    comPanel.repaint();
                });
            });
        };
        pageLoaders[2] = reload;
        JButton btnReset=PDSApp.secondaryBtn("⟳  Refresh");
        btnReset.addActionListener(e->reload.run());
        btnSubmit.addActionListener(e->{
            if(slotIds.isEmpty()){PDSApp.showAlert(p,"No time slots available.",true);return;}
            if(comIds.isEmpty()){PDSApp.showAlert(p,"No commodities available.",true);return;}
            if(shopId==-1){PDSApp.showAlert(p,"Shop not assigned.",true);return;}
            if(spinners.stream().noneMatch(s->(int)s.getValue()>0)){PDSApp.showAlert(p,"Enter quantity for at least one commodity.",true);return;}
            try{
                int slotId = slotIds.get(cbSlot.getSelectedIndex());
                List<Integer> quantities = new ArrayList<>();
                for (JSpinner sp : spinners) {
                    quantities.add((int) sp.getValue());
                }
                int reqId = BeneficiaryController.submitRequest(benId, shopId, slotId, comIds, quantities);
                lblMsg.setText("✔  Request #"+reqId+" placed!");
                lblMsg.setForeground(PDSApp.SUCCESS);
                spinners.forEach(s->s.setValue(0));
                
                showSuccessDialog(reqId);
                
                reload.run();
            }catch(SQLException ex){
                lblMsg.setText("✗  "+ex.getMessage());
                lblMsg.setForeground(PDSApp.DANGER);
            }
        });
        
        JPanel form=PDSApp.card();
        form.setLayout(new GridBagLayout());
        GridBagConstraints fGbc = new GridBagConstraints();
        fGbc.fill = GridBagConstraints.HORIZONTAL;
        fGbc.weightx = 1.0;
        fGbc.gridx = 0;

        JPanel sr=new JPanel(new FlowLayout(FlowLayout.LEFT,10,4));
        sr.setBackground(PDSApp.CARD_BG);
        sr.add(PDSApp.formLabel("Time Slot:"));
        sr.add(cbSlot);

        JPanel formCards = new JPanel(new GridLayout(1, 4, 12, 0));
        formCards.setBackground(PDSApp.CARD_BG);
        formCards.setPreferredSize(new Dimension(0, 95));
        formCards.setMinimumSize(new Dimension(0, 95));
        formCards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 95));
        formCards.add(PDSApp.statCard("Total Requests", lblReqTotalVisits, PDSApp.ACCENT, false));
        formCards.add(PDSApp.statCard("Fulfilled", lblReqFulfilledVisits, PDSApp.SUCCESS, false));
        formCards.add(PDSApp.statCard("Pending", lblReqPendingVisits, PDSApp.WARNING, false));
        formCards.add(PDSApp.statCard("Items Received", lblReqTotalItems, PDSApp.INFO, false));

        JPanel br=new JPanel(new FlowLayout(FlowLayout.LEFT,10,4));
        br.setBackground(PDSApp.CARD_BG);
        br.add(btnSubmit);
        br.add(lblMsg);

        // Lay out components inside form card (Cards at top, time slot below them, items list below time slot)
        fGbc.gridy = 0;
        fGbc.insets = new Insets(0, 0, 16, 0);
        form.add(formCards, fGbc);

        fGbc.gridy = 1;
        fGbc.insets = new Insets(0, 0, 14, 0);
        form.add(sr, fGbc);

        fGbc.gridy = 2;
        fGbc.insets = new Insets(0, 0, 6, 0);
        form.add(PDSApp.sectionLabel("Select Commodities & Quantities"), fGbc);

        fGbc.gridy = 3;
        fGbc.fill = GridBagConstraints.BOTH;
        fGbc.weighty = 1.0;
        fGbc.insets = new Insets(0, 0, 12, 0);
        form.add(spCom, fGbc);

        fGbc.gridy = 4;
        fGbc.fill = GridBagConstraints.HORIZONTAL;
        fGbc.weighty = 0.0;
        fGbc.insets = new Insets(0, 0, 0, 0);
        form.add(br, fGbc);
        
        // Left Column Header Cards (Registered Shop + Category) to fill whitespace
        JPanel leftHeaderCards = new JPanel(new GridLayout(1, 2, 12, 0));
        leftHeaderCards.setBackground(PAGE);
        leftHeaderCards.setPreferredSize(new Dimension(0, 100));
        leftHeaderCards.setMinimumSize(new Dimension(0, 100));
        leftHeaderCards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        leftHeaderCards.add(PDSApp.statCard("Registered Shop", lblReqShopName, PDSApp.ACCENT));
        leftHeaderCards.add(PDSApp.statCard("Ration Category", lblReqCategory, PDSApp.WARNING));

        JPanel leftContainer = new JPanel(new BorderLayout(0, 14));
        leftContainer.setBackground(PAGE);
        leftContainer.add(leftHeaderCards, BorderLayout.NORTH);
        leftContainer.add(form, BorderLayout.CENTER);

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(PAGE);
        GridBagConstraints wGbc = new GridBagConstraints();
        wGbc.fill = GridBagConstraints.BOTH;
        wGbc.weighty = 1.0;

        // Left Container (60% width)
        wGbc.gridx = 0; wGbc.weightx = 0.60;
        wGbc.insets = new Insets(0, 0, 0, 0);
        wrapper.add(leftContainer, wGbc);

        // Right Chart Card (40% width)
        wGbc.gridx = 1; wGbc.weightx = 0.40;
        wGbc.insets = new Insets(0, 16, 0, 0);
        wrapper.add(chart, wGbc);

        JPanel top=new JPanel(new BorderLayout());top.setBackground(PAGE);top.add(PDSApp.pageHeader("Place Ration Request"),BorderLayout.WEST);top.add(btnReset,BorderLayout.EAST);top.setBorder(new EmptyBorder(0,0,14,0));
        p.add(top,BorderLayout.NORTH);p.add(wrapper,BorderLayout.CENTER);return p;
    }

    private JPanel buildHistory() {
        JPanel p=new JPanel(new BorderLayout(0,14));p.setBackground(PAGE);p.setBorder(new EmptyBorder(16,20,16,20));
        JTable tbl=PDSApp.styledTable(new String[]{"Req ID","Date","Slot Date","Start Time","Items","Status"});
        DefaultTableModel model=(DefaultTableModel)tbl.getModel();
        tbl.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                String val = (v != null) ? v.toString() : "";
                JLabel pill;
                if ("FULFILLED".equals(val)) {
                    pill = PDSApp.statusPill("  FULFILLED  ", PDSApp.SUCCESS, PDSApp.SUCCESS_BG);
                } else if ("PLACED".equals(val)) {
                    pill = PDSApp.statusPill("  PLACED  ", PDSApp.ACCENT, PDSApp.ACCENT_BG);
                } else {
                    pill = PDSApp.statusPill("  " + val + "  ", PDSApp.TEXT_MUTED, PDSApp.SOFT_BG);
                }
                if (sel) {
                    pill.setBackground(PDSApp.ACCENT_BG);
                    pill.setForeground(PDSApp.ACCENT);
                }
                return pill;
            }
        });
        Runnable load = () -> {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    List<Object[]> history = BeneficiaryController.getRequestHistory(benId);
                    SwingUtilities.invokeLater(() -> {
                        model.setRowCount(0);
                        for (Object[] row : history) {
                            model.addRow(row);
                        }
                    });
                } catch(SQLException ex) {
                    ex.printStackTrace();
                }
            });
        };
        JButton btnRef=PDSApp.secondaryBtn("⟳  Refresh");btnRef.addActionListener(e->load.run());
        JPanel top=new JPanel(new BorderLayout());top.setBackground(PAGE);top.add(PDSApp.pageHeader("Request History"),BorderLayout.WEST);top.add(btnRef,BorderLayout.EAST);top.setBorder(new EmptyBorder(0,0,14,0));
        pageLoaders[3] = load;
        p.add(top,BorderLayout.NORTH);p.add(PDSApp.tableScroll(tbl),BorderLayout.CENTER);return p;
    }

    private JPanel buildQuotaAlertBanner(List<Object[]> remaining) {
        JPanel banner = new JPanel(new BorderLayout());
        banner.setOpaque(false);
        if (remaining != null) {
            List<String> exhausted = new ArrayList<>();
            for (Object[] r : remaining) {
                int rem = (int) r[5];
                if (rem <= 0) {
                    exhausted.add((String) r[1]);
                }
            }
            if (!exhausted.isEmpty()) {
                String msg = "⚠️ Alert: Monthly allocation quota exhausted for: " + String.join(", ", exhausted) + ". Next allocation resets on the 1st of next month.";
                JLabel lbl = new JLabel(msg);
                lbl.setFont(new Font(FAM, Font.BOLD, 12));
                lbl.setForeground(PDSApp.DANGER);
                lbl.setBackground(PDSApp.DANGER_BG);
                lbl.setOpaque(true);
                lbl.setBorder(new EmptyBorder(10, 16, 10, 16));
                banner.add(lbl, BorderLayout.CENTER);
                banner.setBorder(new EmptyBorder(0, 0, 14, 0));
            } else {
                JLabel lbl = new JLabel("✓ Active Account allocations are fully verified and within active limits.");
                lbl.setFont(new Font(FAM, Font.PLAIN, 12));
                lbl.setForeground(PDSApp.SUCCESS);
                lbl.setBackground(PDSApp.SUCCESS_BG);
                lbl.setOpaque(true);
                lbl.setBorder(new EmptyBorder(10, 16, 10, 16));
                banner.add(lbl, BorderLayout.CENTER);
                banner.setBorder(new EmptyBorder(0, 0, 14, 0));
            }
        }
        return banner;
    }

    private JPanel buildAbout() {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setBackground(PAGE);
        p.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(PAGE);
        JLabel headerLabel = PDSApp.pageHeader("About My Profile & Shop Registry");
        headerLabel.setFont(PDSApp.FONT_H1.deriveFont(Font.BOLD, 28f));
        top.add(headerLabel, BorderLayout.WEST);
        top.setBorder(new EmptyBorder(0, 0, 14, 0));

        JLabel lCardStatus = new JLabel("—");
        JLabel lCardCategory = new JLabel("—");
        JLabel lCardNumber = new JLabel("—");
        JLabel lCardName = new JLabel("—");

        JLabel lName = new JLabel("—");
        JLabel lCard = new JLabel("—");
        JLabel lDob = new JLabel("—");
        JLabel lCat = new JLabel("—");
        JLabel lStat = new JLabel("—");

        // Single grouped cards deck (Status + Category + Card No + Name)
        JPanel formCards = new JPanel(new GridLayout(1, 4, 12, 0));
        formCards.setBackground(PAGE);
        formCards.setPreferredSize(new Dimension(0, 95));
        formCards.setMinimumSize(new Dimension(0, 95));
        formCards.setMaximumSize(new Dimension(Integer.MAX_VALUE, 95));
        formCards.add(PDSApp.statCard("Card Status", lCardStatus, PDSApp.SUCCESS, false));
        formCards.add(PDSApp.statCard("Category", lCardCategory, PDSApp.WARNING, false));
        formCards.add(PDSApp.statCard("Ration Card No.", lCardNumber, PDSApp.ACCENT, false));
        formCards.add(PDSApp.statCard("Registered Identity", lCardName, PDSApp.INFO, false));

        JPanel dossierCard = PDSApp.card();
        dossierCard.setLayout(new GridBagLayout());
        dossierCard.setBorder(new EmptyBorder(20, 24, 24, 24));
        GridBagConstraints dGbc = new GridBagConstraints();
        dGbc.fill = GridBagConstraints.HORIZONTAL;
        dGbc.weightx = 1.0; dGbc.gridx = 0; dGbc.gridy = 0;

        Font customSectionFont = new Font(PDSApp.FONT_LABEL.getFamily(), Font.BOLD, 12);
        Font customLabelFont = new Font(PDSApp.FONT_LABEL.getFamily(), Font.BOLD, 13);
        Font customValFont = new Font(PDSApp.FONT_LABEL.getFamily(), Font.PLAIN, 16);

        dGbc.insets = new Insets(0, 0, 10, 0);
        JLabel secProfile = PDSApp.sectionLabel("Profile Registration Records");
        secProfile.setFont(customSectionFont);
        dossierCard.add(secProfile, dGbc);

        dGbc.gridy++;
        dGbc.insets = new Insets(0, 0, 2, 0);
        JLabel lblName = PDSApp.formLabel("Full Registered Identity Name");
        lblName.setFont(customLabelFont);
        dossierCard.add(lblName, dGbc);
        dGbc.gridy++;
        dGbc.insets = new Insets(0, 0, 14, 0);
        lName.setFont(customValFont);
        dossierCard.add(lName, dGbc);

        dGbc.gridy++;
        dGbc.insets = new Insets(0, 0, 2, 0);
        JLabel lblCard = PDSApp.formLabel("Ration Card Verification Code");
        lblCard.setFont(customLabelFont);
        dossierCard.add(lblCard, dGbc);
        dGbc.gridy++;
        dGbc.insets = new Insets(0, 0, 14, 0);
        lCard.setFont(customValFont);
        dossierCard.add(lCard, dGbc);

        dGbc.gridy++;
        dGbc.insets = new Insets(0, 0, 2, 0);
        JLabel lblDob = PDSApp.formLabel("Date of Birth Dossier");
        lblDob.setFont(customLabelFont);
        dossierCard.add(lblDob, dGbc);
        dGbc.gridy++;
        dGbc.insets = new Insets(0, 0, 14, 0);
        lDob.setFont(customValFont);
        dossierCard.add(lDob, dGbc);

        dGbc.gridy++;
        dGbc.insets = new Insets(0, 0, 2, 0);
        JLabel lblCat = PDSApp.formLabel("Benefit Category Classification");
        lblCat.setFont(customLabelFont);
        dossierCard.add(lblCat, dGbc);
        dGbc.gridy++;
        dGbc.insets = new Insets(0, 0, 14, 0);
        lCat.setFont(customValFont);
        dossierCard.add(lCat, dGbc);

        dGbc.gridy++;
        dGbc.insets = new Insets(0, 0, 2, 0);
        JLabel lblStat = PDSApp.formLabel("Benefit Dossier Status Code");
        lblStat.setFont(customLabelFont);
        dossierCard.add(lblStat, dGbc);
        dGbc.gridy++;
        dGbc.insets = new Insets(0, 0, 0, 0);
        lStat.setFont(customValFont);
        dossierCard.add(lStat, dGbc);

        // Vertical glue spacer at the bottom
        dGbc.gridy++;
        dGbc.fill = GridBagConstraints.BOTH;
        dGbc.weighty = 1.0;
        dossierCard.add(Box.createVerticalGlue(), dGbc);

        JPanel leftContainer = new JPanel(new GridBagLayout());
        leftContainer.setBackground(PAGE);
        GridBagConstraints lcGbc = new GridBagConstraints();
        lcGbc.fill = GridBagConstraints.BOTH;
        lcGbc.weightx = 1.0;

        lcGbc.gridx = 0;
        lcGbc.gridy = 0;
        lcGbc.weighty = 0.0;
        lcGbc.insets = new Insets(0, 0, 16, 0);
        leftContainer.add(formCards, lcGbc);

        lcGbc.gridy = 1;
        lcGbc.weighty = 1.0;
        lcGbc.insets = new Insets(0, 0, 0, 0);
        leftContainer.add(dossierCard, lcGbc);

        class ShopCapacityGaugePanel extends JPanel {
            private float animProgress = 0f;
            private int usagePct = 0;
            private Timer animTimer;

            public ShopCapacityGaugePanel() {
                setLayout(new BorderLayout());
                setOpaque(false);
                setPreferredSize(new Dimension(0, 160));
            }

            public void setUsagePct(int pct) {
                this.usagePct = pct;
                if (animTimer != null) animTimer.stop();
                animProgress = 0f;
                animTimer = new Timer(16, e -> {
                    animProgress += (usagePct - animProgress) * 0.1f;
                    if (Math.abs(animProgress - usagePct) < 0.2f) {
                        animProgress = usagePct;
                        animTimer.stop();
                    }
                    repaint();
                });
                animTimer.start();
            }

            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int size = Math.min(getWidth(), getHeight()) - 40;
                if (size < 60) size = 60;
                int cx = (getWidth() - size) / 2;
                int cy = (getHeight() - size) / 2 - 5;

                g2.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(PDSApp.BORDER_CLR);
                g2.drawArc(cx, cy, size, size, -225, 270);

                Color arcColor = PDSApp.SUCCESS;
                if (animProgress >= 85f) arcColor = PDSApp.DANGER;
                else if (animProgress >= 50f) arcColor = PDSApp.WARNING;

                g2.setColor(arcColor);
                float angle = (animProgress / 100f) * 270f;
                g2.drawArc(cx, cy, size, size, -225, -(int)angle);

                g2.setFont(PDSApp.FONT_NUM.deriveFont(Font.BOLD, 18f));
                g2.setColor(PDSApp.TEXT_PRIMARY);
                String pctStr = String.format("%d%%", Math.round(animProgress));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(pctStr)) / 2;
                int ty = cy + (size / 2) + (fm.getAscent() / 2) - 4;
                g2.drawString(pctStr, tx, ty);

                g2.setFont(PDSApp.FONT_LABEL.deriveFont(Font.BOLD, 9f));
                g2.setColor(PDSApp.TEXT_MUTED);
                String descStr = "VOLUMETRIC LOAD";
                FontMetrics fmDesc = g2.getFontMetrics();
                int dx = (getWidth() - fmDesc.stringWidth(descStr)) / 2;
                g2.drawString(descStr, dx, ty + 14);

                g2.dispose();
            }
        }

        JLabel lsName = new JLabel("—");
        JLabel lsLoc = new JLabel("—");
        JLabel lsCap = new JLabel("—");
        JLabel lsId = new JLabel("—");
        JLabel lsLoad = new JLabel("—");
        ShopCapacityGaugePanel capacityGauge = new ShopCapacityGaugePanel();

        JPanel shopCard = PDSApp.card();
        shopCard.setLayout(new GridBagLayout());
        shopCard.setBorder(new EmptyBorder(20, 24, 24, 24));
        GridBagConstraints sGbc = new GridBagConstraints();
        sGbc.fill = GridBagConstraints.HORIZONTAL;
        sGbc.weightx = 1.0; sGbc.gridx = 0; sGbc.gridy = 0;

        JLabel secShop = PDSApp.sectionLabel("Assigned Fair Price Shop");
        secShop.setFont(customSectionFont);
        shopCard.add(secShop, sGbc);
        
        sGbc.gridy++;
        sGbc.fill = GridBagConstraints.BOTH;
        sGbc.weighty = 0.0;
        sGbc.insets = new Insets(8, 0, 12, 0);
        shopCard.add(capacityGauge, sGbc);

        sGbc.fill = GridBagConstraints.HORIZONTAL;
        sGbc.weighty = 0.0;
        sGbc.gridy++;
        sGbc.insets = new Insets(0, 0, 8, 0);
        JLabel secRegistry = PDSApp.sectionLabel("Depot Registry Telemetry");
        secRegistry.setFont(customSectionFont);
        shopCard.add(secRegistry, sGbc);

        sGbc.gridy++;
        sGbc.insets = new Insets(0, 0, 2, 0);
        JLabel lblShopName = PDSApp.formLabel("Assigned Outlet Name");
        lblShopName.setFont(customLabelFont);
        shopCard.add(lblShopName, sGbc);
        sGbc.gridy++;
        sGbc.insets = new Insets(0, 0, 12, 0);
        lsName.setFont(customValFont);
        shopCard.add(lsName, sGbc);

        sGbc.gridy++;
        sGbc.insets = new Insets(0, 0, 2, 0);
        JLabel lblShopId = PDSApp.formLabel("Registered Outlet ID");
        lblShopId.setFont(customLabelFont);
        shopCard.add(lblShopId, sGbc);
        sGbc.gridy++;
        sGbc.insets = new Insets(0, 0, 12, 0);
        lsId.setFont(customValFont);
        shopCard.add(lsId, sGbc);

        sGbc.gridy++;
        sGbc.insets = new Insets(0, 0, 2, 0);
        JLabel lblShopLoc = PDSApp.formLabel("Official Registration Address");
        lblShopLoc.setFont(customLabelFont);
        shopCard.add(lblShopLoc, sGbc);
        sGbc.gridy++;
        sGbc.insets = new Insets(0, 0, 12, 0);
        lsLoc.setFont(customValFont);
        shopCard.add(lsLoc, sGbc);

        sGbc.gridy++;
        sGbc.insets = new Insets(0, 0, 2, 0);
        JLabel lblShopCap = PDSApp.formLabel("Monthly Outflow Capacity Threshold");
        lblShopCap.setFont(customLabelFont);
        shopCard.add(lblShopCap, sGbc);
        sGbc.gridy++;
        sGbc.insets = new Insets(0, 0, 12, 0);
        lsCap.setFont(customValFont);
        shopCard.add(lsCap, sGbc);

        sGbc.gridy++;
        sGbc.insets = new Insets(0, 0, 2, 0);
        JLabel lblShopLoad = PDSApp.formLabel("Current Monthly Volumetric Load");
        lblShopLoad.setFont(customLabelFont);
        shopCard.add(lblShopLoad, sGbc);
        sGbc.gridy++;
        sGbc.insets = new Insets(0, 0, 0, 0);
        lsLoad.setFont(customValFont);
        shopCard.add(lsLoad, sGbc);

        // Vertical glue spacer at the bottom of shopCard
        sGbc.gridy++;
        sGbc.fill = GridBagConstraints.BOTH;
        sGbc.weighty = 1.0;
        shopCard.add(Box.createVerticalGlue(), sGbc);

        Runnable load = () -> {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    BeneficiaryController.ProfileDetails details = BeneficiaryController.getProfileDetails(benId);
                    
                    int issuedQtyTemp = 0;
                    if (details.shopId != -1) {
                        Connection conn = DB.getConnection();
                        String loadSql = "SELECT COALESCE(SUM(dt.QuantityIssued), 0) " +
                                         "FROM DISTRIBUTION_TRANSACTION dt " +
                                         "JOIN REQUEST r ON r.RequestID = dt.RequestID " +
                                         "WHERE r.ShopID = ? AND MONTH(r.RequestDate) = MONTH(CURRENT_DATE()) AND YEAR(r.RequestDate) = YEAR(CURRENT_DATE())";
                        try (PreparedStatement ps = conn.prepareStatement(loadSql)) {
                            ps.setInt(1, details.shopId);
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    issuedQtyTemp = rs.getInt(1);
                                }
                            }
                        }
                    }
                    
                    final int finalIssuedQty = issuedQtyTemp;
                    int cap = details.shopCapacity;
                    int pctTemp = 0;
                    if (cap > 0) {
                        pctTemp = (int) Math.min(100, Math.round(issuedQtyTemp * 100.0 / cap));
                    }
                    final int finalPct = pctTemp;

                    SwingUtilities.invokeLater(() -> {
                        lCardStatus.setText(details.status);
                        lCardCategory.setText(details.categoryName);
                        lCardNumber.setText(details.rationCardNo);
                        lCardName.setText(details.name);

                        lName.setText(details.name);
                        lCard.setText(details.rationCardNo);
                        lDob.setText(details.dob);
                        lCat.setText(details.categoryName);
                        lStat.setText(details.status);
                        
                        lsName.setText(details.shopName);
                        lsLoc.setText(details.shopLocation);
                        lsCap.setText(details.shopCapacity + " Kg");
                        lsId.setText(String.valueOf(details.shopId));
                        
                        lsLoad.setText(finalIssuedQty + " Kg");
                        capacityGauge.setUsagePct(finalPct);
                    });
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            });
        };
        pageLoaders[4] = load;

        JPanel container = new JPanel(new GridBagLayout());
        container.setBackground(PAGE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        // Left Container (60% width)
        gbc.gridx = 0; gbc.weightx = 0.60;
        gbc.insets = new Insets(0, 0, 0, 0);
        container.add(leftContainer, gbc);

        // Right Column (40% width)
        gbc.gridx = 1; gbc.weightx = 0.40;
        gbc.insets = new Insets(0, 16, 0, 0);
        container.add(shopCard, gbc);

        p.add(top, BorderLayout.NORTH);
        p.add(container, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildTransfer() {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setBackground(PAGE);
        p.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(PAGE);
        top.add(PDSApp.pageHeader("Shop Reassignment Settings"), BorderLayout.WEST);
        top.setBorder(new EmptyBorder(0, 0, 14, 0));

        JPanel container = new JPanel(new GridBagLayout());
        container.setBackground(PAGE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        JPanel transferCard = PDSApp.card();
        transferCard.setLayout(new GridBagLayout());
        transferCard.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints tGbc = new GridBagConstraints();
        tGbc.fill = GridBagConstraints.HORIZONTAL;
        tGbc.weightx = 1.0; tGbc.gridx = 0; tGbc.gridy = 0;
        
        transferCard.add(PDSApp.sectionLabel("Assigned Shop Registration Details"), tGbc);
        tGbc.insets = new Insets(14, 0, 4, 0);
        
        JLabel lblCurrentShop = new JLabel("—"); lblCurrentShop.setFont(PDSApp.FONT_LABEL);
        JComboBox<String> cbShops = PDSApp.styledCombo(); cbShops.setPreferredSize(new Dimension(100, 36));
        List<Integer> shopIds = new ArrayList<>();
        JButton btnTransfer = PDSApp.primaryBtn("Request Reassignment Transfer");
        JLabel lblTransferMsg = new JLabel(" "); lblTransferMsg.setFont(PDSApp.FONT_LABEL);

        tGbc.gridy++; tGbc.insets = new Insets(10, 0, 4, 0); transferCard.add(PDSApp.formLabel("Active Registered Outlet"), tGbc);
        tGbc.gridy++; tGbc.insets = new Insets(0, 0, 12, 0); transferCard.add(lblCurrentShop, tGbc);
        tGbc.gridy++; tGbc.insets = new Insets(0, 0, 4, 0); transferCard.add(PDSApp.formLabel("Select Target Outlet Reassignment"), tGbc);
        tGbc.gridy++; tGbc.insets = new Insets(0, 0, 16, 0); transferCard.add(cbShops, tGbc);
        tGbc.gridy++; tGbc.insets = new Insets(8, 0, 0, 0); transferCard.add(btnTransfer, tGbc);
        tGbc.gridy++; tGbc.insets = new Insets(8, 0, 0, 0); transferCard.add(lblTransferMsg, tGbc);

        Runnable loadShops = () -> {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    BeneficiaryController.ProfileDetails details = BeneficiaryController.getProfileDetails(benId);
                    List<Object[]> shopsList = BeneficiaryController.getAvailableShops();
                    
                    List<Integer> tempShopIds = new ArrayList<>();
                    List<String> tempShopLabels = new ArrayList<>();
                    for (Object[] shop : shopsList) {
                        int sId = (int) shop[0];
                        String name = (String) shop[1];
                        String loc = (String) shop[2];
                        if (sId != details.shopId) {
                            tempShopIds.add(sId);
                            tempShopLabels.add(name + "  |  " + loc);
                        }
                    }
                    
                    SwingUtilities.invokeLater(() -> {
                        cbShops.removeAllItems();
                        shopIds.clear();
                        lblCurrentShop.setText(details.shopName + " (" + details.shopLocation + ")");
                        for (int i = 0; i < tempShopIds.size(); i++) {
                            shopIds.add(tempShopIds.get(i));
                            cbShops.addItem(tempShopLabels.get(i));
                        }
                    });
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            });
        };
        pageLoaders[5] = loadShops;

        btnTransfer.addActionListener(e -> {
            if (cbShops.getSelectedIndex() < 0) {
                lblTransferMsg.setText("✗ Select a target shop outlet first.");
                lblTransferMsg.setForeground(PDSApp.DANGER);
                return;
            }
            int newShopId = shopIds.get(cbShops.getSelectedIndex());
            try {
                BeneficiaryController.transferShop(benId, newShopId);
                lblTransferMsg.setText("✔ Reassignment request complete.");
                lblTransferMsg.setForeground(PDSApp.SUCCESS);
                shopId = newShopId;
                loadShops.run();
            } catch (SQLException ex) {
                lblTransferMsg.setText("✗ " + ex.getMessage());
                lblTransferMsg.setForeground(PDSApp.DANGER);
            }
        });

        gbc.gridx = 0; gbc.weightx = 1.0; gbc.insets = new Insets(0, 0, 0, 0);
        container.add(transferCard, gbc);

        p.add(top, BorderLayout.NORTH);
        p.add(container, BorderLayout.CENTER);
        return p;
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
        JButton btnChange = PDSApp.primaryBtn("Update Access Credentials");
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
                BeneficiaryController.changePassword(benId, oldP, newP);
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

    // ── Live Quota Visualizer Chart Component ─────────────────────────────────
    class QuotaUsageChart extends JPanel {
        private final List<String> names = new ArrayList<>();
        private final List<Integer> maxes = new ArrayList<>();
        private final List<Integer> remaining = new ArrayList<>();
        private final List<JSpinner> spinners = new ArrayList<>();
        private float sweep = 0f;
        private final Timer anim = new Timer(20, new ActionListener() {
            @Override public void actionPerformed(ActionEvent e) {
                sweep += 0.05f;
                if (sweep > 1.0f) {
                    sweep = 1.0f;
                    ((Timer) e.getSource()).stop();
                }
                repaint();
            }
        });

        public QuotaUsageChart() {
            setOpaque(false);
            anim.start();
        }

        public void setData(List<String> names, List<Integer> maxes, List<Integer> remaining, List<JSpinner> spinners) {
            this.names.clear(); this.names.addAll(names);
            this.maxes.clear(); this.maxes.addAll(maxes);
            this.remaining.clear(); this.remaining.addAll(remaining);
            this.spinners.clear(); this.spinners.addAll(spinners);
            sweep = 0f;
            anim.restart();
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Card background
            g2.setColor(PDSApp.CARD_BG);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
            
            // Outer glow border
            g2.setColor(PDSApp.BORDER_CLR);
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 16, 16));

            if (names.isEmpty()) {
                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                g2.setColor(PDSApp.TEXT_MUTED);
                g2.drawString("No quota data available", getWidth() / 2 - 70, getHeight() / 2);
                g2.dispose();
                return;
            }

            int padLeft = 24, padRight = 24, padTop = 50, padBottom = 20;
            int chartW = getWidth() - padLeft - padRight;
            int chartH = getHeight() - padTop - padBottom;

            int items = names.size();
            int rowHeight = chartH / items;

            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.setColor(PDSApp.TEXT_PRIMARY);
            g2.drawString("REAL-TIME QUOTA VISUALIZER", padLeft, 30);

            for (int i = 0; i < items; i++) {
                String name = names.get(i);
                int maxVal = maxes.get(i);
                int remVal = remaining.get(i);
                int orderVal = 0;
                if (i < spinners.size()) {
                    orderVal = (int) spinners.get(i).getValue();
                }

                int ry = padTop + (i * rowHeight) + (rowHeight - 16) / 2;

                // Label
                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                g2.setColor(PDSApp.TEXT_SEC);
                g2.drawString(name.toUpperCase(), padLeft, ry - 6);

                // Quota stats string
                String info = "Limit: " + maxVal + "  |  Rem: " + remVal + "  |  Req: " + orderVal;
                g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                g2.setColor(PDSApp.TEXT_MUTED);
                int infoW = g2.getFontMetrics().stringWidth(info);
                g2.drawString(info, getWidth() - padRight - infoW, ry - 6);

                // Background track (Max Allowed Quota)
                g2.setColor(PDSApp.SOFT_BG);
                g2.fill(new RoundRectangle2D.Float(padLeft, ry, chartW, 8, 4, 4));

                // Remaining Quota bar (Blue)
                float remRatio = maxVal > 0 ? (float) remVal / maxVal : 0f;
                int remW = (int) (chartW * remRatio * sweep);
                if (remW > 0) {
                    g2.setPaint(new GradientPaint(padLeft, ry, PDSApp.ACCENT, padLeft + remW, ry, PDSApp.ACCENT.darker()));
                    g2.fill(new RoundRectangle2D.Float(padLeft, ry, remW, 8, 4, 4));
                }

                // Current Order Selection Overlay (Amber/Orange)
                if (orderVal > 0) {
                    float orderRatio = maxVal > 0 ? (float) orderVal / maxVal : 0f;
                    int orderW = (int) (chartW * orderRatio * sweep);
                    if (orderW > 0) {
                        g2.setPaint(new GradientPaint(padLeft, ry, PDSApp.WARNING, padLeft + orderW, ry, PDSApp.WARNING.darker()));
                        g2.fill(new RoundRectangle2D.Float(padLeft, ry, orderW, 8, 4, 4));
                    }
                }
            }

            g2.dispose();
        }
    }
}
