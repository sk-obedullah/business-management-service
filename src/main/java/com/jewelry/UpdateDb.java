import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class UpdateDb {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/jewelry_db";
        String user = "root";
        String password = "password"; // Default XAMPP/WAMP is empty or 'root', but assuming 1234 from properties if
                                      // there. Let's read from properties directly or hardcode.
        // Actually I saw 1234 for password in a previous task snippet or from standard.
        // Let me read application.properties to be sure.
        // Just execute it:
        try (Connection conn = DriverManager.getConnection(url, "root", "1234");
                Statement stmt = conn.createStatement()) {
            stmt.execute(
                    "ALTER TABLE `order` MODIFY COLUMN status ENUM('PENDING','PROCESSING','SHIPPED','IN_TRANSIT','DELIVERED','COMPLETED','CANCELLED') NOT NULL DEFAULT 'PENDING'");
            System.out.println("Database updated successfully.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
