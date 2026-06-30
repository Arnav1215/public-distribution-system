import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AnalyticsController {

    /**
     * Admin: Total quantity distributed per commodity.
     * Returns a list of arrays where Object[0] is the CommodityName and Object[1] is the Integer quantity issued.
     */
    public static List<Object[]> getAdminCommodityStats() throws SQLException {
        Connection conn = DB.getConnection();
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT c.CommodityName, COALESCE(SUM(dt.QuantityIssued), 0) AS TotalIssued " +
                     "FROM COMMODITY c " +
                     "LEFT JOIN DISTRIBUTION_TRANSACTION dt ON c.CommodityID = dt.CommodityID " +
                     "GROUP BY c.CommodityName " +
                     "ORDER BY TotalIssued DESC LIMIT 8";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Object[]{ rs.getString(1), rs.getInt(2) });
            }
        }
        return list;
    }

    /**
     * Admin: Distribution transaction volume trends over the last 7 days.
     * Returns a list of arrays where Object[0] is the date string (e.g. "06/24") and Object[1] is the Integer transaction count.
     */
    public static List<Object[]> getAdminDailyTrend() throws SQLException {
        Connection conn = DB.getConnection();
        List<Object[]> list = new ArrayList<>();
        // Select last 7 days count of distributions
        String sql = "SELECT DATE_FORMAT(r.RequestDate, '%m/%d') as dt, COUNT(dt.TransactionID) as cnt " +
                     "FROM REQUEST r " +
                     "JOIN DISTRIBUTION_TRANSACTION dt ON r.RequestID = dt.RequestID " +
                     "GROUP BY r.RequestDate " +
                     "ORDER BY r.RequestDate DESC LIMIT 7";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Object[]{ rs.getString(1), rs.getInt(2) });
            }
        }
        // If empty, return fallback data based on actual db request dates to make sure graph draws something
        if (list.isEmpty()) {
            String fallbackSql = "SELECT DATE_FORMAT(RequestDate, '%m/%d') as dt, COUNT(*) as cnt " +
                                 "FROM REQUEST GROUP BY RequestDate ORDER BY RequestDate DESC LIMIT 7";
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(fallbackSql)) {
                while (rs.next()) {
                    list.add(new Object[]{ rs.getString(1), rs.getInt(2) });
                }
            }
        }
        return list;
    }

    /**
     * Shop: Current stock levels per commodity for this shop.
     * Returns list of arrays where Object[0] is CommodityName and Object[1] is Integer QuantityAvailable.
     */
    public static List<Object[]> getShopStockStats(int shopId) throws SQLException {
        Connection conn = DB.getConnection();
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT c.CommodityName, COALESCE(s.QuantityAvailable, 0) " +
                     "FROM STOCK s " +
                     "JOIN COMMODITY c ON c.CommodityID = s.CommodityID " +
                     "WHERE s.ShopID = ? " +
                     "ORDER BY c.CommodityName LIMIT 8";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, shopId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[]{ rs.getString(1), rs.getInt(2) });
                }
            }
        }
        return list;
    }

    /**
     * Shop: Distribution transaction volume trends over the last 7 days for this shop.
     */
    public static List<Object[]> getShopDailyTrend(int shopId) throws SQLException {
        Connection conn = DB.getConnection();
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT DATE_FORMAT(r.RequestDate, '%m/%d') as dt, COUNT(dt.TransactionID) as cnt " +
                     "FROM REQUEST r " +
                     "JOIN DISTRIBUTION_TRANSACTION dt ON r.RequestID = dt.RequestID " +
                     "WHERE r.ShopID = ? " +
                     "GROUP BY r.RequestDate " +
                     "ORDER BY r.RequestDate DESC LIMIT 7";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, shopId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Object[]{ rs.getString(1), rs.getInt(2) });
                }
            }
        }
        // Fallback to request count if no distribution transactions exist yet
        if (list.isEmpty()) {
            String fallbackSql = "SELECT DATE_FORMAT(RequestDate, '%m/%d') as dt, COUNT(*) as cnt " +
                                 "FROM REQUEST WHERE ShopID = ? GROUP BY RequestDate ORDER BY RequestDate DESC LIMIT 7";
            try (PreparedStatement ps = conn.prepareStatement(fallbackSql)) {
                ps.setInt(1, shopId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new Object[]{ rs.getString(1), rs.getInt(2) });
                    }
                }
            }
        }
        return list;
    }

    /**
     * Beneficiary: Quota consumption percentage for the current month.
     */
    public static int getBeneficiaryQuotaUsagePct(int benId) throws SQLException {
        Connection conn = DB.getConnection();
        // Calculate total MaxAllowedQuantity and total received quantities
        String sql = "SELECT SUM(mq.MaxAllowedQuantity), " +
                     "COALESCE(SUM((SELECT SUM(dt.QuantityIssued) FROM DISTRIBUTION_TRANSACTION dt " +
                     "              JOIN REQUEST r ON r.RequestID = dt.RequestID " +
                     "              WHERE r.BeneficiaryID = mq.BeneficiaryID " +
                     "                AND dt.CommodityID = mq.CommodityID " +
                     "                AND MONTH(r.RequestDate) = mq.Month " +
                     "                AND YEAR(r.RequestDate) = mq.Year)), 0) " +
                     "FROM MONTHLY_QUOTA mq " +
                     "WHERE mq.BeneficiaryID = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, benId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int max = rs.getInt(1);
                    int rec = rs.getInt(2);
                    if (max > 0) {
                        return (int) Math.min(100, Math.round(rec * 100.0 / max));
                    }
                }
            }
        }
        return 0;
    }

    /**
     * System Activity Timeline: Recent requests, audits, and distributions.
     * Returns list of arrays: [Time, Operation/Action, Entity/Details, Performed By]
     */
    public static List<Object[]> getSystemActivityTimeline() throws SQLException {
        Connection conn = DB.getConnection();
        List<Object[]> list = new ArrayList<>();
        // Query recent 12 entries from AUDIT_LOG or REQUESTs/DISTRIBUTIONs
        String sql = "SELECT DATE_FORMAT(Timestamp, '%H:%i') as tm, OperationType, EntityName, PerformedBy " +
                     "FROM AUDIT_LOG " +
                     "ORDER BY LogID DESC LIMIT 12";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getString(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4)
                });
            }
        }
        return list;
    }
}
