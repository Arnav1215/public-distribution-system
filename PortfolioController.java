import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PortfolioController {
    public static class BeneficiaryInfo {
        public final String name;
        public final String rationCardNo;
        public final String status;
        public final String categoryName;
        public final String shopName;

        public BeneficiaryInfo(String name, String rationCardNo, String status, String categoryName, String shopName) {
            this.name = name;
            this.rationCardNo = rationCardNo;
            this.status = status;
            this.categoryName = categoryName;
            this.shopName = shopName;
        }
    }

    public static class PortfolioData {
        public final BeneficiaryInfo info;
        public final List<Object[]> quotaRows;
        public final List<Object[]> txnRows;

        public PortfolioData(BeneficiaryInfo info, List<Object[]> quotaRows, List<Object[]> txnRows) {
            this.info = info;
            this.quotaRows = quotaRows;
            this.txnRows = txnRows;
        }
    }

    public static PortfolioData getPortfolio(int benId) throws SQLException {
        Connection conn = DB.getConnection();
        BeneficiaryInfo info = null;

        // 1. Beneficiary details
        String benSql =
            "SELECT b.Name, b.RationCardNo, b.Status, c.CategoryName, f.ShopName " +
            "FROM BENEFICIARY b " +
            "JOIN CATEGORY c ON c.CategoryID = b.CategoryID " +
            "JOIN FAIR_PRICE_SHOP f ON f.ShopID = b.ShopID " +
            "WHERE b.BeneficiaryID = ?";
        try (PreparedStatement ps = conn.prepareStatement(benSql)) {
            ps.setInt(1, benId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    info = new BeneficiaryInfo(
                        rs.getString("Name"),
                        rs.getString("RationCardNo"),
                        rs.getString("Status"),
                        rs.getString("CategoryName"),
                        rs.getString("ShopName")
                    );
                }
            }
        }

        if (info == null) {
            return null; // Not found
        }

        // 2. Monthly quota
        List<Object[]> quotaRows = new ArrayList<>();
        String quotaSql =
            "SELECT c.CommodityName, mq.Month, mq.Year, mq.MaxAllowedQuantity " +
            "FROM MONTHLY_QUOTA mq JOIN COMMODITY c ON c.CommodityID = mq.CommodityID " +
            "WHERE mq.BeneficiaryID = ? ORDER BY mq.Year DESC, mq.Month DESC";
        try (PreparedStatement ps = conn.prepareStatement(quotaSql)) {
            ps.setInt(1, benId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    quotaRows.add(new Object[]{
                        rs.getString(1), 
                        rs.getInt(2), 
                        rs.getInt(3), 
                        rs.getInt(4)
                    });
                }
            }
        }

        // 3. Transaction history
        List<Object[]> txnRows = new ArrayList<>();
        String txnSql =
            "SELECT dt.TransactionID, c.CommodityName, dt.QuantityIssued, r.RequestDate " +
            "FROM DISTRIBUTION_TRANSACTION dt " +
            "JOIN REQUEST r ON r.RequestID = dt.RequestID " +
            "JOIN COMMODITY c ON c.CommodityID = dt.CommodityID " +
            "WHERE r.BeneficiaryID = ? ORDER BY r.RequestDate DESC";
        try (PreparedStatement ps = conn.prepareStatement(txnSql)) {
            ps.setInt(1, benId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    txnRows.add(new Object[]{
                        rs.getInt(1), 
                        rs.getString(2), 
                        rs.getInt(3), 
                        rs.getString(4)
                    });
                }
            }
        }

        return new PortfolioData(info, quotaRows, txnRows);
    }
}
