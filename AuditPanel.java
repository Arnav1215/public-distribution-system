import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class AuditPanel extends JPanel {

    private JTable tbl;
    private DefaultTableModel model;
    private JLabel lblCount = new JLabel("—");

    public AuditPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(PDSApp.PAGE_BG);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);

        loadLog();
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(PDSApp.PAGE_BG);
        p.setBorder(new EmptyBorder(0, 0, 22, 0));
        p.add(PDSApp.pageHeader("Audit Log"), BorderLayout.WEST);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setBackground(PDSApp.PAGE_BG);
        lblCount.setFont(PDSApp.FONT_LABEL);
        lblCount.setForeground(PDSApp.TEXT_MUTED);
        JButton btnRefresh = PDSApp.secondaryBtn("⟳  Refresh");
        btnRefresh.addActionListener(e -> loadLog());
        right.add(lblCount);
        right.add(btnRefresh);
        p.add(right, BorderLayout.EAST);
        return p;
    }

    private JPanel buildCenter() {
        JPanel p = new JPanel(new BorderLayout(0, 14));
        p.setBackground(PDSApp.PAGE_BG);

        // Info banner
        JPanel banner = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        banner.setBackground(PDSApp.ACCENT_BG);
        banner.setBorder(new CompoundBorder(
            new LineBorder(PDSApp.BORDER_CLR, 1, true),
            new EmptyBorder(2, 6, 2, 6)));
        JLabel info = new JLabel(
            "ℹ  Entries below are written automatically by Trigger 2 " +
            "(trg_deduct_stock_after_distribution) each time items are distributed.");
        info.setFont(PDSApp.FONT_SMALL);
        info.setForeground(PDSApp.ACCENT);
        banner.add(info);

        // Table
        model = new DefaultTableModel(
            new String[]{"Log ID", "Entity", "Entity ID", "Operation", "Performed By", "Timestamp"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tbl = PDSApp.styledTable(
            new String[]{"Log ID", "Entity", "Entity ID", "Operation", "Performed By", "Timestamp"});
        tbl.setModel(model);
        tbl.getColumnModel().getColumn(0).setMaxWidth(70);
        tbl.getColumnModel().getColumn(0).setCellRenderer(centerRenderer());
        tbl.getColumnModel().getColumn(2).setMaxWidth(80);
        tbl.getColumnModel().getColumn(2).setCellRenderer(centerRenderer());
        tbl.getColumnModel().getColumn(3).setCellRenderer(operationRenderer());
        tbl.getColumnModel().getColumn(4).setCellRenderer(centerRenderer());

        p.add(banner,                   BorderLayout.NORTH);
        p.add(PDSApp.tableScroll(tbl),  BorderLayout.CENTER);
        return p;
    }

    private void loadLog() {
        model.setRowCount(0);
        try {
            java.util.List<Object[]> logs = AuditController.getAuditLogs();
            for (Object[] log : logs) {
                model.addRow(log);
            }
        } catch (SQLException ex) { 
            PDSApp.showAlert(this, ex.getMessage(), true); 
        }
        int n = model.getRowCount();
        lblCount.setText(n + (n == 1 ? " entry" : " entries"));
    }

    private DefaultTableCellRenderer centerRenderer() {
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setHorizontalAlignment(SwingConstants.CENTER);
        r.setFont(PDSApp.FONT_LABEL);
        return r;
    }

    private DefaultTableCellRenderer operationRenderer() {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                String val = (v != null) ? v.toString() : "";
                JLabel pill = PDSApp.statusPill("  " + val + "  ", PDSApp.ACCENT, PDSApp.ACCENT_BG);
                if (sel) {
                    pill.setBackground(PDSApp.ACCENT_BG);
                    pill.setForeground(PDSApp.ACCENT);
                }
                return pill;
            }
        };
    }
}
