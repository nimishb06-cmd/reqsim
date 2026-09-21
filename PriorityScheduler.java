import java.sql.*;

public class PriorityScheduler {

    public static void run() {

        try (Connection con = Database.getConnection()) {

            String sql =
                    "SELECT request_id, location, priority " +
                    "FROM rescue_request " +
                    "WHERE status='PENDING' " +
                    "ORDER BY priority DESC, created_at ASC";

            Statement st = con.createStatement();

            ResultSet rs = st.executeQuery(sql);

            System.out.println(
                    "\n===== PRIORITY QUEUE =====");

            while (rs.next()) {

                System.out.println(
                        "Request ID: "
                        + rs.getInt("request_id")
                        + " | Priority: "
                        + rs.getInt("priority")
                        + " | Location: "
                        + rs.getString("location"));
            }

        } catch (Exception e) {

            System.out.println(e.getMessage());
        }
    }
}