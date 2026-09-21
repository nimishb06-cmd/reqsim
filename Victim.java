import java.sql.*;
import java.util.Scanner;

public class Victim {

    static Scanner sc = new Scanner(System.in);

    public static void menu() {

        while (true) {

            System.out.println("\n===== VICTIM MODULE =====");
            System.out.println("1. Register");
            System.out.println("2. Create SOS");
            System.out.println("3. Track Request");
            System.out.println("4. Find Available Shelters");
            System.out.println("5. Request Resource");
            System.out.println("6. Back");

            System.out.print("Enter choice: ");
            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {

                case 1:
                    register();
                    break;

                case 2:
                    createSOS();
                    break;

                case 3:
                    trackRequest();
                    break;

                case 4:
                    findShelters();
                    break;

                case 5:
                    requestResource();
                    break;

                case 6:
                    return;

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    static void register() {

        try (Connection con = Database.getConnection()) {

            System.out.print("Name: ");
            String name = sc.nextLine();

            System.out.print("Email: ");
            String email = sc.nextLine();

            System.out.print("Phone: ");
            String phone = sc.nextLine();

            System.out.print("Password: ");
            String password = sc.nextLine();

            con.setAutoCommit(false);

            String sql =
                    "INSERT INTO users(name,email,phone,password,role) " +
                    "VALUES(?,?,?,?,?)";

            PreparedStatement ps =
                    con.prepareStatement(
                            sql,
                            Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.setString(4, password);
            ps.setString(5, "VICTIM");

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();

            int userId = 0;

            if (rs.next()) {
                userId = rs.getInt(1);
            }

            PreparedStatement ps2 =
                    con.prepareStatement(
                            "INSERT INTO victim(user_id) VALUES(?)");

            ps2.setInt(1, userId);
            ps2.executeUpdate();

            con.commit();

            System.out.println("Victim registered successfully.");
            System.out.println("Victim ID = " + userId);

        } catch (Exception e) {

            System.out.println("Registration failed: "
                    + e.getMessage());
        }
    }

    static void createSOS() {

        try (Connection con = Database.getConnection()) {

            System.out.print("Victim ID: ");
            int victimId = sc.nextInt();

            System.out.print("Disaster ID: ");
            int disasterId = sc.nextInt();
            sc.nextLine();

            System.out.print("Location: ");
            String location = sc.nextLine();

            System.out.print("Emergency Type: ");
            String type = sc.nextLine();

            System.out.print("Priority (1-5): ");
            int priority = sc.nextInt();

            if (priority < 1 || priority > 5) {
                System.out.println("Priority must be 1-5.");
                return;
            }

            String sql =
                    "INSERT INTO rescue_request " +
                    "(victim_id,disaster_id,location," +
                    "emergency_type,priority) " +
                    "VALUES(?,?,?,?,?)";

            PreparedStatement ps =
                    con.prepareStatement(sql);

            ps.setInt(1, victimId);
            ps.setInt(2, disasterId);
            ps.setString(3, location);
            ps.setString(4, type);
            ps.setInt(5, priority);

            ps.executeUpdate();

            System.out.println("SOS created successfully.");

        } catch (Exception e) {

            System.out.println("Error: " + e.getMessage());
        }
    }

    static void trackRequest() {

        try (Connection con = Database.getConnection()) {

            System.out.print("Victim ID: ");
            int id = sc.nextInt();

            String sql =
                    "SELECT request_id,location," +
                    "emergency_type,priority,status " +
                    "FROM rescue_request " +
                    "WHERE victim_id=?";

            PreparedStatement ps =
                    con.prepareStatement(sql);

            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                System.out.println(
                        "Request ID: " +
                        rs.getInt("request_id"));

                System.out.println(
                        "Location: " +
                        rs.getString("location"));

                System.out.println(
                        "Emergency: " +
                        rs.getString("emergency_type"));

                System.out.println(
                        "Priority: " +
                        rs.getInt("priority"));

                System.out.println(
                        "Status: " +
                        rs.getString("status"));

                System.out.println("-------------------");
            }

        } catch (Exception e) {

            System.out.println("Error: " + e.getMessage());
        }
    }

    static void findShelters() {

        try (Connection con = Database.getConnection()) {

            String sql =
                    "SELECT * FROM shelter " +
                    "WHERE occupied < capacity";

            Statement st = con.createStatement();

            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {

                System.out.println(
                        rs.getInt("shelter_id")
                        + " | "
                        + rs.getString("name")
                        + " | "
                        + rs.getString("location")
                        + " | Available: "
                        + (rs.getInt("capacity")
                        - rs.getInt("occupied")));
            }

        } catch (Exception e) {

            System.out.println("Error: " + e.getMessage());
        }
    }

    static void requestResource() {

        try (Connection con = Database.getConnection()) {

            System.out.print("Request ID: ");
            int requestId = sc.nextInt();

            System.out.print("Resource ID: ");
            int resourceId = sc.nextInt();

            System.out.print("Quantity: ");
            int quantity = sc.nextInt();

            String sql =
                    "INSERT INTO resource_allocation" +
                    "(resource_id,request_id,quantity) " +
                    "VALUES(?,?,?)";

            PreparedStatement ps =
                    con.prepareStatement(sql);

            ps.setInt(1, resourceId);
            ps.setInt(2, requestId);
            ps.setInt(3, quantity);

            ps.executeUpdate();

            System.out.println(
                    "Resource request submitted.");

        } catch (Exception e) {

            System.out.println("Error: " + e.getMessage());
        }
    }
}
