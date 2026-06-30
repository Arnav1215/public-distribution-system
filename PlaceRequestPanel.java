import java.awt.*;
import java.sql.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class PlaceRequestPanel extends JPanel {

    private JTextField tfBenId = PDSApp.inputField(14);
    private JLabel lblName, lblCard, lblCat, lblShop;
    private JComboBox<String> cbCommodity = PDSApp.styledCombo();
    private JSpinner spQty   = PDSApp.styledSpinner(1, 9999);
    private JTable tblItems;
    private DefaultTableModel itemModel;
    private JLabel lblReqBadge = new JLabel("No active request");
    private JButton btnLookup, btnAddItem, btnSubmit, btnCancel;

    private int currentReqId = -1;
    private int benShopId    = -1;
    private final java.util.List<Integer> comIdList   = new ArrayList<>();
    private final java.util.List<String>  comNameList = new ArrayList<>();
    private final java.util.List<String>  comUnitList = new ArrayList<>();

    public PlaceRequestPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(PDSApp.PAGE_BG);
        setBorder(new EmptyBorder(16, 20, 16, 20));
        add(buildHeader(),  BorderLayout.NORTH);
        add(buildCenter(),  BorderLayout.CENTER);
        loadCommodities();
        setButtonStates(false);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(PDSApp.PAGE_BG);
        p.setBorder(new EmptyBorder(0, 0, 22, 0));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setBackground(PDSApp.PAGE_BG);
        left.add(PDSApp.pageHeader("Place Ration Request"));
        lblReqBadge.setFont(PDSApp.FONT_BOLD);
        lblReqBadge.setForeground(PDSApp.TEXT_MUTED);
        lblReqBadge.setBorder(new CompoundBorder(
            new LineBorder(PDSApp.BORDER_CLR, 1, true),
            new EmptyBorder(6, 14, 6, 14)));
        lblReqBadge.setBackground(PDSApp.CARD_BG);
        lblReqBadge.setOpaque(true);
        p.add(left,         BorderLayout.WEST);
        p.add(lblReqBadge,  BorderLayout.EAST);
        return p;
    }

    private JPanel buildCenter() {
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setBackground(PDSApp.PAGE_BG);
        // ── Lookup card ──
        JPanel lookupCard = PDSApp.card();
        lookupCard.setLayout(new BorderLayout(0, 14));
        lookupCard.add(PDSApp.sectionLabel("Step 1 — Beneficiary Lookup"), BorderLayout.NORTH);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row.setBackground(PDSApp.CARD_BG);
        btnLookup = PDSApp.primaryBtn("Lookup & Start Request");
        tfBenId.addActionListener(e -> btnLookup.doClick());
        row.add(PDSApp.formLabel("Beneficiary ID"));
        row.add(Box.createHorizontalStrut(6));
        row.add(tfBenId);
        row.add(btnLookup);
        lookupCard.add(row, BorderLayout.CENTER);
        lblName = new JLabel("—"); lblCard = new JLabel("—");
        lblCat  = new JLabel("—"); lblShop = new JLabel("—");
        JPanel tiles = new JPanel(new GridLayout(1, 4, 12, 0));
        tiles.setBackground(PDSApp.CARD_BG);
        tiles.add(PDSApp.infoTile("Name",        lblName));
        tiles.add(PDSApp.infoTile("Ration Card", lblCard));
        tiles.add(PDSApp.infoTile("Category",    lblCat));
        tiles.add(PDSApp.infoTile("Shop",        lblShop));
        lookupCard.add(tiles, BorderLayout.SOUTH);
        lookupCard.setMinimumSize(new Dimension(0, 160));
        lookupCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        // ── Item form card ──
        btnAddItem = PDSApp.primaryBtn("+ Add Item");
        btnSubmit  = PDSApp.successBtn("✔  Submit Request");
        btnSubmit.putClientProperty("JButton.buttonType", "default");
        btnCancel  = PDSApp.dangerBtn("✖  Cancel Request");
        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        inputRow.setBackground(PDSApp.CARD_BG);
        inputRow.add(PDSApp.formLabel("Commodity"));
        inputRow.add(cbCommodity);
        inputRow.add(PDSApp.formLabel("Quantity"));
        inputRow.add(spQty);
        inputRow.add(btnAddItem);
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        actionRow.setBackground(PDSApp.CARD_BG);
        actionRow.add(btnSubmit);
        actionRow.add(btnCancel);
        JPanel itemForm = PDSApp.card();
        itemForm.setLayout(new BoxLayout(itemForm, BoxLayout.Y_AXIS));
        itemForm.add(PDSApp.sectionLabel("Step 2 — Add Commodities"));
        itemForm.add(Box.createVerticalStrut(8));
        itemForm.add(inputRow);
        itemForm.add(Box.createVerticalStrut(4));
        itemForm.add(actionRow);
        itemForm.setMinimumSize(new Dimension(0, 120));
        itemForm.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        // ── Items table ──
        itemModel = new DefaultTableModel(new String[]{"#", "Commodity", "Unit", "Quantity Requested"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblItems = PDSApp.styledTable(new String[]{"#", "Commodity", "Unit", "Quantity Requested"});
        tblItems.setModel(itemModel);
        tblItems.getColumnModel().getColumn(0).setMaxWidth(40);
        tblItems.getColumnModel().getColumn(0).setCellRenderer(centerRenderer());
        tblItems.getColumnModel().getColumn(2).setMaxWidth(80);
        tblItems.getColumnModel().getColumn(3).setMaxWidth(160);
        tblItems.getColumnModel().getColumn(3).setCellRenderer(centerRenderer());
        JScrollPane tblScroll = PDSApp.tableScroll(tblItems);
        tblScroll.setPreferredSize(new Dimension(0, 200));

        // Stack everything in a BoxLayout column — nothing collapses
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setBackground(PDSApp.PAGE_BG);
        col.add(lookupCard);
        col.add(Box.createVerticalStrut(16));
        col.add(itemForm);
        col.add(Box.createVerticalStrut(16));
        col.add(tblScroll);
        p.add(col, BorderLayout.CENTER);
        btnLookup.addActionListener(e -> lookupAndStart());
        btnAddItem.addActionListener(e -> addItem());
        btnSubmit.addActionListener(e -> submitRequest());
        btnCancel.addActionListener(e -> cancelRequest());
        return p;
    }

    private void loadCommodities() {
        cbCommodity.removeAllItems();
        comIdList.clear(); comNameList.clear(); comUnitList.clear();
        try {
            PlaceRequestController.CommodityData data = PlaceRequestController.getCommodities();
            for (int i = 0; i < data.ids.size(); i++) {
                comIdList.add(data.ids.get(i));
                comNameList.add(data.names.get(i));
                comUnitList.add(data.units.get(i));
                cbCommodity.addItem(data.names.get(i) + "  (" + data.units.get(i) + ")");
            }
        } catch (SQLException ex) { ex.printStackTrace(); }
    }

    private void lookupAndStart() {
        String txt = tfBenId.getText().trim();
        if (txt.isEmpty()) { PDSApp.showAlert(this, "Enter a Beneficiary ID.", true); return; }
        loadCommodities(); // refresh in case admin added new commodities
        try {
            int benId = Integer.parseInt(txt);
            PlaceRequestController.BeneficiaryLookup lookup = PlaceRequestController.lookupBeneficiary(benId);
            if (lookup == null) {
                PDSApp.showAlert(this, "Beneficiary ID " + benId + " not found.", true); return;
            }
            if (!"ACTIVE".equals(lookup.status)) {
                PDSApp.showAlert(this, "Beneficiary is INACTIVE. Cannot place request.", true); return;
            }
            if (BeneficiaryController.isQuotaExhausted(benId)) {
                PDSApp.showAlert(this, "Quota exhausted. It will be refilled next month on the 1st.", true);
                return;
            }
            benShopId = lookup.shopId;
            lblName.setText(lookup.name);
            lblCard.setText(lookup.rationCardNo);
            lblCat.setText(lookup.categoryName);
            lblShop.setText(lookup.shopName);

            currentReqId = PlaceRequestController.startRequest(benId, benShopId);

            lblReqBadge.setText("Active Request  #" + currentReqId);
            lblReqBadge.setForeground(PDSApp.ACCENT);
            lblReqBadge.setBorder(new CompoundBorder(
                new LineBorder(PDSApp.ACCENT, 1, true),
                new EmptyBorder(6, 14, 6, 14)));
            itemModel.setRowCount(0);
            setButtonStates(true);

        } catch (NumberFormatException ex) {
            PDSApp.showAlert(this, "Beneficiary ID must be a number.", true);
        } catch (SQLException ex) {
            PDSApp.showAlert(this, ex.getMessage(), true);
        }
    }

    private void addItem() {
        if (currentReqId == -1) return;
        if (comIdList.isEmpty()) { PDSApp.showAlert(this, "No commodities available. Ask admin to add commodities first.", true); return; }
        try {
            int idx   = cbCommodity.getSelectedIndex();
            int comId = comIdList.get(idx);
            int qty   = (int) spQty.getValue();
            // Check for duplicate commodity in this request
            for (int i = 0; i < itemModel.getRowCount(); i++) {
                if (comNameList.get(idx).equals(itemModel.getValueAt(i, 1))) {
                    PDSApp.showAlert(this, comNameList.get(idx) + " is already added. Remove it first or adjust quantity.", true);
                    return;
                }
            }
            
            PlaceRequestController.addRequestItem(currentReqId, comId, qty);
            
            itemModel.addRow(new Object[]{
                itemModel.getRowCount() + 1,
                comNameList.get(idx),
                comUnitList.get(idx),
                qty
            });
        } catch (SQLException ex) { PDSApp.showAlert(this, ex.getMessage(), true); }
    }

    private void submitRequest() {
        if (itemModel.getRowCount() == 0) {
            PDSApp.showAlert(this, "Add at least one item before submitting.", true); return;
        }
        PDSApp.showAlert(this,
            "Request #" + currentReqId + " placed successfully with " +
            itemModel.getRowCount() + " item(s).\n\nGo to 'Distribute Items' to fulfil it.", false);
        reset();
    }

    private void cancelRequest() {
        if (currentReqId != -1) {
            PlaceRequestController.cancelRequest(currentReqId);
        }
        reset();
    }

    private void reset() {
        currentReqId = -1; benShopId = -1;
        tfBenId.setText("");
        lblName.setText("—"); lblCard.setText("—"); lblCat.setText("—"); lblShop.setText("—");
        lblReqBadge.setText("No active request");
        lblReqBadge.setForeground(PDSApp.TEXT_MUTED);
        lblReqBadge.setBorder(new CompoundBorder(
            new LineBorder(PDSApp.BORDER_CLR, 1, true),
            new EmptyBorder(6, 14, 6, 14)));
        itemModel.setRowCount(0);
        setButtonStates(false);
    }

    private void setButtonStates(boolean active) {
        btnAddItem.setEnabled(active);
        btnSubmit.setEnabled(active);
        btnCancel.setEnabled(active);
        btnLookup.setEnabled(!active);
        tfBenId.setEnabled(!active);
    }

    private DefaultTableCellRenderer centerRenderer() {
        DefaultTableCellRenderer r = new DefaultTableCellRenderer();
        r.setHorizontalAlignment(SwingConstants.CENTER);
        return r;
    }
}
