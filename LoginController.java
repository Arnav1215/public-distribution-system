import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginController {

    public static class UserSession {
        public final int userId;
        public final String username;
        public final String role;
        public final int linkedId;
        public UserSession(int userId, String username, String role, int linkedId) {
            this.userId = userId;
            this.username = username;
            this.role = role;
            this.linkedId = linkedId;
        }
    }

    public static UserSession authenticate(String username, String password) throws SQLException {
        Connection conn = DB.getConnection();
        String sql = "SELECT UserID, Username, Role, LinkedID FROM USERS WHERE Username=? AND PasswordHash=SHA2(?,256)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int linkedId = rs.getObject("LinkedID") != null ? rs.getInt("LinkedID") : -1;
                    return new UserSession(
                        rs.getInt("UserID"),
                        rs.getString("Username"),
                        rs.getString("Role"),
                        linkedId
                    );
                }
            }
        }
        return null; // authentication failed
    }
}
