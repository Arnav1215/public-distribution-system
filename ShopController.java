import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ShopController {

    public static class SummaryMetrics {
        public String totalBeneficiaries = "0";
        public String pendingRequests = "0";
        public String fulfilledRequests = "0";
        public String totalTransactions = "0";
        public String lowStockItems = "0";
        public String totalStockItems = "0";
    }

    public static class OutletInfo {
        public final String shopName;
        public final String location;
        public final int capacity;
        public final String warehouseLocation;
        public final int warehouseStock;

        public OutletInfo(String shopName, String location, int capacity, String warehouseLocation, int warehouseStock) {
            this.shopName = shopName;
            this.location = location;
            this.capacity = capacity;
            this.warehouseLocation = warehouseLocation;
            this.warehouseStock = warehouseStock;
        }
    }

    private static String scalar(Statement st, String sql) throws SQLException {
        try (ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : "0";
        }
    }

    public static SummaryMetrics getSummaryMetrics(int shopId) throws SQLException {
        Connection conn = DB.getConnection();
        SummaryMetrics m = new SummaryMetrics();
        try (Statement st = conn.createStatement()) {
            m.totalBeneficiaries = scalar(st, "SELECT COUNT(*) FROM BENEFICIARY WHERE ShopID=" + shopId);
            m.pendingRequests = scalar(st, "SELECT COUNT(*) FROM REQUEST WHERE ShopID=" + shopId + " AND Status='PLACED'");
            m.fulfilledRequests = scalar(st, "SELECT COUNT(*) FROM REQUEST WHERE ShopID=" + shopId + " AND Status='FULFILLED'");
            m.totalTransactions = scalar(st, "SELECT COUNT(*) FROM DISTRIBUTION_TRANSACTION dt JOIN REQUEST r ON r.RequestID=dt.RequestID WHERE r.ShopID=" + shopId);
            m.lowStockItems = scalar(st, "SELECT COUNT(*) FROM STOCK WHERE ShopID=" + shopId + " AND QuantityAvailable<50");
            m.totalStockItems = scalar(st, "SELECT COUNT(*) FROM STOCK WHERE ShopID=" + shopId);
        }
        return m;
    }

    public static List<Object[]> getStockRoster(int shopId) throws SQLException {
        Connection conn = DB.getConnection();
        List<Object[]> roster = new ArrayList<>();
        String sql = "SELECT s.StockID, c.CommodityName, c.Unit, s.QuantityAvailable, " +
                     "CASE WHEN s.QuantityAvailable < 50 THEN 'LOW' ELSE 'OK' END " +
                     "FROM STOCK s JOIN COMMODITY c ON c.CommodityID = s.CommodityID " +
                     "WHERE s.ShopID = ? ORDER BY c.CommodityName";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, shopId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    roster.add(new Object[]{
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getInt(4),
                        rs.getString(5)
                    });
                }
            }
        }
        return roster;
    }

    public static void updateStockQty(int stockId, int qty) throws SQLException {
        Connection conn = DB.getConnection();
        String sql = "UPDATE STOCK SET QuantityAvailable = ? WHERE StockID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setInt(2, stockId);
            ps.executeUpdate();
        }
    }

    public static List<Object[]> getIncomingRequests(int shopId) throws SQLException {
        Connection conn = DB.getConnection();
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT r.RequestID, b.Name, b.RationCardNo, r.RequestDate, ts.SlotDate, ts.StartTime, ts.EndTime, r.Status " +
                     "FROM REQUEST r JOIN BENEFICIARY b ON b.BeneficiaryID = r.BeneficiaryID " +
                     "JOIN TIME_SLOT ts ON ts.SlotID = r.SlotID " +
                     "WHERE r.ShopID = ? ORDER BY r.RequestDate DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, shopId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[]{
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getString(5),
                        rs.getString(6),
                        rs.getString(7),
                        rs.getString(8)
                    });
                }
            }
        }
        return list;
    }

    public static void executeRequestDistribution(int reqId) throws SQLException {
        Connection conn = DB.getConnection();
        conn.setAutoCommit(false);
        try {
            String updateSql = "UPDATE REQUEST SET Status='FULFILLED' WHERE RequestID=?";
            try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                updatePs.setInt(1, reqId);
                updatePs.executeUpdate();
            }
            
            String logSql = "INSERT INTO DISTRIBUTION_TRANSACTION (RequestID, CommodityID, QuantityIssued) " +
                            "SELECT ?, CommodityID, QuantityRequested FROM REQUEST_ITEM WHERE RequestID=?";
            try (PreparedStatement logPs = conn.prepareStatement(logSql)) {
                logPs.setInt(1, reqId);
                logPs.setInt(2, reqId);
                logPs.executeUpdate();
            }
            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public static List<Object[]> getShopBeneficiaries(int shopId) throws SQLException {
        Connection conn = DB.getConnection();
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT b.BeneficiaryID, b.Name, b.RationCardNo, b.DOB, b.Status " +
                     "FROM BENEFICIARY b WHERE b.ShopID = ? ORDER BY b.BeneficiaryID";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, shopId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[]{
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getString(5)
                    });
                }
            }
        }
        return list;
    }

    public static OutletInfo getOutletInfo(int shopId) throws SQLException {
        Connection conn = DB.getConnection();
        String sql = "SELECT s.ShopName, s.Location, s.Capacity, COALESCE(w.Location, 'None Assigned') AS WarehouseLocation, COALESCE(w.AvailableStock, 0) AS WarehouseStock " +
                     "FROM FAIR_PRICE_SHOP s LEFT JOIN WAREHOUSE w ON w.WarehouseID = s.WarehouseID WHERE s.ShopID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, shopId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new OutletInfo(
                        rs.getString("ShopName"),
                        rs.getString("Location"),
                        rs.getInt("Capacity"),
                        rs.getString("WarehouseLocation"),
                        rs.getInt("WarehouseStock")
                    );
                }
            }
        }
        return null;
    }

    public static List<Object[]> getAuditLogs() throws SQLException {
        Connection conn = DB.getConnection();
        List<Object[]> logs = new ArrayList<>();
        String sql = "SELECT a.LogID, COALESCE((SELECT c.CommodityName FROM COMMODITY c WHERE c.CommodityID = (SELECT ri.CommodityID FROM REQUEST_ITEM ri WHERE ri.RequestID = a.EntityID LIMIT 1)), 'Stock Ledger Event') as ItemName, a.OperationType, a.Timestamp " +
                     "FROM AUDIT_LOG a WHERE a.OperationType IN ('STOCK_DEDUCTED', 'STOCK_UPDATED') ORDER BY a.LogID DESC LIMIT 40";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                logs.add(new Object[]{
                    rs.getInt(1),
                    rs.getString(2),
                    "STOCK_DEDUCTED".equals(rs.getString("OperationType")) ? "📦 DISBURSED" : "📥 REPLENISHED",
                    rs.getString(4)
                });
            }
        }
        return logs;
    }

    public static boolean updateCredentials(String username, String currentPass, String newPass) throws SQLException {
        Connection conn = DB.getConnection();
        String checkSql = "SELECT UserID FROM USERS WHERE Username=? AND PasswordHash=SHA2(?,256)";
        try (PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
            psCheck.setString(1, username);
            psCheck.setString(2, currentPass);
            try (ResultSet rs = psCheck.executeQuery()) {
                if (!rs.next()) {
                    return false; // Auth failure
                }
            }
        }

        String updateSql = "UPDATE USERS SET PasswordHash=SHA2(?,256) WHERE Username=?";
        try (PreparedStatement psUp = conn.prepareStatement(updateSql)) {
            psUp.setString(1, newPass);
            psUp.setString(2, username);
            psUp.executeUpdate();
        }
        return true;
    }

    public static void orderStock(int shopId, int stockId, int qty) throws SQLException {
        if (qty <= 0) {
            throw new SQLException("Order quantity must be positive.");
        }
        Connection conn = DB.getConnection();
        conn.setAutoCommit(false);
        try {
            int commodityId = -1;
            try (PreparedStatement ps = conn.prepareStatement("SELECT CommodityID FROM STOCK WHERE StockID = ?")) {
                ps.setInt(1, stockId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) commodityId = rs.getInt(1);
                }
            }
            if (commodityId == -1) {
                throw new SQLException("Invalid stock ID.");
            }

            int warehouseId = -1;
            int availableStock = 0;
            String whSql = "SELECT w.WarehouseID, w.AvailableStock FROM FAIR_PRICE_SHOP s " +
                           "JOIN WAREHOUSE w ON w.WarehouseID = s.WarehouseID WHERE s.ShopID = ?";
            try (PreparedStatement ps = conn.prepareStatement(whSql)) {
                ps.setInt(1, shopId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        warehouseId = rs.getInt("WarehouseID");
                        availableStock = rs.getInt("AvailableStock");
                    }
                }
            }

            if (warehouseId == -1) {
                throw new SQLException("No warehouse assigned to this shop.");
            }
            if (availableStock < qty) {
                throw new SQLException("Insufficient stock in assigned warehouse. Available: " + availableStock + " Kg");
            }

            String deductSql = "UPDATE WAREHOUSE SET AvailableStock = AvailableStock - ? WHERE WarehouseID = ?";
            try (PreparedStatement ps = conn.prepareStatement(deductSql)) {
                ps.setInt(1, qty);
                ps.setInt(2, warehouseId);
                ps.executeUpdate();
            }

            String addSql = "UPDATE STOCK SET QuantityAvailable = QuantityAvailable + ? WHERE StockID = ?";
            try (PreparedStatement ps = conn.prepareStatement(addSql)) {
                ps.setInt(1, qty);
                ps.setInt(2, stockId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO AUDIT_LOG (EntityName, EntityID, OperationType, PerformedBy) VALUES ('WAREHOUSE', ?, 'STOCK_DEDUCTED', ?)")) {
                ps.setInt(1, warehouseId);
                ps.setString(2, "SHOP_" + shopId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO AUDIT_LOG (EntityName, EntityID, OperationType, PerformedBy) VALUES ('STOCK', ?, 'STOCK_UPDATED', ?)")) {
                ps.setInt(1, stockId);
                ps.setString(2, "SHOP_" + shopId);
                ps.executeUpdate();
            }

            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(true);
        }
    }
}
