import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DB {
    private static final String DB_URL  = "jdbc:mysql://localhost:3306/PDS_DB?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "*****";
    
    private static Connection conn;

    public static synchronized Connection getConnection() throws SQLException {
        if (conn == null || conn.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL JDBC Driver not found", e);
            }
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        }
        return conn;
    }

    public static synchronized void closeConnection() {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                conn = null;
            }
        }
    }
}
