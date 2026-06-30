import java.awt.*;
import java.awt.geom.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class DistributePanel extends JPanel {

    private JTextField    tfReqId = PDSApp.inputField(14);
    private JLabel        lblBen  = new JLabel("—");
    private JLabel        lblDate = new JLabel("—");
    private JLabel        lblStat = new JLabel("—");
    private JTable        tblItems;
    private DefaultTableModel itemModel;
    private JTextArea     taLog   = new JTextArea();
    private JButton       btnLoad, btnDistribute;

    public DistributePanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(PDSApp.PAGE_BG);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(PDSApp.PAGE_BG);
        p.setBorder(new EmptyBorder(0, 0, 22, 0));
        p.add(PDSApp.pageHeader("Distribute Items"), BorderLayout.WEST);
        JButton btnRefresh = PDSApp.secondaryBtn("⟳  Refresh");
        btnRefresh.addActionListener(e -> { tfReqId.setText(""); itemModel.setRowCount(0); taLog.setText(""); lblBen.setText("—"); lblDate.setText("—"); lblStat.setText("—"); btnDistribute.setEnabled(false); });
        p.add(btnRefresh, BorderLayout.EAST);
        return p;
    }

    private JPanel buildCenter() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(PDSApp.PAGE_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // ── Lookup card ──
        JPanel lookupCard = PDSApp.card();
        lookupCard.setLayout(new BorderLayout(0, 14));
        lookupCard.add(PDSApp.sectionLabel("Request Lookup"), BorderLayout.NORTH);
        
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        topRow.setOpaque(false);
        btnLoad       = PDSApp.primaryBtn("Load Request");
        tfReqId.addActionListener(e -> btnLoad.doClick());
        btnDistribute = new PDSApp.RippleButton("▶  Distribute  (Triggers Fire)", new Color(0x6366F1), new Color(0x4F46E5), Color.WHITE);
        btnDistribute.setFont(PDSApp.FONT_BOLD);
        btnDistribute.setBorder(new EmptyBorder(12, 24, 12, 24));
        btnDistribute.putClientProperty("RippleButton.pill", true);
        btnDistribute.setEnabled(false);
        topRow.add(PDSApp.formLabel("Request ID"));
        topRow.add(Box.createHorizontalStrut(6));
        topRow.add(tfReqId);
        topRow.add(btnLoad);
        topRow.add(Box.createHorizontalStrut(20));
        topRow.add(btnDistribute);
        lookupCard.add(topRow, BorderLayout.CENTER);

        // Request info tiles using stylish accent-colored cards
        JPanel tiles = new JPanel(new GridLayout(1, 3, 12, 0));
        tiles.setOpaque(false);
        tiles.add(stylishInfoCard("Beneficiary", lblBen, PDSApp.ACCENT));
        tiles.add(stylishInfoCard("Request Date", lblDate, PDSApp.WARNING));
        tiles.add(stylishInfoCard("Status", lblStat, PDSApp.SUCCESS));
        lookupCard.add(tiles, BorderLayout.SOUTH);

        p.add(lookupCard, gbc);

        // ── Items table card ──
        itemModel = new DefaultTableModel(
            new String[]{"Commodity ID", "Commodity", "Qty Requested", "Stock Available", "Feasible"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblItems = PDSApp.styledTable(
            new String[]{"Commodity ID", "Commodity", "Qty Requested", "Stock Available", "Feasible"});
        tblItems.setModel(itemModel);
        tblItems.getColumnModel().getColumn(0).setMaxWidth(110);
        tblItems.getColumnModel().getColumn(0).setCellRenderer(centerRenderer());
        tblItems.getColumnModel().getColumn(2).setMaxWidth(120);
        tblItems.getColumnModel().getColumn(2).setCellRenderer(centerRenderer());
        tblItems.getColumnModel().getColumn(3).setMaxWidth(130);
        tblItems.getColumnModel().getColumn(3).setCellRenderer(centerRenderer());
        tblItems.getColumnModel().getColumn(4).setMaxWidth(90);
        tblItems.getColumnModel().getColumn(4).setCellRenderer(feasibleRenderer());

        JPanel tableCard = PDSApp.card();
        tableCard.setLayout(new BorderLayout(0, 12));
        tableCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        tableCard.add(PDSApp.sectionLabel("Requested Items Ledger"), BorderLayout.NORTH);
        tableCard.add(PDSApp.tableScroll(tblItems), BorderLayout.CENTER);

        // ── Trigger log card ──
        taLog.setEditable(false);
        taLog.setFont(PDSApp.FONT_MONO);
        taLog.setBackground(Color.BLACK); // Dark console background (black colored)
        taLog.setForeground(new Color(0x10B981)); // Mint green text
        taLog.setCaretColor(Color.WHITE);
        taLog.setLineWrap(true);
        taLog.setBorder(new EmptyBorder(12, 14, 12, 14));
        JScrollPane logScroll = new JScrollPane(taLog);
        logScroll.setBorder(null);
        logScroll.getViewport().setBackground(Color.BLACK);

        JPanel logCard = PDSApp.card();
        logCard.setLayout(new BorderLayout(0, 12));
        logCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        logCard.add(PDSApp.sectionLabel("Trigger Execution Log"), BorderLayout.NORTH);
        logCard.add(logScroll, BorderLayout.CENTER);

        gbc.gridy = 1;
        gbc.weighty = 0.6;
        gbc.insets = new Insets(16, 0, 0, 0);
        p.add(tableCard, gbc);

        gbc.gridy = 2;
        gbc.weighty = 0.4;
        gbc.insets = new Insets(16, 0, 0, 0);
        p.add(logCard, gbc);

        btnLoad.addActionListener(e -> loadRequest());
        btnDistribute.addActionListener(e -> distribute());

        return p;
    }

    private void loadRequest() {
        itemModel.setRowCount(0);
        taLog.setText("");
        btnDistribute.setEnabled(false);
        String txt = tfReqId.getText().trim();
        if (txt.isEmpty()) { PDSApp.showAlert(this, "Enter a Request ID.", true); return; }
        try {
            int reqId = Integer.parseInt(txt);

            DistributeController.DistributeDetails details = DistributeController.getRequestDetails(reqId);
            if (details == null) {
                PDSApp.showAlert(this, "Request #" + reqId + " not found.", true); return;
            }

            lblBen.setText(details.header.benName);
            lblDate.setText(details.header.requestDate);
            String st = details.header.status;
            lblStat.setText(st);
            lblStat.setForeground("FULFILLED".equals(st) ? PDSApp.SUCCESS :
                                  "PLACED".equals(st)    ? PDSApp.ACCENT  : PDSApp.WARNING);

            for (Object[] row : details.items) {
                itemModel.addRow(row);
            }

            boolean alreadyFulfilled = "FULFILLED".equals(st);

            if (alreadyFulfilled) {
                taLog.setText("  [SYSTEM LEDGER VERIFICATION]\n  ✔ Request #" + reqId + " is verified and fully FULFILLED.\n  All commodities listed above have been issued.");
            }
            btnDistribute.setEnabled(true); // Keep enabled so click handler can trigger alert dialog

        } catch (NumberFormatException ex) {
            PDSApp.showAlert(this, "Request ID must be a number.", true);
        } catch (SQLException ex) {
            PDSApp.showAlert(this, ex.getMessage(), true);
        }
    }

    private void distribute() {
        taLog.setText("");
        StringBuilder log = new StringBuilder();
        boolean success = false;
        try {
            int reqId = Integer.parseInt(tfReqId.getText().trim());

            // Check if request is already fulfilled in the database
            DistributeController.DistributeDetails details = DistributeController.getRequestDetails(reqId);
            if (details != null && "FULFILLED".equalsIgnoreCase(details.header.status)) {
                taLog.setText("  [TRANSACTION BLOCKED]\n  ✘ Distribution BLOCKED — Request #" + reqId + " is already FULFILLED.\n  Re-distribution of items is not allowed.");
                PDSApp.showAlert(this, "This request has already been FULFILLED.\nDouble distribution is blocked by database integrity constraints.", true);
                return;
            }

            // Check feasibility first before touching DB
            boolean anyInfeasible = false;
            for (int i = 0; i < itemModel.getRowCount(); i++) {
                if ("NO".equals(itemModel.getValueAt(i, 4))) {
                    anyInfeasible = true;
                    String comName = (String) itemModel.getValueAt(i, 1);
                    int stock = (int) itemModel.getValueAt(i, 3);
                    int qty   = (int) itemModel.getValueAt(i, 2);
                    log.append("  [TRIGGER 1]  ✘  BLOCKED — ").append(comName).append("\n")
                       .append("               ↳  Insufficient stock: need ").append(qty)
                       .append(", available ").append(stock).append("\n\n");
                }
            }
            if (anyInfeasible) {
                log.append("─────────────────────────────────────────────────────\n");
                log.append("  Distribution BLOCKED — insufficient stock for one or more items.\n");
                log.append("  Update stock levels and try again.\n");
                taLog.setText(log.toString());
                PDSApp.showAlert(this, "Distribution blocked: insufficient stock for one or more items.\nSee trigger log for details.", true);
                return;
            }

            // All feasible — gather items list and run controller
            java.util.List<Object[]> items = new java.util.ArrayList<>();
            for (int i = 0; i < itemModel.getRowCount(); i++) {
                items.add(new Object[]{
                    (int) itemModel.getValueAt(i, 0),
                    (String) itemModel.getValueAt(i, 1),
                    (int) itemModel.getValueAt(i, 2)
                });
            }

            DistributeController.DistributionResult result = DistributeController.executeDistribution(reqId, items);
            taLog.setText(result.logOutput);
            
            if (result.success) {
                lblStat.setText("FULFILLED");
                lblStat.setForeground(PDSApp.SUCCESS);
                success = true;
            }
            
            loadRequest();
            if (success) btnDistribute.setEnabled(false);

        } catch (Exception ex) { PDSApp.showAlert(this, ex.getMessage(), true); }
    }

    private DefaultTableCellRenderer centerRenderer() {
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setHorizontalAlignment(SwingConstants.CENTER);
        return r;
    }

    private DefaultTableCellRenderer feasibleRenderer() {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                String val = (v != null) ? v.toString() : "";
                boolean yes = "YES".equals(val);
                JLabel pill = PDSApp.statusPill("  " + val + "  ", 
                    yes ? PDSApp.SUCCESS : PDSApp.DANGER, 
                    yes ? PDSApp.SUCCESS_BG : PDSApp.DANGER_BG);
                if (sel) {
                    pill.setBackground(PDSApp.ACCENT_BG);
                    pill.setForeground(PDSApp.ACCENT);
                }
                return pill;
            }
        };
    }

    private JPanel stylishInfoCard(String label, JLabel valLbl, Color accentColor) {
        JPanel p = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Premium diffuse shadow
                for (int i = 0; i < 6; i++) {
                    g2.setColor(new Color(0, 0, 0, 1 + (5 - i)));
                    g2.fill(new RoundRectangle2D.Float(i, i + 2, getWidth() - i * 2, getHeight() - i * 2, 16, 16));
                }
                g2.setColor(PDSApp.CARD_BG);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                // Accent bar top
                g2.setColor(accentColor);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), 4, 16, 16));
                g2.fillRect(0, 4, getWidth(), 1); // remove bottom round of accent bar
                
                g2.setColor(PDSApp.BORDER_CLR);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 16, 16));
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(16, 18, 16, 18));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel lbl = new JLabel(label.toUpperCase());
        lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        lbl.setForeground(PDSApp.TEXT_MUTED);

        valLbl.setFont(new Font("SansSerif", Font.BOLD, 15));

        gbc.gridy = 0;
        gbc.insets = new Insets(4, 0, 2, 0);
        p.add(lbl, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(2, 0, 2, 0);
        p.add(valLbl, gbc);

        return p;
    }
}