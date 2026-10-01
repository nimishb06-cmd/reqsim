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

            System.out.print("Enter Choice: ");
            int ch = sc.nextInt();
            sc.nextLine();

            switch (ch) {

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
                    System.out.println("Invalid Choice");
            }
        }
    }

    // 1. Registration
    static void register() {

        try {

            Connection con = DBConnection.getConnection();

            System.out.print("Name: ");
            String name = sc.nextLine();

            System.out.print("Phone: ");
            String phone = sc.nextLine();

            String sql = "INSERT INTO VICTIM(name,phone,status) VALUES(?,?,?)";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, "Registered");

            ps.executeUpdate();

            System.out.println("Victim Registered Successfully!");

            con.close();

        } catch (Exception e) {
            System.out.println(e);
        }
    }

// 2. Create SOS
    static void createSOS() {

        try {

            Connection con = DBConnection.getConnection();

            System.out.print("Victim ID: ");
            int id = sc.nextInt();
            sc.nextLine();

            System.out.print("Location: ");
            String location = sc.nextLine();

            System.out.print("Emergency Type: ");
            String type = sc.nextLine();
 
            System.out.print("Severity (1-4): ");
            int severity = sc.nextInt();
   
            String sql = "UPDATE VICTIM SET location=?, emergency_type=?, severity=?, status=? WHERE victim_id=?";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, location);
            ps.setString(2, type);
            ps.setInt(3, severity);
            ps.setString(4, "SOS Pending");
            ps.setInt(5, id);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("\nSOS Created Successfully!");
                System.out.println("Status : SOS Pending");
            } else {
            System.out.println("Invalid Victim ID!");
        }

            con.close();

        } catch (Exception e) {
        System.out.println("Error: " + e.getMessage());
    }
}
    // 3. Track Request
    static void trackRequest() {

        try {

            Connection con = DBConnection.getConnection();

            System.out.print("Victim ID: ");
            int id = sc.nextInt();

            String sql = "SELECT * FROM VICTIM WHERE victim_id=?";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1, id);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                System.out.println("\n----- REQUEST STATUS -----");
                System.out.println("Victim ID      : " + rs.getInt("victim_id"));
                System.out.println("Name           : " + rs.getString("name"));
                System.out.println("Location       : " + rs.getString("location"));
                System.out.println("Emergency Type : " + rs.getString("emergency_type"));
                System.out.println("Status         : " + rs.getString("status"));

            } else {

                System.out.println("Victim Not Found");
            }

            con.close();

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // 4. Find Shelters
    static void findShelters() {

        try {

            Connection con = DBConnection.getConnection();

            String sql = "SELECT * FROM SHELTER_MANAGER WHERE available_beds>0";

            Statement st = con.createStatement();

            ResultSet rs = st.executeQuery(sql);

            System.out.println("\n----- AVAILABLE SHELTERS -----");

            while (rs.next()) {

                System.out.println("Shelter ID : " + rs.getInt("shelter_id"));
                System.out.println("Name       : " + rs.getString("shelter_name"));
                System.out.println("Address    : " + rs.getString("address"));
                System.out.println("Beds       : " + rs.getInt("available_beds"));
                System.out.println("----------------------------");
            }

            con.close();

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // 5. Request Resource
static void requestResource() {

    try {

        Connection con = DBConnection.getConnection();

        System.out.print("Victim ID: ");
        int id = sc.nextInt();
        sc.nextLine();

        System.out.print("Resource Needed: ");
        String resource = sc.nextLine();

        String sql = "UPDATE VICTIM SET resource_request=?, resource_status=? WHERE victim_id=?";

        PreparedStatement ps = con.prepareStatement(sql);

        ps.setString(1, resource);
        ps.setString(2, "Resource Requested");
        ps.setInt(3, id);

        int rows = ps.executeUpdate();

        if (rows > 0)
            System.out.println("Resource Request Sent Successfully!");
        else
            System.out.println("Invalid Victim ID!");

        con.close();

    } catch (Exception e) {
        System.out.println(e);
    }
}

}   
