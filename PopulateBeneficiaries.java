import java.sql.*;
import java.time.LocalDate;

public class PopulateBeneficiaries {
    public static void main(String[] args) {
        Connection conn = null;
        try {
            conn = DB.getConnection();
            conn.setAutoCommit(false);

            System.out.println("Cleaning up existing beneficiary data...");
            
            // Delete dependent records in correct dependency order
            String[] cleanupSqls = {
                "DELETE dt FROM DISTRIBUTION_TRANSACTION dt JOIN REQUEST r ON r.RequestID=dt.RequestID",
                "DELETE ri FROM REQUEST_ITEM ri JOIN REQUEST r ON r.RequestID=ri.RequestID",
                "DELETE FROM REQUEST",
                "DELETE FROM MONTHLY_QUOTA",
                "DELETE FROM USERS WHERE Role='BENEFICIARY'",
                "DELETE FROM BENEFICIARY"
            };

            for (String sql : cleanupSqls) {
                try (Statement stmt = conn.createStatement()) {
                    int rows = stmt.executeUpdate(sql);
                    System.out.println("Executed: " + sql + " (Rows affected: " + rows + ")");
                }
            }

            System.out.println("Generating 100 new beneficiaries...");

            // Prepare statements for insertion
            String insertBenSql = "INSERT INTO BENEFICIARY(BeneficiaryID,Name,RationCardNo,DOB,Status,CategoryID,ShopID) VALUES(?,?,?,?,'ACTIVE',?,?)";
            String insertUserSql = "INSERT INTO USERS(Username,PasswordHash,Role,LinkedID) VALUES(?,SHA2(?,256),'BENEFICIARY',?)";

            try (PreparedStatement psBen = conn.prepareStatement(insertBenSql);
                 PreparedStatement psUser = conn.prepareStatement(insertUserSql)) {

                int[] categories = {1, 2, 3, 4}; // APL, BPL, PHH, AAY
                int[] shops = {101, 102, 103, 104};

                for (int i = 1; i <= 100; i++) {
                    int benId = i;
                    String name = "Beneficiary " + i;
                    String rationCardNo = "RC" + (10000 + i);
                    String dob = "1990-01-01";
                    int categoryId = categories[(i - 1) % categories.length];
                    int shopId = shops[(i - 1) % shops.length];

                    // unique username
                    String username = "ben" + i;
                    String password = "pass123";

                    // Insert into BENEFICIARY
                    psBen.setInt(1, benId);
                    psBen.setString(2, name);
                    psBen.setString(3, rationCardNo);
                    psBen.setString(4, dob);
                    psBen.setInt(5, categoryId);
                    psBen.setInt(6, shopId);
                    psBen.executeUpdate();

                    // Insert into USERS
                    psUser.setString(1, username);
                    psUser.setString(2, password);
                    psUser.setInt(3, benId);
                    psUser.executeUpdate();
                }
            }

            conn.commit();
            System.out.println("Successfully committed 100 beneficiaries!");

            // Generate monthly quotas using the controller logic
            System.out.println("Generating monthly quotas for new beneficiaries...");
            for (int i = 1; i <= 100; i++) {
                BeneficiaryController.ensureMonthlyQuotaExists(i);
            }
            System.out.println("Monthly quotas generated successfully!");

        } catch (Exception e) {
            System.err.println("Transaction failed, rolling back!");
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            DB.closeConnection();
        }
    }
}
