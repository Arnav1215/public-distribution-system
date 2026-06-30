import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;

public class BeneficiaryController {

    public static class ProfileDetails {
        public String name = "—";
        public String rationCardNo = "—";
        public String dob = "—";
        public String status = "—";
        public String categoryName = "—";
        public String shopName = "—";
        public int shopId = -1;
        public String shopLocation = "—";
        public int shopCapacity = 0;
        public int totalRequests = 0;
        public int fulfilledRequests = 0;
        public int pendingRequests = 0;
        public int totalItemsReceived = 0;
        public List<Object[]> recentRequests = new ArrayList<>();
    }

    public static class FormData {
        public final List<Integer> slotIds = new ArrayList<>();
        public final List<String> slotLabels = new ArrayList<>();
        public final List<Integer> comIds = new ArrayList<>();
        public final List<String> comNames = new ArrayList<>();
        public final List<String> comUnits = new ArrayList<>();
    }

    public static int getShopId(int benId) throws SQLException {
        Connection conn = DB.getConnection();
        String sql = "SELECT ShopID FROM BENEFICIARY WHERE BeneficiaryID=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, benId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public static ProfileDetails getProfileDetails(int benId) throws SQLException {
        ensureMonthlyQuotaExists(benId);
        Connection conn = DB.getConnection();
        ProfileDetails details = new ProfileDetails();

        // 1. Basic profile
        String profileSql = 
            "SELECT b.Name,b.RationCardNo,b.DOB,b.Status,cat.CategoryName,f.ShopName,b.ShopID,f.Location,f.Capacity " +
            "FROM BENEFICIARY b " +
            "JOIN CATEGORY cat ON cat.CategoryID=b.CategoryID " +
            "JOIN FAIR_PRICE_SHOP f ON f.ShopID=b.ShopID " +
            "WHERE b.BeneficiaryID=?";
        try (PreparedStatement ps = conn.prepareStatement(profileSql)) {
            ps.setInt(1, benId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    details.name = rs.getString("Name");
                    details.rationCardNo = rs.getString("RationCardNo");
                    details.dob = rs.getString("DOB");
                    details.status = rs.getString("Status");
                    details.categoryName = rs.getString("CategoryName");
                    details.shopName = rs.getString("ShopName");
                    details.shopId = rs.getInt("ShopID");
                    details.shopLocation = rs.getString("Location");
                    details.shopCapacity = rs.getInt("Capacity");
                }
            }
        }

        // 2. Request counts
        String countSql = 
            "SELECT COUNT(*),SUM(CASE WHEN Status='FULFILLED' THEN 1 ELSE 0 END),SUM(CASE WHEN Status='PLACED' THEN 1 ELSE 0 END) " +
            "FROM REQUEST WHERE BeneficiaryID=?";
        try (PreparedStatement ps = conn.prepareStatement(countSql)) {
            ps.setInt(1, benId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    details.totalRequests = rs.getInt(1);
                    details.fulfilledRequests = rs.getInt(2);
                    details.pendingRequests = rs.getInt(3);
                }
            }
        }

        // 3. Items received
        String itemsSql = 
            "SELECT COALESCE(SUM(dt.QuantityIssued),0) " +
            "FROM DISTRIBUTION_TRANSACTION dt " +
            "JOIN REQUEST r ON r.RequestID=dt.RequestID " +
            "WHERE r.BeneficiaryID=?";
        try (PreparedStatement ps = conn.prepareStatement(itemsSql)) {
            ps.setInt(1, benId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    details.totalItemsReceived = rs.getInt(1);
                }
            }
        }

        // 4. Recent activity
        String recentSql = 
            "SELECT r.RequestID,r.RequestDate,COUNT(ri.RequestItemID),r.Status " +
            "FROM REQUEST r " +
            "LEFT JOIN REQUEST_ITEM ri ON ri.RequestID=r.RequestID " +
            "WHERE r.BeneficiaryID=? " +
            "GROUP BY r.RequestID,r.RequestDate,r.Status " +
            "ORDER BY r.RequestDate DESC LIMIT 6";
        try (PreparedStatement ps = conn.prepareStatement(recentSql)) {
            ps.setInt(1, benId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    details.recentRequests.add(new Object[]{
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getInt(3) + " item(s)",
                        rs.getString(4)
                    });
                }
            }
        }

        return details;
    }

    public static List<Object[]> getQuotaRecords(int benId) throws SQLException {
        ensureMonthlyQuotaExists(benId);
        Connection conn = DB.getConnection();
        List<Object[]> records = new ArrayList<>();
        String sql = 
            "SELECT c.CommodityName,c.Unit,mq.Month,mq.Year,mq.MaxAllowedQuantity," +
            "COALESCE((SELECT SUM(dt.QuantityIssued) FROM DISTRIBUTION_TRANSACTION dt JOIN REQUEST r ON r.RequestID=dt.RequestID WHERE r.BeneficiaryID=mq.BeneficiaryID AND dt.CommodityID=mq.CommodityID AND MONTH(r.RequestDate)=mq.Month AND YEAR(r.RequestDate)=mq.Year),0) AS Received " +
            "FROM MONTHLY_QUOTA mq " +
            "JOIN COMMODITY c ON c.CommodityID=mq.CommodityID " +
            "WHERE mq.BeneficiaryID=? " +
            "ORDER BY mq.Year DESC,mq.Month DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, benId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int max = rs.getInt(5);
                    int rec = rs.getInt(6);
                    int rem = max - rec;
                    int pct = max > 0 ? (int) Math.min(100, Math.round(rec * 100.0 / max)) : 0;
                    records.add(new Object[]{
                        rs.getString(1),
                        rs.getString(2),
                        rs.getInt(3),
                        rs.getInt(4),
                        max,
                        rec,
                        rem,
                        pct
                    });
                }
            }
        }
        return records;
    }

    public static FormData getFormData() throws SQLException {
        ensureTimeSlotsExist();
        Connection conn = DB.getConnection();
        FormData data = new FormData();

        // Time slots (only show 4 slots: 2 of tomorrow and 2 of next day)
        java.sql.Date tomorrow = java.sql.Date.valueOf(LocalDate.now().plusDays(1));
        java.sql.Date nextDay = java.sql.Date.valueOf(LocalDate.now().plusDays(2));
        String slotSql = "SELECT SlotID, SlotDate, StartTime, EndTime FROM TIME_SLOT " +
                         "WHERE SlotDate = ? OR SlotDate = ? ORDER BY SlotDate, StartTime";
        try (PreparedStatement ps = conn.prepareStatement(slotSql)) {
            ps.setDate(1, tomorrow);
            ps.setDate(2, nextDay);
            try (ResultSet rs = ps.executeQuery()) {
                int tomorrowCount = 0;
                int nextDayCount = 0;
                while (rs.next()) {
                    java.sql.Date sDate = rs.getDate(2);
                    if (sDate != null) {
                        LocalDate sLocalDate = sDate.toLocalDate();
                        if (sLocalDate.equals(LocalDate.now().plusDays(1))) {
                            if (tomorrowCount < 2) {
                                data.slotIds.add(rs.getInt(1));
                                data.slotLabels.add(rs.getString(2) + "  |  " + rs.getString(3) + " – " + rs.getString(4));
                                tomorrowCount++;
                            }
                        } else if (sLocalDate.equals(LocalDate.now().plusDays(2))) {
                            if (nextDayCount < 2) {
                                data.slotIds.add(rs.getInt(1));
                                data.slotLabels.add(rs.getString(2) + "  |  " + rs.getString(3) + " – " + rs.getString(4));
                                nextDayCount++;
                            }
                        }
                    }
                }
            }
        }

        // Commodities
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT CommodityID,CommodityName,Unit FROM COMMODITY ORDER BY CommodityName")) {
            while (rs.next()) {
                data.comIds.add(rs.getInt(1));
                data.comNames.add(rs.getString(2));
                data.comUnits.add(rs.getString(3));
            }
        }

        return data;
    }

    public static int submitRequest(int benId, int shopId, int slotId, List<Integer> comIds, List<Integer> quantities) throws SQLException {
        ensureMonthlyQuotaExists(benId);
        if (isQuotaExhausted(benId)) {
            throw new SQLException("Quota exhausted. It will be refilled next month on the 1st.");
        }
        
        // Quota check
        List<Object[]> remaining = getRemainingQuotas(benId);
        for (int i = 0; i < comIds.size(); i++) {
            int comId = comIds.get(i);
            int qty = quantities.get(i);
            if (qty > 0) {
                boolean hasQuota = false;
                int remQty = 0;
                String comName = "Commodity " + comId;
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
        }

        Connection conn = DB.getConnection();
        conn.setAutoCommit(false);
        try {
            int reqId = -1;
            String reqSql = "INSERT INTO REQUEST(RequestDate,Status,BeneficiaryID,ShopID,SlotID) VALUES (CURDATE(),'PLACED',?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(reqSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, benId);
                ps.setInt(2, shopId);
                ps.setInt(3, slotId);
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) {
                    if (k.next()) {
                        reqId = k.getInt(1);
                    }
                }
            }

            if (reqId == -1) {
                throw new SQLException("Failed to place request record.");
            }

            String itemSql = "INSERT INTO REQUEST_ITEM(RequestID,CommodityID,QuantityRequested) VALUES (?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                for (int i = 0; i < comIds.size(); i++) {
                    int qty = quantities.get(i);
                    if (qty > 0) {
                        ps.setInt(1, reqId);
                        ps.setInt(2, comIds.get(i));
                        ps.setInt(3, qty);
                        ps.executeUpdate();
                    }
                }
            }

            conn.commit();
            return reqId;
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public static List<Object[]> getRequestHistory(int benId) throws SQLException {
        Connection conn = DB.getConnection();
        List<Object[]> history = new ArrayList<>();
        String sql = 
            "SELECT r.RequestID,r.RequestDate,ts.SlotDate,ts.StartTime,COUNT(ri.RequestItemID),r.Status " +
            "FROM REQUEST r " +
            "JOIN TIME_SLOT ts ON ts.SlotID=r.SlotID " +
            "LEFT JOIN REQUEST_ITEM ri ON ri.RequestID=r.RequestID " +
            "WHERE r.BeneficiaryID=? " +
            "GROUP BY r.RequestID,r.RequestDate,ts.SlotDate,ts.StartTime,r.Status " +
            "ORDER BY r.RequestDate DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, benId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    history.add(new Object[]{
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getInt(5) + " item(s)",
                        rs.getString(6)
                    });
                }
            }
        }
        return history;
    }

    public static void ensureMonthlyQuotaExists(int benId) {
        try {
            Connection conn = DB.getConnection();
            LocalDate now = LocalDate.now();
            int month = now.getMonthValue();
            int year = now.getYear();
            
            // 1. Get CategoryID of this beneficiary
            int catId = -1;
            String catSql = "SELECT CategoryID FROM BENEFICIARY WHERE BeneficiaryID=?";
            try (PreparedStatement ps = conn.prepareStatement(catSql)) {
                ps.setInt(1, benId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        catId = rs.getInt(1);
                    }
                }
            }
            
            if (catId == -1) {
                return;
            }
            
            // 2. Fetch all allocation rules for this category
            List<Object[]> rules = new ArrayList<>();
            String ruleSql = "SELECT CommodityID, MonthlyQuota FROM ALLOCATION_RULE WHERE CategoryID=?";
            try (PreparedStatement ps = conn.prepareStatement(ruleSql)) {
                ps.setInt(1, catId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rules.add(new Object[]{rs.getInt(1), rs.getInt(2)});
                    }
                }
            }
            
            // 3. For each rule, ensure it is represented in MONTHLY_QUOTA for the current month/year
            for (Object[] rule : rules) {
                int comId = (int) rule[0];
                int ruleQuota = (int) rule[1];
                
                int existingQuotaId = -1;
                int existingMaxAllowed = -1;
                String checkSql = "SELECT QuotaID, MaxAllowedQuantity FROM MONTHLY_QUOTA WHERE BeneficiaryID=? AND CommodityID=? AND Month=? AND Year=?";
                try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                    ps.setInt(1, benId);
                    ps.setInt(2, comId);
                    ps.setInt(3, month);
                    ps.setInt(4, year);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            existingQuotaId = rs.getInt(1);
                            existingMaxAllowed = rs.getInt(2);
                        }
                    }
                }
                
                if (existingQuotaId == -1) {
                    // Does not exist, insert it
                    int maxId = 0;
                    try (Statement st = conn.createStatement();
                         ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(QuotaID),0) FROM MONTHLY_QUOTA")) {
                        if (rs.next()) {
                            maxId = rs.getInt(1);
                        }
                    }
                    String insertSql = "INSERT INTO MONTHLY_QUOTA(QuotaID,BeneficiaryID,CommodityID,Month,Year,MaxAllowedQuantity) VALUES(?,?,?,?,?,?)";
                    try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                        ps.setInt(1, maxId + 1);
                        ps.setInt(2, benId);
                        ps.setInt(3, comId);
                        ps.setInt(4, month);
                        ps.setInt(5, year);
                        ps.setInt(6, ruleQuota);
                        ps.executeUpdate();
                    }
                } else {
                    // Exists, check if MaxAllowedQuantity needs to be updated to match the new allocation rule
                    if (existingMaxAllowed != ruleQuota) {
                        String updateSql = "UPDATE MONTHLY_QUOTA SET MaxAllowedQuantity=? WHERE QuotaID=?";
                        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                            ps.setInt(1, ruleQuota);
                            ps.setInt(2, existingQuotaId);
                            ps.executeUpdate();
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void ensureTimeSlotsExist() {
        try {
            Connection conn = DB.getConnection();
            LocalDate today = LocalDate.now();
            for (int i = 0; i <= 2; i++) {
                LocalDate targetDate = today.plusDays(i);
                java.sql.Date sqlDate = java.sql.Date.valueOf(targetDate);
                
                boolean exists = false;
                try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM TIME_SLOT WHERE SlotDate=?")) {
                    ps.setDate(1, sqlDate);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            exists = true;
                        }
                    }
                }
                
                if (!exists) {
                    int maxId = 0;
                    try (Statement st = conn.createStatement();
                         ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(SlotID),0) FROM TIME_SLOT")) {
                        if (rs.next()) {
                            maxId = rs.getInt(1);
                        }
                    }
                    
                    String[][] times = {
                        {"09:00:00", "11:00:00"},
                        {"11:00:00", "13:00:00"},
                        {"14:00:00", "16:00:00"},
                        {"16:00:00", "18:00:00"}
                    };
                    
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO TIME_SLOT(SlotID,SlotDate,StartTime,EndTime,MaxOrders) VALUES(?,?,?,?,20)")) {
                        for (String[] t : times) {
                            maxId++;
                            ps.setInt(1, maxId);
                            ps.setDate(2, sqlDate);
                            ps.setTime(3, java.sql.Time.valueOf(t[0]));
                            ps.setTime(4, java.sql.Time.valueOf(t[1]));
                            ps.executeUpdate();
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void changePassword(int benId, String oldPass, String newPass) throws SQLException {
        Connection conn = DB.getConnection();
        String checkSql = "SELECT 1 FROM USERS WHERE Role='BENEFICIARY' AND LinkedID=? AND PasswordHash=SHA2(?,256)";
        try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
            ps.setInt(1, benId);
            ps.setString(2, oldPass);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Current password is incorrect.");
                }
            }
        }
        
        String updateSql = "UPDATE USERS SET PasswordHash=SHA2(?,256) WHERE Role='BENEFICIARY' AND LinkedID=?";
        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            ps.setString(1, newPass);
            ps.setInt(2, benId);
            ps.executeUpdate();
        }
    }

    public static List<Object[]> getAvailableShops() throws SQLException {
        Connection conn = DB.getConnection();
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT ShopID, ShopName, Location, Capacity FROM FAIR_PRICE_SHOP ORDER BY ShopName";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getInt(4)
                });
            }
        }
        return list;
    }

    public static void transferShop(int benId, int shopId) throws SQLException {
        Connection conn = DB.getConnection();
        String sql = "UPDATE BENEFICIARY SET ShopID=? WHERE BeneficiaryID=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, shopId);
            ps.setInt(2, benId);
            ps.executeUpdate();
        }
    }

    public static List<Object[]> getRemainingQuotas(int benId) throws SQLException {
        ensureMonthlyQuotaExists(benId);
        Connection conn = DB.getConnection();
        List<Object[]> list = new ArrayList<>();
        String sql = 
            "SELECT c.CommodityID, c.CommodityName, c.Unit, mq.MaxAllowedQuantity, " +
            "COALESCE((SELECT SUM(dt.QuantityIssued) FROM DISTRIBUTION_TRANSACTION dt JOIN REQUEST r ON r.RequestID=dt.RequestID WHERE r.BeneficiaryID=mq.BeneficiaryID AND dt.CommodityID=mq.CommodityID AND MONTH(r.RequestDate)=mq.Month AND YEAR(r.RequestDate)=mq.Year),0) AS Received " +
            "FROM MONTHLY_QUOTA mq " +
            "JOIN COMMODITY c ON c.CommodityID=mq.CommodityID " +
            "WHERE mq.BeneficiaryID=? AND mq.Month=MONTH(CURDATE()) AND mq.Year=YEAR(CURDATE())";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, benId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int max = rs.getInt(4);
                    int rec = rs.getInt(5);
                    int rem = Math.max(0, max - rec);
                    list.add(new Object[]{
                        rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        max,
                        rec,
                        rem
                    });
                }
            }
        }
        return list;
    }

    public static boolean isQuotaExhausted(int benId) throws SQLException {
        ensureMonthlyQuotaExists(benId);
        List<Object[]> remaining = getRemainingQuotas(benId);
        if (remaining.isEmpty()) {
            return false;
        }
        int totalRemaining = 0;
        for (Object[] r : remaining) {
            totalRemaining += (int) r[5];
        }
        return totalRemaining == 0;
    }
}
