import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AdminController {

    public static class SummaryMetrics {
        public String totalBeneficiaries = "0";
        public String totalRequests = "0";
        public String totalPendingClearances = "0";
        public String totalTransactions = "0";
        public String lowStockAlerts = "0";
        public String totalCommodities = "0";
        public String totalShops = "0";
        public String totalSuppliers = "0";
    }

    private static String scalar(Statement st, String sql) throws SQLException {
        try (ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : "0";
        }
    }

    public static SummaryMetrics getSummaryMetrics() throws SQLException {
        Connection conn = DB.getConnection();
        SummaryMetrics m = new SummaryMetrics();
        try (Statement st = conn.createStatement()) {
            m.totalBeneficiaries = scalar(st, "SELECT COUNT(*) FROM BENEFICIARY");
            m.totalRequests = scalar(st, "SELECT COUNT(*) FROM REQUEST");
            m.totalPendingClearances = scalar(st, "SELECT COUNT(*) FROM REQUEST WHERE Status='PENDING'");
            m.totalTransactions = scalar(st, "SELECT COUNT(*) FROM DISTRIBUTION_TRANSACTION");
            m.lowStockAlerts = scalar(st, "SELECT COUNT(*) FROM STOCK WHERE QuantityAvailable < 50");
            m.totalCommodities = scalar(st, "SELECT COUNT(*) FROM COMMODITY");
            m.totalShops = scalar(st, "SELECT COUNT(*) FROM FAIR_PRICE_SHOP");
            m.totalSuppliers = scalar(st, "SELECT COUNT(*) FROM SUPPLIER");
        }
        return m;
    }

    public static List<Object[]> getDropdownCategories() throws SQLException {
        Connection conn = DB.getConnection();
        List<Object[]> list = new ArrayList<>();
        try (Statement st = conn.createStatement(); 
             ResultSet rs = st.executeQuery("SELECT CategoryID, CategoryName FROM CATEGORY ORDER BY CategoryName")) {
            while (rs.next()) {
                list.add(new Object[]{rs.getInt(1), rs.getString(2)});
            }
        }
        return list;
    }

    public static List<Object[]> getDropdownShops() throws SQLException {
        Connection conn = DB.getConnection();
        List<Object[]> list = new ArrayList<>();
        try (Statement st = conn.createStatement(); 
             ResultSet rs = st.executeQuery("SELECT ShopID, ShopName FROM FAIR_PRICE_SHOP ORDER BY ShopName")) {
            while (rs.next()) {
                list.add(new Object[]{rs.getInt(1), rs.getString(2)});
            }
        }
        return list;
    }

    public static List<Object[]> executeGenericQuery(String sql) throws SQLException {
        Connection conn = DB.getConnection();
        List<Object[]> rows = new ArrayList<>();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            while (rs.next()) {
                Object[] row = new Object[colCount];
                for (int i = 0; i < colCount; i++) {
                    row[i] = rs.getObject(i + 1);
                }
                rows.add(row);
            }
        }
        return rows;
    }

    public static String getScalarValue(String sql) throws SQLException {
        Connection conn = DB.getConnection();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : "0";
        }
    }

    public static void addBeneficiary(String name, String cardNo, String dob, int catId, int shopId, String username, String password) throws SQLException {
        Connection conn = DB.getConnection();
        conn.setAutoCommit(false);
        try {
            int benId;
            try (Statement st = conn.createStatement(); 
                 ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(BeneficiaryID),0)+1 FROM BENEFICIARY")) {
                rs.next();
                benId = rs.getInt(1);
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO BENEFICIARY(BeneficiaryID,Name,RationCardNo,DOB,Status,CategoryID,ShopID) VALUES(?,?,?,?,'ACTIVE',?,?)")) {
                ps.setInt(1, benId);
                ps.setString(2, name);
                ps.setString(3, cardNo);
                ps.setString(4, dob);
                ps.setInt(5, catId);
                ps.setInt(6, shopId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO USERS(Username,PasswordHash,Role,LinkedID) VALUES(?,SHA2(?,256),'BENEFICIARY',?)")) {
                ps.setString(1, username);
                ps.setString(2, password);
                ps.setInt(3, benId);
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

    public static void updateBeneficiaryShop(int benId, int shopId) throws SQLException {
        Connection conn = DB.getConnection();
        try (PreparedStatement ps = conn.prepareStatement("UPDATE BENEFICIARY SET ShopID=? WHERE BeneficiaryID=?")) {
            ps.setInt(1, shopId);
            ps.setInt(2, benId);
            ps.executeUpdate();
        }
    }

    public static void updateBeneficiaryStatus(int benId, String status) throws SQLException {
        Connection conn = DB.getConnection();
        try (PreparedStatement ps = conn.prepareStatement("UPDATE BENEFICIARY SET Status=? WHERE BeneficiaryID=?")) {
            ps.setString(1, status);
            ps.setInt(2, benId);
            ps.executeUpdate();
        }
    }

    public static void deleteBeneficiary(int benId) throws SQLException {
        Connection conn = DB.getConnection();
        conn.setAutoCommit(false);
        try {
            String[] sqls = {
                "DELETE dt FROM DISTRIBUTION_TRANSACTION dt JOIN REQUEST r ON r.RequestID=dt.RequestID WHERE r.BeneficiaryID=?",
                "DELETE ri FROM REQUEST_ITEM ri JOIN REQUEST r ON r.RequestID=ri.RequestID WHERE r.BeneficiaryID=?",
                "DELETE FROM REQUEST WHERE BeneficiaryID=?",
                "DELETE FROM MONTHLY_QUOTA WHERE BeneficiaryID=?",
                "DELETE FROM USERS WHERE Role='BENEFICIARY' AND LinkedID=?",
                "DELETE FROM BENEFICIARY WHERE BeneficiaryID=?"
            };
            for (String sql : sqls) {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, benId);
                    ps.executeUpdate();
                }
            }
            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public static void addShop(String sn, String loc, int capacity, int warehouseId, String user, String pass) throws SQLException {
        Connection conn = DB.getConnection();
        conn.setAutoCommit(false);
        try {
            int shopId;
            try (Statement st = conn.createStatement(); 
                 ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(ShopID),0)+1 FROM FAIR_PRICE_SHOP")) {
                rs.next();
                shopId = rs.getInt(1);
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO FAIR_PRICE_SHOP(ShopID,ShopName,Location,Capacity,WarehouseID) VALUES(?,?,?,?,?)")) {
                ps.setInt(1, shopId);
                ps.setString(2, sn);
                ps.setString(3, loc);
                ps.setInt(4, capacity);
                ps.setInt(5, warehouseId);
                ps.executeUpdate();
            }
            int stockId;
            try (Statement st = conn.createStatement(); 
                 ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(StockID),0) FROM STOCK")) {
                rs.next();
                stockId = rs.getInt(1);
            }
            try (Statement st = conn.createStatement(); 
                 ResultSet rs = st.executeQuery("SELECT CommodityID FROM COMMODITY"); 
                 PreparedStatement ps2 = conn.prepareStatement(
                      "INSERT INTO STOCK(StockID,ShopID,CommodityID,QuantityAvailable) VALUES(?,?,?,0)")) {
                while (rs.next()) {
                    stockId++;
                    ps2.setInt(1, stockId);
                    ps2.setInt(2, shopId);
                    ps2.setInt(3, rs.getInt(1));
                    ps2.executeUpdate();
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO USERS(Username,PasswordHash,Role,LinkedID) VALUES(?,SHA2(?,256),'SHOP_OPERATOR',?)")) {
                ps.setString(1, user);
                ps.setString(2, pass);
                ps.setInt(3, shopId);
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

    public static void updateShopWarehouse(int shopId, int warehouseId) throws SQLException {
        Connection conn = DB.getConnection();
        String sql = "UPDATE FAIR_PRICE_SHOP SET WarehouseID = ? WHERE ShopID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, warehouseId);
            ps.setInt(2, shopId);
            ps.executeUpdate();
        }
    }

    public static void deleteShop(int shopId) throws SQLException {
        Connection conn = DB.getConnection();
        conn.setAutoCommit(false);
        try {
            String[] sqls = {
                "DELETE dt FROM DISTRIBUTION_TRANSACTION dt JOIN REQUEST r ON r.RequestID = dt.RequestID LEFT JOIN BENEFICIARY b ON b.BeneficiaryID = r.BeneficiaryID WHERE r.ShopID = ? OR b.ShopID = ?",
                "DELETE ri FROM REQUEST_ITEM ri JOIN REQUEST r ON r.RequestID = ri.RequestID LEFT JOIN BENEFICIARY b ON b.BeneficiaryID = r.BeneficiaryID WHERE r.ShopID = ? OR b.ShopID = ?",
                "DELETE r FROM REQUEST r LEFT JOIN BENEFICIARY b ON b.BeneficiaryID = r.BeneficiaryID WHERE r.ShopID = ? OR b.ShopID = ?",
                "DELETE FROM STOCK WHERE ShopID = ?",
                "DELETE FROM USERS WHERE Role = 'BENEFICIARY' AND LinkedID IN (SELECT BeneficiaryID FROM BENEFICIARY WHERE ShopID = ?)",
                "DELETE FROM MONTHLY_QUOTA WHERE BeneficiaryID IN (SELECT BeneficiaryID FROM BENEFICIARY WHERE ShopID = ?)",
                "DELETE FROM BENEFICIARY WHERE ShopID = ?",
                "DELETE FROM USERS WHERE Role = 'SHOP_OPERATOR' AND LinkedID = ?",
                "DELETE FROM FAIR_PRICE_SHOP WHERE ShopID = ?"
            };
            int idx = 0;
            for (String sql : sqls) {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    if (idx < 3) {
                        ps.setInt(1, shopId);
                        ps.setInt(2, shopId);
                    } else {
                        ps.setInt(1, shopId);
                    }
                    ps.executeUpdate();
                }
                idx++;
            }
            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public static void addCategory(String name, String desc) throws SQLException {
        Connection conn = DB.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO CATEGORY(CategoryID,CategoryName,Description) SELECT COALESCE(MAX(CategoryID),0)+1,?,? FROM CATEGORY")) {
            ps.setString(1, name);
            ps.setString(2, desc);
            ps.executeUpdate();
        }
    }

    public static void deleteCategory(int catId) throws SQLException {
        Connection conn = DB.getConnection();
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM CATEGORY WHERE CategoryID=?")) {
            ps.setInt(1, catId);
            ps.executeUpdate();
        }
    }

    public static void addCommodity(String name) throws SQLException {
        Connection conn = DB.getConnection();
        conn.setAutoCommit(false);
        try {
            int comId;
            try (Statement st = conn.createStatement(); 
                 ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(CommodityID),0)+1 FROM COMMODITY")) {
                rs.next();
                comId = rs.getInt(1);
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO COMMODITY(CommodityID,CommodityName,Unit,ExpiryTracking) VALUES(?,?,'Kg',0)")) {
                ps.setInt(1, comId);
                ps.setString(2, name);
                ps.executeUpdate();
            }
            int stockId;
            try (Statement st = conn.createStatement(); 
                 ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(StockID),0) FROM STOCK")) {
                rs.next();
                stockId = rs.getInt(1);
            }
            try (Statement st = conn.createStatement(); 
                 ResultSet rs = st.executeQuery("SELECT ShopID FROM FAIR_PRICE_SHOP"); 
                 PreparedStatement ps2 = conn.prepareStatement(
                     "INSERT INTO STOCK(StockID,ShopID,CommodityID,QuantityAvailable) VALUES(?,?,?,0)")) {
                while (rs.next()) {
                    stockId++;
                    ps2.setInt(1, stockId);
                    ps2.setInt(2, rs.getInt(1));
                    ps2.setInt(3, comId);
                    ps2.executeUpdate();
                }
            }
            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public static void deleteCommodity(int comId) throws SQLException {
        Connection conn = DB.getConnection();
        conn.setAutoCommit(false);
        try {
            String[] sqls = {
                "DELETE FROM DISTRIBUTION_TRANSACTION WHERE CommodityID = ?",
                "DELETE FROM REQUEST_ITEM WHERE CommodityID = ?",
                "DELETE FROM MONTHLY_QUOTA WHERE CommodityID = ?",
                "DELETE FROM ALLOCATION_RULE WHERE CommodityID = ?",
                "DELETE FROM BATCH WHERE CommodityID = ?",
                "DELETE FROM STOCK WHERE CommodityID = ?",
                "DELETE FROM COMMODITY WHERE CommodityID = ?"
            };
            for (String sql : sqls) {
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, comId);
                    ps.executeUpdate();
                }
            }
            conn.commit();
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public static void addSupplier(String name, String contact) throws SQLException {
        Connection conn = DB.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO SUPPLIER(SupplierID,OrganizationName,GSTNumber,Status) SELECT COALESCE(MAX(SupplierID),0)+1,?,?,'ACTIVE' FROM SUPPLIER")) {
            ps.setString(1, name);
            ps.setString(2, contact);
            ps.executeUpdate();
        }
    }

    public static void addWarehouse(String location, int capacity) throws SQLException {
        Connection conn = DB.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO WAREHOUSE(WarehouseID,Location,Capacity) SELECT COALESCE(MAX(WarehouseID),0)+1,?,? FROM WAREHOUSE")) {
            ps.setString(1, location);
            ps.setInt(2, capacity);
            ps.executeUpdate();
        }
    }

    public static void addAllocationRule(int catId, int comId, int quota, int limit) throws SQLException {
        Connection conn = DB.getConnection();
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO ALLOCATION_RULE(RuleID,CategoryID,CommodityID,MonthlyQuota,PerVisitLimit) SELECT COALESCE(MAX(RuleID),0)+1,?,?,?,? FROM ALLOCATION_RULE")) {
            ps.setInt(1, catId);
            ps.setInt(2, comId);
            ps.setInt(3, quota);
            ps.setInt(4, limit);
            ps.executeUpdate();
        }
    }

    public static void deleteAllocationRule(int ruleId) throws SQLException {
        Connection conn = DB.getConnection();
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM ALLOCATION_RULE WHERE RuleID=?")) {
            ps.setInt(1, ruleId);
            ps.executeUpdate();
        }
    }

    public static void deleteSupplier(int supplierId) throws SQLException {
        Connection conn = DB.getConnection();
        conn.setAutoCommit(false);
        try {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM BATCH WHERE SupplierID=?")) {
                ps.setInt(1, supplierId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM SUPPLIER WHERE SupplierID=?")) {
                ps.setInt(1, supplierId);
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

    public static void deleteWarehouse(int warehouseId) throws SQLException {
        Connection conn = DB.getConnection();
        conn.setAutoCommit(false);
        try {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM BATCH WHERE WarehouseID=?")) {
                ps.setInt(1, warehouseId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM WAREHOUSE WHERE WarehouseID=?")) {
                ps.setInt(1, warehouseId);
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
