import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;

public class PortfolioPanel extends JPanel {

    private JTextField tfBenId = PDSApp.inputField(14);
    private JLabel lblName, lblCard, lblCat, lblStatus, lblShop;
    private JTable tblQuota, tblTxn;
    private DefaultTableModel quotaModel, txnModel;
    private JLabel lblTxnCount = new JLabel("0 transactions");

    public PortfolioPanel() {
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
        p.add(PDSApp.pageHeader("Beneficiary Portfolio"), BorderLayout.WEST);
        JButton btnRefresh = PDSApp.secondaryBtn("⟳  Refresh");
        btnRefresh.addActionListener(e -> { tfBenId.setText(""); lblName.setText("—"); lblCard.setText("—"); lblCat.setText("—"); lblStatus.setText("—"); lblShop.setText("—"); quotaModel.setRowCount(0); txnModel.setRowCount(0); lblTxnCount.setText("0 transactions"); });
        p.add(btnRefresh, BorderLayout.EAST);
        return p;
    }

    private JPanel buildCenter() {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setBackground(PDSApp.PAGE_BG);

        // ── Search card ──
        JPanel searchCard = PDSApp.card();
        searchCard.setLayout(new BorderLayout(0, 0));
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        searchRow.setBackground(PDSApp.CARD_BG);
        JButton btnLoad = PDSApp.primaryBtn("Load Portfolio");
        tfBenId.addActionListener(e -> btnLoad.doClick());
        searchRow.add(PDSApp.formLabel("Beneficiary ID"));
        searchRow.add(Box.createHorizontalStrut(6));
        searchRow.add(tfBenId);
        searchRow.add(btnLoad);
        searchCard.add(searchRow, BorderLayout.CENTER);

        // ── Info tiles ──
        lblName   = new JLabel("—"); lblCard   = new JLabel("—");
        lblCat    = new JLabel("—"); lblStatus = new JLabel("—");
        lblShop   = new JLabel("—");
        JPanel tiles = new JPanel(new GridLayout(1, 5, 12, 0));
        tiles.setBackground(PDSApp.PAGE_BG);
        tiles.add(PDSApp.infoTile("Name",        lblName));
        tiles.add(PDSApp.infoTile("Ration Card", lblCard));
        tiles.add(PDSApp.infoTile("Category",    lblCat));
        tiles.add(PDSApp.infoTile("Status",      lblStatus));
        tiles.add(PDSApp.infoTile("Shop",        lblShop));

        // ── Tables ──
        tblQuota = PDSApp.styledTable(new String[]{"Commodity", "Month", "Year", "Max Allowed Qty"});
        quotaModel = (DefaultTableModel) tblQuota.getModel();
        tblQuota.getColumnModel().getColumn(1).setMaxWidth(80);
        tblQuota.getColumnModel().getColumn(1).setCellRenderer(centerRenderer());
        tblQuota.getColumnModel().getColumn(2).setMaxWidth(80);
        tblQuota.getColumnModel().getColumn(2).setCellRenderer(centerRenderer());
        tblQuota.getColumnModel().getColumn(3).setCellRenderer(centerRenderer());

        tblTxn = PDSApp.styledTable(new String[]{"Txn ID", "Commodity", "Qty Issued", "Request Date"});
        txnModel = (DefaultTableModel) tblTxn.getModel();
        tblTxn.getColumnModel().getColumn(0).setMaxWidth(70);
        tblTxn.getColumnModel().getColumn(0).setCellRenderer(centerRenderer());
        tblTxn.getColumnModel().getColumn(2).setMaxWidth(100);
        tblTxn.getColumnModel().getColumn(2).setCellRenderer(centerRenderer());

        JScrollPane spQ = PDSApp.tableScroll(tblQuota);
        spQ.setBorder(BorderFactory.createTitledBorder(
            new LineBorder(PDSApp.BORDER_CLR, 1, true), "  Monthly Quota  ",
            0, 0, PDSApp.FONT_SBOLD, PDSApp.TEXT_MUTED));

        JScrollPane spT = PDSApp.tableScroll(tblTxn);
        spT.setBorder(BorderFactory.createTitledBorder(
            new LineBorder(PDSApp.BORDER_CLR, 1, true), "  Transaction History  ",
            0, 0, PDSApp.FONT_SBOLD, PDSApp.TEXT_MUTED));

        JPanel txnPanel = new JPanel(new BorderLayout(0, 4));
        txnPanel.setBackground(PDSApp.PAGE_BG);
        lblTxnCount.setFont(PDSApp.FONT_SMALL);
        lblTxnCount.setForeground(PDSApp.TEXT_MUTED);
        txnPanel.add(lblTxnCount, BorderLayout.NORTH);
        txnPanel.add(spT, BorderLayout.CENTER);

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.setBackground(PDSApp.PAGE_BG);
        northPanel.add(searchCard);
        northPanel.add(Box.createVerticalStrut(16));
        northPanel.add(tiles);

        JPanel tableRow = new JPanel(new GridLayout(1, 2, 16, 0));
        tableRow.setBackground(PDSApp.PAGE_BG);
        tableRow.add(spQ);
        tableRow.add(txnPanel);

        p.add(northPanel, BorderLayout.NORTH);
        p.add(tableRow,   BorderLayout.CENTER);

        btnLoad.addActionListener(e -> loadPortfolio());
        return p;
    }

    private void loadPortfolio() {
        quotaModel.setRowCount(0);
        txnModel.setRowCount(0);
        String txt = tfBenId.getText().trim();
        if (txt.isEmpty()) { PDSApp.showAlert(this, "Enter a Beneficiary ID.", true); return; }
        try {
            int benId = Integer.parseInt(txt);

            PortfolioController.PortfolioData data = PortfolioController.getPortfolio(benId);
            if (data == null) {
                PDSApp.showAlert(this, "Beneficiary #" + benId + " not found.", true);
                return;
            }

            // Set beneficiary info
            lblName.setText(data.info.name);
            lblCard.setText(data.info.rationCardNo);
            lblCat.setText(data.info.categoryName);
            lblShop.setText(data.info.shopName);
            String st = data.info.status;
            lblStatus.setText(st);
            lblStatus.setForeground("ACTIVE".equals(st) ? PDSApp.SUCCESS : PDSApp.DANGER);

            // Populate quota
            for (Object[] row : data.quotaRows) {
                quotaModel.addRow(row);
            }
            if (quotaModel.getRowCount() == 0) {
                quotaModel.addRow(new Object[]{"No quota records", "—", "—", "—"});
            }

            // Populate transaction history
            int txnCount = 0;
            for (Object[] row : data.txnRows) {
                txnModel.addRow(row);
                txnCount++;
            }
            if (txnCount == 0) {
                txnModel.addRow(new Object[]{"—", "No transactions yet", "—", "—"});
            }
            lblTxnCount.setText(txnCount + " transaction" + (txnCount == 1 ? "" : "s"));

        } catch (NumberFormatException ex) {
            PDSApp.showAlert(this, "Beneficiary ID must be a number.", true);
        } catch (SQLException ex) {
            PDSApp.showAlert(this, ex.getMessage(), true);
        }
    }

    private DefaultTableCellRenderer centerRenderer() {
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setHorizontalAlignment(SwingConstants.CENTER);
        return r;
    }
}
