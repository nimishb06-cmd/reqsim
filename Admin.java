import java.sql.*;
import java.util.Scanner;

public class Admin {

    static Scanner sc = new Scanner(System.in);

    // ================= MENU =================
    public static void menu() {

        while (true) {

            System.out.println("\n===== ADMIN MODULE =====");
            System.out.println("1. Register Admin");
            System.out.println("2. User Management");
            System.out.println("3. Create / Update Disaster Event");
            System.out.println("4. Allocate Resources");
            System.out.println("5. Assign Rescue Team");
            System.out.println("6. Assign Volunteers");
            System.out.println("7. Monitor Emergency Requests");
            System.out.println("8. Track Volunteer Availability");
            System.out.println("9. Assign Shelter");
            System.out.println("10. Generate Report");
            System.out.println("11. Back");

            System.out.print("Enter Choice: ");
            int ch = sc.nextInt();
            sc.nextLine();

            switch (ch) {

                case 1: addAdmin(); break;
                case 2: userManagement(); break;
                case 3: disasterEvent(); break;
                case 4: allocateResources(); break;
                case 5: assignRescueTeam(); break;
                case 6: assignVolunteer(); break;
                case 7: monitorRequests(); break;
                case 8: volunteerAvailability(); break;
                case 9: assignShelter(); break;
                case 10: generateReport(); break;
                case 11: return;

                default:
                    System.out.println("Invalid Choice");
            }
        }
    }

    // 1. ADMIN REGISTRATION
    // DBMS : INSERT, Primary Key

    static void addAdmin() {

        try {
            Connection con = DBConnection.getConnection();

            System.out.print("Name : ");
            String name = sc.nextLine();

            System.out.print("Email : ");
            String email = sc.nextLine();

            System.out.print("Password : ");
            String pass = sc.nextLine();

            System.out.print("Phone : ");
            String phone = sc.nextLine();

            String sql = "INSERT INTO ADMIN(name,email,password,phone) VALUES(?,?,?,?)";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, pass);
            ps.setString(4, phone);

            ps.executeUpdate();

            System.out.println("Admin Added Successfully!");

            con.close();

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // ==========================================================
    // 2. USER MANAGEMENT
    // DBMS : SELECT

    static void userManagement() {

        try {

            Connection con = DBConnection.getConnection();

            Statement st = con.createStatement();

            ResultSet rs = st.executeQuery("SELECT victim_id,name,phone,status FROM VICTIM");

            System.out.println("\n----- REGISTERED USERS -----");

            while (rs.next()) {

                System.out.println(
                        rs.getInt(1) + " | " +
                        rs.getString(2) + " | " +
                        rs.getString(3) + " | " +
                        rs.getString(4));
            }

            con.close();

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // ==========================================================
    // 3. CREATE / UPDATE DISASTER EVENT
    // DBMS : UPDATE + Transaction
    // ==========================================================

    static void disasterEvent() {

        try {

            Connection con = DBConnection.getConnection();

            con.setAutoCommit(false);

            System.out.print("Victim ID : ");
            int id = sc.nextInt();
            sc.nextLine();

            System.out.print("Disaster Type : ");
            String type = sc.nextLine();

            System.out.print("Location : ");
            String loc = sc.nextLine();

            String sql = "UPDATE VICTIM SET emergency_type=?,location=?,status=? WHERE victim_id=?";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, type);
            ps.setString(2, loc);
            ps.setString(3, "Emergency Active");
            ps.setInt(4, id);

            ps.executeUpdate();

            con.commit();

            System.out.println("Disaster Event Updated!");

            con.close();

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // ==========================================================
    // 4. ALLOCATE RESOURCES
    // DBMS : UPDATE, Transaction
    // ==========================================================

// 4. Allocate Resources
static void allocateResources() {

    try {

        Connection con = DBConnection.getConnection();

        String sql = "SELECT victim_id, name, resource_request FROM VICTIM WHERE resource_status='Resource Requested'";

        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);

        System.out.println("\n===== RESOURCE REQUESTS =====");
        System.out.println("ID\tName\tResource");

        boolean found = false;

        while (rs.next()) {

            found = true;

            System.out.println(
                rs.getInt("victim_id") + "\t" +
                rs.getString("name") + "\t" +
                rs.getString("resource_request"));
        }

        if (!found) {
            System.out.println("No Resource Requests Found!");
            con.close();
            return;
        }

        System.out.print("\nEnter Victim ID: ");
        int id = sc.nextInt();

        String update = "UPDATE VICTIM SET resource_status='Resources Allocated' WHERE victim_id=?";

        PreparedStatement ps = con.prepareStatement(update);
        ps.setInt(1, id);

        int rows = ps.executeUpdate();

        if (rows > 0)
            System.out.println("Resources Allocated Successfully!");
        else
            System.out.println("Invalid Victim ID!");

        con.close();

    } catch (Exception e) {
        System.out.println(e);
    }
}

    // ==========================================================
    // 5. ASSIGN RESCUE TEAM
    // DBMS : INSERT, Foreign Key
    // ==========================================================
// 5. Assign Rescue Team
static void assignRescueTeam() {

    try {

        Connection con = DBConnection.getConnection();

        // Show available rescue teams
        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(
            "SELECT team_id, team_name, leader, vehicle FROM RESCUE_TEAM WHERE rescue_status='Available'"
        );

        System.out.println("\n===== AVAILABLE RESCUE TEAMS =====");
        System.out.println("ID\tTeam\tLeader\tVehicle");

        while (rs.next()) {
            System.out.println(
                rs.getInt("team_id") + "\t" +
                rs.getString("team_name") + "\t" +
                rs.getString("leader") + "\t" +
                rs.getString("vehicle")
            );
        }

        System.out.print("\nEnter Victim ID: ");
        int victimId = sc.nextInt();

        System.out.print("Enter Team ID: ");
        int teamId = sc.nextInt();

        // Update VICTIM table
        String sql1 = "UPDATE VICTIM SET team_id=?, status=? WHERE victim_id=?";
        PreparedStatement ps1 = con.prepareStatement(sql1);

        ps1.setInt(1, teamId);
        ps1.setString(2, "Approved");
        ps1.setInt(3, victimId);

        int victimRows = ps1.executeUpdate();

        if (victimRows == 0) {
            System.out.println("Invalid Victim ID!");
            con.close();
            return;
        }

        // Update RESCUE_TEAM table
        String sql2 = "UPDATE RESCUE_TEAM SET victim_id=?, rescue_status=? WHERE team_id=?";
        PreparedStatement ps2 = con.prepareStatement(sql2);

        ps2.setInt(1, victimId);
        ps2.setString(2, "Assigned");
        ps2.setInt(3, teamId);

        int teamRows = ps2.executeUpdate();

        if (teamRows == 0) {
            System.out.println("Invalid Team ID!");
        } else {
            System.out.println("Rescue Team Assigned Successfully!");
        }

        con.close();

    } catch (Exception e) {
        System.out.println("Error: " + e.getMessage());
    }
}

    // ==========================================================
    // 6. ASSIGN VOLUNTEER
    // DBMS : INSERT + Transaction
    // ==========================================================

    static void assignVolunteer() {

        try {

            Connection con = DBConnection.getConnection();

            System.out.print("Volunteer Name : ");
            String name = sc.nextLine();

            System.out.print("Phone : ");
            String phone = sc.nextLine();

            System.out.print("Skill : ");
            String skill = sc.nextLine();

            System.out.print("Shelter ID : ");
            int sid = sc.nextInt();

            String sql = "INSERT INTO VOLUNTEER(name,phone,skill,shelter_id) VALUES(?,?,?,?)";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, skill);
            ps.setInt(4, sid);

            ps.executeUpdate();

            System.out.println("Volunteer Assigned!");

            con.close();

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // ==========================================================
    // 7. MONITOR EMERGENCY REQUESTS
    // DBMS : SELECT
    // ==========================================================

    static void monitorRequests() {

    try {

        Connection con = DBConnection.getConnection();

        String sql = "SELECT victim_id, name, location, emergency_type, severity FROM VICTIM WHERE status='SOS Pending'";

        Statement st = con.createStatement();
        ResultSet rs = st.executeQuery(sql);

        System.out.println("\n===== PENDING SOS REQUESTS =====");
        System.out.println("ID\tName\tLocation\tEmergency\tSeverity");

        boolean found = false;

        while (rs.next()) {
            found = true;

            System.out.println(
                rs.getInt("victim_id") + "\t" +
                rs.getString("name") + "\t" +
                rs.getString("location") + "\t\t" +
                rs.getString("emergency_type") + "\t\t" +
                rs.getInt("severity"));
        }

        if (!found) {
            System.out.println("No Pending SOS Requests!");
            con.close();
            return;
        }

        System.out.print("\nEnter Victim ID : ");
        int victimId = sc.nextInt();

        System.out.print("Assign Rescue Team ID : ");
        int teamId = sc.nextInt();

        // Approve victim & assign team
        String updateVictim =
            "UPDATE VICTIM SET status='Approved', team_id=? WHERE victim_id=?";

        PreparedStatement ps1 = con.prepareStatement(updateVictim);
        ps1.setInt(1, teamId);
        ps1.setInt(2, victimId);

        int rows = ps1.executeUpdate();

        if (rows > 0) {

            // Update rescue team status
            String updateTeam =
                "UPDATE RESCUE_TEAM SET rescue_status='Assigned' WHERE team_id=?";

            PreparedStatement ps2 = con.prepareStatement(updateTeam);
            ps2.setInt(1, teamId);
            ps2.executeUpdate();

            System.out.println("SOS Approved & Rescue Team Assigned!");

        } else {
            System.out.println("Invalid Victim ID!");
        }

        con.close();

    } catch (Exception e) {
        System.out.println(e);
    }
}
    // ==========================================================
    // 8. TRACK VOLUNTEER AVAILABILITY
    // DBMS : SELECT
    // ==========================================================

    static void volunteerAvailability() {

        try {

            Connection con = DBConnection.getConnection();

            Statement st = con.createStatement();

            ResultSet rs = st.executeQuery("SELECT * FROM VOLUNTEER");

            System.out.println("\n----- VOLUNTEERS -----");

            while (rs.next()) {

                System.out.println(
                        rs.getInt("volunteer_id") + " | " +
                        rs.getString("name") + " | " +
                        rs.getString("skill"));
            }

            con.close();

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // ==========================================================
    // 9. ASSIGN SHELTER
    // DBMS : UPDATE + Foreign Key
    // ==========================================================

    static void assignShelter() {

        try {

            Connection con = DBConnection.getConnection();

            System.out.print("Victim ID : ");
            int id = sc.nextInt();

            System.out.print("Shelter ID(Admin ID) : ");
            int aid = sc.nextInt();

            String sql = "UPDATE VICTIM SET admin_id=?,status='Shelter Assigned' WHERE victim_id=?";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1, aid);
            ps.setInt(2, id);

            ps.executeUpdate();

            System.out.println("Shelter Assigned!");

            con.close();

        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // ==========================================================
    // 10. GENERATE REPORT
    // DBMS : SQL JOIN
    // ==========================================================

    static void generateReport() {

        try {

            Connection con = DBConnection.getConnection();

            String sql =
            "SELECT V.victim_id,V.name,A.name AS admin_name,V.status " +
            "FROM VICTIM V LEFT JOIN ADMIN A ON V.admin_id=A.admin_id";

            Statement st = con.createStatement();

            ResultSet rs = st.executeQuery(sql);

            System.out.println("\n========== REPORT ==========");

            while (rs.next()) {

                System.out.println(
                        rs.getInt("victim_id") + " | " +
                        rs.getString("name") + " | " +
                        rs.getString("admin_name") + " | " +
                        rs.getString("status"));
            }

            con.close();

        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
