import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(
                "jdbc:mysql://127.0.0.1:3307/disaster_db",
                "root",
                ""
            );
        } catch (Exception e) {
            System.out.println("Database Error: " + e.getMessage());
            return null;
        }
    }
}
