import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class StockController {
    public static class StockData {
        public final List<Object[]> rows;
        public final int lowCount;
        public final int okCount;

        public StockData(List<Object[]> rows, int lowCount, int okCount) {
            this.rows = rows;
            this.lowCount = lowCount;
            this.okCount = okCount;
        }
    }

    public static StockData getStockAnalysis() throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        int low = 0, ok = 0;
        String sql =
            "SELECT f.ShopName, f.Location, c.CommodityName, s.QuantityAvailable, " +
            "CASE WHEN s.QuantityAvailable < 50 THEN 'LOW' ELSE 'OK' END AS StockStatus " +
            "FROM STOCK s " +
            "JOIN FAIR_PRICE_SHOP f ON f.ShopID = s.ShopID " +
            "JOIN COMMODITY c ON c.CommodityID = s.CommodityID " +
            "ORDER BY f.ShopName, c.CommodityName";

        Connection conn = DB.getConnection();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String status = rs.getString("StockStatus");
                rows.add(new Object[]{
                    rs.getString("ShopName"), 
                    rs.getString("Location"),
                    rs.getString("CommodityName"), 
                    rs.getInt("QuantityAvailable"), 
                    status
                });
                if ("LOW".equals(status)) {
                    low++;
                } else {
                    ok++;
                }
            }
        }
        return new StockData(rows, low, ok);
    }
}
