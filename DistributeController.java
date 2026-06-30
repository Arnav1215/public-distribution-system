import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DistributeController {
    
    public static class RequestHeader {
        public final String benName;
        public final String requestDate;
        public final String status;
        public final int shopId;

        public RequestHeader(String benName, String requestDate, String status, int shopId) {
            this.benName = benName;
            this.requestDate = requestDate;
            this.status = status;
            this.shopId = shopId;
        }
    }

    public static class DistributeDetails {
        public final RequestHeader header;
        public final List<Object[]> items;

        public DistributeDetails(RequestHeader header, List<Object[]> items) {
            this.header = header;
            this.items = items;
        }
    }

    public static class DistributionResult {
        public final boolean success;
        public final String logOutput;

        public DistributionResult(boolean success, String logOutput) {
            this.success = success;
            this.logOutput = logOutput;
        }
    }

    public static DistributeDetails getRequestDetails(int reqId) throws SQLException {
        Connection conn = DB.getConnection();
        RequestHeader header = null;

        String reqSql =
            "SELECT b.Name, r.RequestDate, r.Status, r.ShopID " +
            "FROM REQUEST r JOIN BENEFICIARY b ON b.BeneficiaryID = r.BeneficiaryID " +
            "WHERE r.RequestID = ?";
        
        try (PreparedStatement ps = conn.prepareStatement(reqSql)) {
            ps.setInt(1, reqId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    header = new RequestHeader(
                        rs.getString("Name"),
                        rs.getString("RequestDate"),
                        rs.getString("Status"),
                        rs.getInt("ShopID")
                    );
                }
            }
        }

        if (header == null) {
            return null;
        }

        List<Object[]> items = new ArrayList<>();
        String itemSql =
            "SELECT ri.CommodityID, c.CommodityName, ri.QuantityRequested, " +
            "COALESCE(s.QuantityAvailable, 0) AS Stock " +
            "FROM REQUEST_ITEM ri " +
            "JOIN COMMODITY c ON c.CommodityID = ri.CommodityID " +
            "LEFT JOIN STOCK s ON s.CommodityID = ri.CommodityID AND s.ShopID = ? " +
            "WHERE ri.RequestID = ?";
        
        try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
            ps.setInt(1, header.shopId);
            ps.setInt(2, reqId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int qty = rs.getInt("QuantityRequested");
                    int stock = rs.getInt("Stock");
                    items.add(new Object[]{
                        rs.getInt("CommodityID"),
                        rs.getString("CommodityName"),
                        qty,
                        stock,
                        stock >= qty ? "YES" : "NO"
                    });
                }
            }
        }

        return new DistributeDetails(header, items);
    }

    public static DistributionResult executeDistribution(int reqId, List<Object[]> items) {
        StringBuilder log = new StringBuilder();
        Connection conn = null;
        try {
            conn = DB.getConnection();
            conn.setAutoCommit(false);

            boolean allOk = true;
            String ins = "INSERT INTO DISTRIBUTION_TRANSACTION " +
                         "(RequestID, CommodityID, QuantityIssued) VALUES (?, ?, ?)";
            
            try (PreparedStatement ps = conn.prepareStatement(ins)) {
                for (Object[] item : items) {
                    int comId = (int) item[0];
                    String comName = (String) item[1];
                    int qty = (int) item[2];
                    
                    ps.setInt(1, reqId);
                    ps.setInt(2, comId);
                    ps.setInt(3, qty);
                    
                    try {
                        ps.executeUpdate();
                        log.append("  [TRIGGER 1]  ✔  Stock check PASSED       →  ")
                           .append(comName).append("\n");
                        log.append("  [TRIGGER 2]  ✔  Stock deducted (×").append(qty)
                           .append(")  →  ").append(comName)
                           .append("  |  Audit entry written\n\n");
                    } catch (SQLException ex) {
                        allOk = false;
                        log.append("  [TRIGGER 1]  ✘  BLOCKED — ").append(comName).append("\n")
                           .append("               ↳  ").append(ex.getMessage()).append("\n\n");
                    }
                }
            }

            log.append("─────────────────────────────────────────────────────\n");
            if (allOk) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE REQUEST SET Status = 'FULFILLED' WHERE RequestID = ?")) {
                    ps.setInt(1, reqId);
                    ps.executeUpdate();
                }
                conn.commit();
                log.append("  Request #").append(reqId).append(" → Status updated to FULFILLED\n");
                return new DistributionResult(true, log.toString());
            } else {
                conn.rollback();
                log.append("  Request #").append(reqId).append(" → FAILED — transaction rolled back.\n");
                return new DistributionResult(false, log.toString());
            }

        } catch (SQLException ex) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rbEx) {
                    rbEx.printStackTrace();
                }
            }
            log.append("  TRANSACTION ROLLED BACK: ").append(ex.getMessage());
            return new DistributionResult(false, log.toString());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
