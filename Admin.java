import java.sql.*;
import java.util.Scanner;

public class Admin {

    static Scanner sc = new Scanner(System.in);

    // ================= ADMIN MENU =================
    public static void adminMenu() {

        while (true) {

            System.out.println("\n===== ADMIN MODULE =====");
            System.out.println("1. Register Admin");
            System.out.println("2. User Management");
            System.out.println("3. Create / Update Disaster Event");
            System.out.println("4. Allocate Resources");
            System.out.println("5. Rescue Team Management");
            System.out.println("6. Assign Volunteers");
            System.out.println("7. Monitor Emergency Requests");
            System.out.println("8. Track Volunteer Availability");
            System.out.println("9. Assign Shelter");
            System.out.println("10. Generate Report");
            System.out.println("11. Back");

            System.out.print("Enter Choice: ");
            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {

                case 1:
                    addAdmin();
                    break;

                case 2:
                    userManagement();
                    break;

                case 3:
                    disasterEvent();
                    break;

                case 4:
                    allocateResources();
                    break;

                case 5:
                    rescueTeamManagement();
                    break;

                case 6:
                    assignVolunteer();
                    break;

                case 7:
                    monitorRequests();
                    break;

                case 8:
                    volunteerAvailability();
                    break;

                case 9:
                    assignShelter();
                    break;

                case 10:
                    generateReport();
                    break;

                case 11:
                    return;

                default:
                    System.out.println("Invalid Choice!");
            }
        }
    }


    // ================= 1. REGISTER ADMIN =================

    public static void addAdmin() {

        System.out.println("\n===== REGISTER ADMIN =====");

        System.out.print("Enter Admin ID: ");
        int adminId = sc.nextInt();
        sc.nextLine();

        System.out.print("Enter Admin Name: ");
        String name = sc.nextLine();

        System.out.print("Enter Phone: ");
        String phone = sc.nextLine();

        try {

            Connection con = DBConnection.getConnection();

            String sql =
                    "INSERT INTO ADMIN(admin_id, name, phone) VALUES (?, ?, ?)";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1, adminId);
            ps.setString(2, name);
            ps.setString(3, phone);

            ps.executeUpdate();

            System.out.println("Admin Registered Successfully!");

            ps.close();
            con.close();

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }


    // ================= 2. USER MANAGEMENT =================

    public static void userManagement() {

        System.out.println("\n===== USER MANAGEMENT =====");

        try {

            Connection con = DBConnection.getConnection();

            String sql =
                    "SELECT victim_id, name, phone, location, status FROM VICTIM";

            PreparedStatement ps = con.prepareStatement(sql);

            ResultSet rs = ps.executeQuery();

            System.out.printf("%-8s %-15s %-15s %-15s %-20s%n",
                    "ID", "Name", "Phone", "Location", "Status");

            System.out.println(
                    "--------------------------------------------------------------------------");

            while (rs.next()) {

                System.out.printf("%-8d %-15s %-15s %-15s %-20s%n",
                        rs.getInt("victim_id"),
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getString("location"),
                        rs.getString("status"));
            }

            rs.close();
            ps.close();
            con.close();

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }


    // ================= 3. DISASTER EVENT =================

    public static void disasterEvent() {

        System.out.println("\n===== CREATE / UPDATE DISASTER EVENT =====");

        System.out.print("Enter Victim ID: ");
        int victimId = sc.nextInt();
        sc.nextLine();

        System.out.print("Enter Disaster Type: ");
        String disaster = sc.nextLine();

        System.out.print("Enter Affected Location: ");
        String location = sc.nextLine();

        Connection con = null;

        try {

            con = DBConnection.getConnection();

            // Start transaction
            con.setAutoCommit(false);

            String sql =
                    "UPDATE VICTIM SET emergency_type=?, location=?, status=? " +
                    "WHERE victim_id=?";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, disaster);
            ps.setString(2, location);
            ps.setString(3, "Emergency Active");
            ps.setInt(4, victimId);

            int rows = ps.executeUpdate();

            if (rows > 0) {

                con.commit();

                System.out.println("Disaster Event Updated Successfully!");

            } else {

                con.rollback();

                System.out.println("Victim ID Not Found!");
            }

            ps.close();
            con.close();

        } catch (SQLException e) {

            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (SQLException ignored) {
            }

            System.out.println("Transaction Failed: " + e.getMessage());
        }
    }


    // ================= 4. ALLOCATE RESOURCES =================

    public static void allocateResources() {

        System.out.println("\n===== RESOURCE REQUESTS =====");

        try {

            Connection con = DBConnection.getConnection();

            String sql =
                    "SELECT victim_id, name, resource_request " +
                    "FROM VICTIM " +
                    "WHERE resource_status='Resource Requested'";

            PreparedStatement ps = con.prepareStatement(sql);

            ResultSet rs = ps.executeQuery();

            boolean found = false;

            System.out.printf("%-8s %-15s %-25s%n",
                    "ID", "Name", "Resource Request");

            System.out.println(
                    "----------------------------------------------------");

            while (rs.next()) {

                found = true;

                System.out.printf("%-8d %-15s %-25s%n",
                        rs.getInt("victim_id"),
                        rs.getString("name"),
                        rs.getString("resource_request"));
            }

            if (!found) {
                System.out.println("No Resource Requests Found.");
                rs.close();
                ps.close();
                con.close();
                return;
            }

            rs.close();
            ps.close();

            System.out.print("\nEnter Victim ID to Allocate Resource: ");
            int victimId = sc.nextInt();
            sc.nextLine();

            String update =
                    "UPDATE VICTIM SET resource_status=? " +
                    "WHERE victim_id=? AND resource_status='Resource Requested'";

            PreparedStatement updatePs = con.prepareStatement(update);

            updatePs.setString(1, "Resources Allocated");
            updatePs.setInt(2, victimId);

            int rows = updatePs.executeUpdate();

            if (rows > 0) {
                System.out.println("Resources Allocated Successfully!");
            } else {
                System.out.println("Resource Request Not Found!");
            }

            updatePs.close();
            con.close();

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }


    // ================= 5. RESCUE TEAM MANAGEMENT =================

    public static void rescueTeamManagement() {

        while (true) {

            System.out.println("\n===== RESCUE TEAM MANAGEMENT =====");
            System.out.println("1. Register New Rescue Team");
            System.out.println("2. View Rescue Teams");
            System.out.println("3. Assign Rescue Team");
            System.out.println("4. Back");

            System.out.print("Enter Choice: ");
            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {

                case 1:
                    registerRescueTeam();
                    break;

                case 2:
                    viewRescueTeams();
                    break;

                case 3:
                    assignRescueTeam();
                    break;

                case 4:
                    return;

                default:
                    System.out.println("Invalid Choice!");
            }
        }
    }


    // ================= REGISTER RESCUE TEAM =================

    public static void registerRescueTeam() {

        System.out.println("\n===== REGISTER NEW RESCUE TEAM =====");

        System.out.print("Enter Team ID: ");
        int teamId = sc.nextInt();
        sc.nextLine();

        System.out.print("Enter Team Name: ");
        String teamName = sc.nextLine();

        System.out.print("Enter Team Leader: ");
        String leader = sc.nextLine();

        System.out.print("Enter Vehicle: ");
        String vehicle = sc.nextLine();

        try {

            Connection con = DBConnection.getConnection();

            String sql =
                    "INSERT INTO RESCUE_TEAM " +
                    "(team_id, team_name, leader, vehicle, rescue_status) " +
                    "VALUES (?, ?, ?, ?, ?)";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1, teamId);
            ps.setString(2, teamName);
            ps.setString(3, leader);
            ps.setString(4, vehicle);
            ps.setString(5, "Available");

            ps.executeUpdate();

            System.out.println("Rescue Team Registered Successfully!");

            ps.close();
            con.close();

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }


    // ================= VIEW RESCUE TEAMS =================

    public static void viewRescueTeams() {

        System.out.println("\n===== RESCUE TEAMS =====");

        try {

            Connection con = DBConnection.getConnection();

            String sql =
                    "SELECT team_id, team_name, leader, vehicle, rescue_status " +
                    "FROM RESCUE_TEAM";

            PreparedStatement ps = con.prepareStatement(sql);

            ResultSet rs = ps.executeQuery();

            System.out.printf("%-8s %-22s %-18s %-18s %-15s%n",
                    "ID", "Team Name", "Leader", "Vehicle", "Status");

            System.out.println(
                    "----------------------------------------------------------------------------");

            while (rs.next()) {

                System.out.printf("%-8d %-22s %-18s %-18s %-15s%n",
                        rs.getInt("team_id"),
                        rs.getString("team_name"),
                        rs.getString("leader"),
                        rs.getString("vehicle"),
                        rs.getString("rescue_status"));
            }

            rs.close();
            ps.close();
            con.close();

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }


    // ================= ASSIGN RESCUE TEAM =================

    public static void assignRescueTeam() {

        System.out.println("\n===== AVAILABLE RESCUE TEAMS =====");

        try {

            Connection con = DBConnection.getConnection();

            // Display available teams
            String teamSql =
                    "SELECT team_id, team_name, leader, vehicle " +
                    "FROM RESCUE_TEAM " +
                    "WHERE rescue_status='Available'";

            PreparedStatement teamPs =
                    con.prepareStatement(teamSql);

            ResultSet teamRs = teamPs.executeQuery();

            boolean teamFound = false;

            System.out.printf("%-8s %-22s %-18s %-18s%n",
                    "ID", "Team Name", "Leader", "Vehicle");

            System.out.println(
                    "------------------------------------------------------------------");

            while (teamRs.next()) {

                teamFound = true;

                System.out.printf("%-8d %-22s %-18s %-18s%n",
                        teamRs.getInt("team_id"),
                        teamRs.getString("team_name"),
                        teamRs.getString("leader"),
                        teamRs.getString("vehicle"));
            }

            teamRs.close();
            teamPs.close();

            if (!teamFound) {

                System.out.println("No Available Rescue Teams.");
                con.close();
                return;
            }


            // Display pending SOS
            System.out.println("\n===== PENDING SOS REQUESTS =====");

            String victimSql =
                    "SELECT victim_id, name, location, emergency_type, severity " +
                    "FROM VICTIM " +
                    "WHERE status='SOS Pending'";

            PreparedStatement victimPs =
                    con.prepareStatement(victimSql);

            ResultSet victimRs = victimPs.executeQuery();

            boolean victimFound = false;

            System.out.printf("%-8s %-15s %-15s %-18s %-10s%n",
                    "ID", "Name", "Location", "Emergency", "Severity");

            System.out.println(
                    "----------------------------------------------------------------");

            while (victimRs.next()) {

                victimFound = true;

                System.out.printf("%-8d %-15s %-15s %-18s %-10d%n",
                        victimRs.getInt("victim_id"),
                        victimRs.getString("name"),
                        victimRs.getString("location"),
                        victimRs.getString("emergency_type"),
                        victimRs.getInt("severity"));
            }

            victimRs.close();
            victimPs.close();

            if (!victimFound) {

                System.out.println("No Pending SOS Requests.");
                con.close();
                return;
            }


            // Input
            System.out.print("\nEnter Victim ID: ");
            int victimId = sc.nextInt();

            System.out.print("Enter Team ID: ");
            int teamId = sc.nextInt();


            // Start transaction
            con.setAutoCommit(false);

            try {

                // Assign team to victim
                String victimUpdate =
                        "UPDATE VICTIM SET team_id=?, status='Approved' " +
                        "WHERE victim_id=? AND status='SOS Pending'";

                PreparedStatement victimUpdatePs =
                        con.prepareStatement(victimUpdate);

                victimUpdatePs.setInt(1, teamId);
                victimUpdatePs.setInt(2, victimId);

                int victimRows =
                        victimUpdatePs.executeUpdate();


                // Mark team as assigned
                String teamUpdate =
                        "UPDATE RESCUE_TEAM SET rescue_status='Assigned' " +
                        "WHERE team_id=? AND rescue_status='Available'";

                PreparedStatement teamUpdatePs =
                        con.prepareStatement(teamUpdate);

                teamUpdatePs.setInt(1, teamId);

                int teamRows =
                        teamUpdatePs.executeUpdate();


                if (victimRows > 0 && teamRows > 0) {

                    con.commit();

                    System.out.println(
                            "\nSOS Approved Successfully!");
                    System.out.println(
                            "Rescue Team Assigned Successfully!");

                } else {

                    con.rollback();

                    System.out.println(
                            "\nAssignment Failed!");
                    System.out.println(
                            "Check Victim ID and Team Availability.");
                }


                victimUpdatePs.close();
                teamUpdatePs.close();

            } catch (SQLException e) {

                con.rollback();

                System.out.println(
                        "Assignment Failed: " + e.getMessage());
            }

            con.close();

        } catch (SQLException e) {

            System.out.println("Error: " + e.getMessage());
        }
    }


    // ================= 6. ASSIGN VOLUNTEER =================

    public static void assignVolunteer() {

        System.out.println("\n===== ASSIGN VOLUNTEER =====");

        System.out.print("Enter Volunteer ID: ");
        int volunteerId = sc.nextInt();
        sc.nextLine();

        System.out.print("Enter Volunteer Name: ");
        String name = sc.nextLine();

        System.out.print("Enter Phone: ");
        String phone = sc.nextLine();

        System.out.print("Enter Availability: ");
        String availability = sc.nextLine();

        try {

            Connection con = DBConnection.getConnection();

            String sql =
                    "INSERT INTO VOLUNTEER " +
                    "(volunteer_id, name, phone, availability) " +
                    "VALUES (?, ?, ?, ?)";

            PreparedStatement ps =
                    con.prepareStatement(sql);

            ps.setInt(1, volunteerId);
            ps.setString(2, name);
            ps.setString(3, phone);
            ps.setString(4, availability);

            ps.executeUpdate();

            System.out.println("Volunteer Added Successfully!");

            ps.close();
            con.close();

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }


    // ================= 7. MONITOR EMERGENCY REQUESTS =================

    public static void monitorRequests() {

        System.out.println("\n===== PENDING SOS REQUESTS =====");

        try {

            Connection con = DBConnection.getConnection();

            String sql =
                    "SELECT victim_id, name, phone, location, " +
                    "emergency_type, severity, status " +
                    "FROM VICTIM " +
                    "WHERE status='SOS Pending'";

            PreparedStatement ps =
                    con.prepareStatement(sql);

            ResultSet rs = ps.executeQuery();

            boolean found = false;

            System.out.printf(
                    "%-8s %-15s %-15s %-15s %-18s %-10s%n",
                    "ID", "Name", "Phone", "Location",
                    "Emergency", "Severity");

            System.out.println(
                    "----------------------------------------------------------------------------");

            while (rs.next()) {

                found = true;

                System.out.printf(
                        "%-8d %-15s %-15s %-15s %-18s %-10d%n",
                        rs.getInt("victim_id"),
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getString("location"),
                        rs.getString("emergency_type"),
                        rs.getInt("severity"));
            }

            if (!found) {
                System.out.println("No Pending SOS Requests.");
            }

            rs.close();
            ps.close();
            con.close();

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }


    // ================= 8. VOLUNTEER AVAILABILITY =================

    public static void volunteerAvailability() {

        System.out.println("\n===== VOLUNTEER AVAILABILITY =====");

        try {

            Connection con = DBConnection.getConnection();

            String sql =
                    "SELECT volunteer_id, name, phone, availability " +
                    "FROM VOLUNTEER";

            PreparedStatement ps =
                    con.prepareStatement(sql);

            ResultSet rs = ps.executeQuery();

            System.out.printf("%-10s %-20s %-15s %-15s%n",
                    "ID", "Name", "Phone", "Availability");

            System.out.println(
                    "----------------------------------------------------------");

            while (rs.next()) {

                System.out.printf("%-10d %-20s %-15s %-15s%n",
                        rs.getInt("volunteer_id"),
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getString("availability"));
            }

            rs.close();
            ps.close();
            con.close();

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }


    // ================= 9. ASSIGN SHELTER =================

    public static void assignShelter() {

        System.out.println("\n===== ASSIGN SHELTER =====");

        System.out.print("Enter Victim ID: ");
        int victimId = sc.nextInt();

        System.out.print("Enter Shelter/Admin ID: ");
        int shelterId = sc.nextInt();

        try {

            Connection con = DBConnection.getConnection();

            String sql =
                    "UPDATE VICTIM SET admin_id=?, status='Shelter Assigned' " +
                    "WHERE victim_id=?";

            PreparedStatement ps =
                    con.prepareStatement(sql);

            ps.setInt(1, shelterId);
            ps.setInt(2, victimId);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Shelter Assigned Successfully!");
            } else {
                System.out.println("Victim ID Not Found!");
            }

            ps.close();
            con.close();

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }


    // ================= 10. GENERATE REPORT =================

    public static void generateReport() {

        System.out.println("\n===== DISASTER RESPONSE REPORT =====");

        try {

            Connection con = DBConnection.getConnection();

            String sql =
                    "SELECT v.victim_id, v.name, v.location, " +
                    "v.emergency_type, v.severity, v.status, " +
                    "v.team_id, a.name AS admin_name " +
                    "FROM VICTIM v " +
                    "LEFT JOIN ADMIN a ON v.admin_id=a.admin_id";

            PreparedStatement ps =
                    con.prepareStatement(sql);

            ResultSet rs = ps.executeQuery();

            System.out.printf(
                    "%-6s %-15s %-15s %-15s %-8s %-18s %-8s %-15s%n",
                    "ID", "Name", "Location", "Emergency",
                    "Severity", "Status", "Team", "Admin");

            System.out.println(
                    "------------------------------------------------------------------------------------------------");

            while (rs.next()) {

                String team =
                        rs.getObject("team_id") == null
                                ? "None"
                                : String.valueOf(rs.getInt("team_id"));

                String admin =
                        rs.getString("admin_name") == null
                                ? "None"
                                : rs.getString("admin_name");

                System.out.printf(
                        "%-6d %-15s %-15s %-15s %-8d %-18s %-8s %-15s%n",
                        rs.getInt("victim_id"),
                        rs.getString("name"),
                        rs.getString("location"),
                        rs.getString("emergency_type"),
                        rs.getInt("severity"),
                        rs.getString("status"),
                        team,
                        admin);
            }

            rs.close();
            ps.close();
            con.close();

        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
