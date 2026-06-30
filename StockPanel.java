import java.awt.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class StockPanel extends JPanel {

    private JTable tbl;
    private DefaultTableModel model;
    private JLabel lblTotal = new JLabel("— units");
    private JLabel lblLow   = new JLabel("— items");
    private JLabel lblOk    = new JLabel("— items");

    
    public StockPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(PDSApp.PAGE_BG);
        setBorder(new EmptyBorder(16, 20, 16, 20));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);

        loadStock();
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(PDSApp.PAGE_BG);
        p.setBorder(new EmptyBorder(0, 0, 22, 0));
        p.add(PDSApp.pageHeader("Stock Analysis"), BorderLayout.WEST);
        JButton btnRefresh = PDSApp.secondaryBtn("⟳  Refresh");
        btnRefresh.addActionListener(e -> loadStock());
        p.add(btnRefresh, BorderLayout.EAST);
        return p;
    }

    private JPanel buildCenter() {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setBackground(PDSApp.PAGE_BG);

        // Stat tiles
        JPanel tiles = new JPanel(new GridLayout(1, 3, 14, 0));
        tiles.setBackground(PDSApp.PAGE_BG);
        tiles.setPreferredSize(new Dimension(0, 120));
        tiles.setMinimumSize(new Dimension(0, 120));
        tiles.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        tiles.add(PDSApp.statCard("Total Stock Entries", lblTotal, PDSApp.ACCENT));
        tiles.add(PDSApp.statCard("LOW Stock  (< 50)",   lblLow,   PDSApp.DANGER));
        tiles.add(PDSApp.statCard("OK Stock  (≥ 50)",    lblOk,    PDSApp.SUCCESS));

        // Table
        model = new DefaultTableModel(
            new String[]{"Shop", "Location", "Commodity", "Qty Available", "Status"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tbl = PDSApp.styledTable(new String[]{"Shop", "Location", "Commodity", "Qty Available", "Status"});
        tbl.setModel(model);
        tbl.getColumnModel().getColumn(3).setMaxWidth(130);
        tbl.getColumnModel().getColumn(3).setCellRenderer(centerRenderer());
        tbl.getColumnModel().getColumn(4).setMaxWidth(100);
        tbl.getColumnModel().getColumn(4).setCellRenderer(statusRenderer());

        // Row background colouring
        tbl.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                l.setFont(PDSApp.FONT_LABEL);
                l.setBorder(new EmptyBorder(0, 10, 0, 10));
                if (!sel) {
                    boolean low = "LOW".equals(model.getValueAt(row, 4));
                    l.setBackground(low ? PDSApp.DANGER_BG : PDSApp.SUCCESS_BG);
                    l.setForeground(PDSApp.TEXT_PRIMARY);
                }
                return l;
            }
        });
        // Re-apply status renderer on top
        tbl.getColumnModel().getColumn(4).setCellRenderer(statusRenderer());
        tbl.getColumnModel().getColumn(3).setCellRenderer(centerRenderer());

        p.add(tiles,                    BorderLayout.NORTH);
        p.add(PDSApp.tableScroll(tbl),  BorderLayout.CENTER);
        return p;
    }

    private void loadStock() {
        model.setRowCount(0);
        int low = 0, ok = 0;
        try {
            StockController.StockData data = StockController.getStockAnalysis();
            for (Object[] row : data.rows) {
                model.addRow(row);
            }
            low = data.lowCount;
            ok = data.okCount;
        } catch (SQLException ex) { 
            PDSApp.showAlert(this, ex.getMessage(), true); 
        }
        lblTotal.setText(String.valueOf(low + ok));
        lblLow.setText(String.valueOf(low));
        lblOk.setText(String.valueOf(ok));
    }

    private DefaultTableCellRenderer centerRenderer() {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                l.setFont(PDSApp.FONT_LABEL);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                if (!sel) {
                    boolean low = "LOW".equals(model.getValueAt(row, 4));
                    l.setBackground(low ? PDSApp.DANGER_BG : PDSApp.SUCCESS_BG);
                    l.setForeground(PDSApp.TEXT_PRIMARY);
                }
                return l;
            }
        };
    }

    private DefaultTableCellRenderer statusRenderer() {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
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
        };
    }
}
