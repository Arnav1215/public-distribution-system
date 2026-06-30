import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class PlaceRequestController {

    public static class CommodityData {
        public final List<Integer> ids = new ArrayList<>();
        public final List<String> names = new ArrayList<>();
        public final List<String> units = new ArrayList<>();
    }

    public static class BeneficiaryLookup {
        public final String name;
        public final String rationCardNo;
        public final String status;
        public final String categoryName;
        public final String shopName;
        public final int shopId;

        public BeneficiaryLookup(String name, String rationCardNo, String status, String categoryName, String shopName, int shopId) {
            this.name = name;
            this.rationCardNo = rationCardNo;
            this.status = status;
            this.categoryName = categoryName;
            this.shopName = shopName;
            this.shopId = shopId;
        }
    }

    public static CommodityData getCommodities() throws SQLException {
        CommodityData data = new CommodityData();
        Connection conn = DB.getConnection();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                 "SELECT CommodityID, CommodityName, Unit FROM COMMODITY ORDER BY CommodityName")) {
            while (rs.next()) {
                data.ids.add(rs.getInt(1));
                data.names.add(rs.getString(2));
                data.units.add(rs.getString(3));
            }
        }
        return data;
    }

    public static BeneficiaryLookup lookupBeneficiary(int benId) throws SQLException {
        Connection conn = DB.getConnection();
        String sql = "SELECT b.Name, b.RationCardNo, b.Status, c.CategoryName, " +
                     "f.ShopName, b.ShopID " +
                     "FROM BENEFICIARY b " +
                     "JOIN CATEGORY c ON c.CategoryID = b.CategoryID " +
                     "JOIN FAIR_PRICE_SHOP f ON f.ShopID = b.ShopID " +
                     "WHERE b.BeneficiaryID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, benId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new BeneficiaryLookup(
                        rs.getString("Name"),
                        rs.getString("RationCardNo"),
                        rs.getString("Status"),
                        rs.getString("CategoryName"),
                        rs.getString("ShopName"),
                        rs.getInt("ShopID")
                    );
                }
            }
        }
        return null;
    }

    public static int startRequest(int benId, int shopId) throws SQLException {
        if (BeneficiaryController.isQuotaExhausted(benId)) {
            throw new SQLException("Quota exhausted. It will be refilled next month on the 1st.");
        }
        BeneficiaryController.ensureTimeSlotsExist();
        Connection conn = DB.getConnection();
        // Get first available slot
        int slotId;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT SlotID FROM TIME_SLOT LIMIT 1")) {
            if (!rs.next()) {
                throw new SQLException("No time slots available in the system.");
            }
            slotId = rs.getInt(1);
        }

        // Insert REQUEST
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO REQUEST (RequestDate, Status, BeneficiaryID, ShopID, SlotID) " +
                "VALUES (CURDATE(), 'PLACED', ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, benId);
            ps.setInt(2, shopId);
            ps.setInt(3, slotId);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) {
                    return k.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to retrieve generated Request ID.");
    }

    public static void addRequestItem(int reqId, int comId, int qty) throws SQLException {
        Connection conn = DB.getConnection();
        int benId = -1;
        try (PreparedStatement ps = conn.prepareStatement("SELECT BeneficiaryID FROM REQUEST WHERE RequestID=?")) {
            ps.setInt(1, reqId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    benId = rs.getInt(1);
                }
            }
        }
        if (benId != -1) {
            int remQty = 0;
            boolean hasQuota = false;
            String comName = "Commodity " + comId;
            java.util.List<Object[]> remaining = BeneficiaryController.getRemainingQuotas(benId);
            for (Object[] r : remaining) {
                if ((int) r[0] == comId) {
                    hasQuota = true;
                    remQty = (int) r[5];
                    comName = (String) r[1];
                    break;
                }
            }
            if (hasQuota && qty > remQty) {
                throw new SQLException("Requested " + qty + " of " + comName + ", but only " + remQty + " remaining.");
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO REQUEST_ITEM (RequestID, CommodityID, QuantityRequested) VALUES (?, ?, ?)")) {
            ps.setInt(1, reqId);
            ps.setInt(2, comId);
            ps.setInt(3, qty);
            ps.executeUpdate();
        }
    }

    public static void cancelRequest(int reqId) {
        if (reqId == -1) return;
        try {
            Connection conn = DB.getConnection();
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM REQUEST_ITEM WHERE RequestID=?")) {
                ps.setInt(1, reqId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM REQUEST WHERE RequestID=?")) {
                ps.setInt(1, reqId);
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
