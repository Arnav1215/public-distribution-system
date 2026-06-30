import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AuditController {
    public static List<Object[]> getAuditLogs() throws SQLException {
        List<Object[]> logs = new ArrayList<>();
        String sql = "SELECT LogID, EntityName, EntityID, OperationType, PerformedBy, Timestamp " +
                     "FROM AUDIT_LOG ORDER BY LogID DESC";
        Connection conn = DB.getConnection();
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                logs.add(new Object[]{
                    rs.getInt(1), 
                    rs.getString(2), 
                    rs.getInt(3),
                    rs.getString(4), 
                    rs.getString(5), 
                    rs.getString(6)
                });
            }
        }
        return logs;
    }
}
