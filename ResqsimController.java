package com.resqsim.web;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class ResqsimController {

    private final JdbcTemplate db;

    public ResqsimController(JdbcTemplate db) {
        this.db = db;
    }

    // =====================================================
    // HEALTH
    // =====================================================

    @GetMapping("/health")
    public Map<String, Object> health() {

        try {

            db.queryForObject(
                    "SELECT 1",
                    Integer.class
            );

            return Map.of(
                    "ok", true,
                    "database", "connected"
            );

        } catch (Exception e) {

            return Map.of(
                    "ok", false,
                    "database", "error",
                    "message", e.getMessage()
            );
        }
    }


    // =====================================================
    // LOGIN
    // =====================================================

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody Map<String, Object> b) {

        String role = s(b, "role");
        String email = s(b, "email");
        String password = s(b, "password");
        String phone = s(b, "phone");

        // ADMIN LOGIN
        if ("admin".equalsIgnoreCase(role)) {

            List<Map<String, Object>> rows =
                    db.queryForList(
                            "SELECT admin_id,name,email,phone " +
                            "FROM ADMIN " +
                            "WHERE email=? AND password=?",
                            email,
                            password
                    );

            if (rows.isEmpty()) {

                return ResponseEntity
                        .status(401)
                        .body(
                                Map.of(
                                        "success", false,
                                        "message",
                                        "Invalid admin email or password"
                                )
                        );
            }

            var user = rows.get(0);

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "role", "admin",
                            "user", user
                    )
            );
        }


        // VICTIM LOGIN
        if ("victim".equalsIgnoreCase(role)) {

            List<Map<String, Object>> rows =
                    db.queryForList(
                            "SELECT victim_id,name,phone,location," +
                            "emergency_type,severity,status,team_id," +
                            "shelter_id,resource_request,resource_status " +
                            "FROM VICTIM " +
                            "WHERE victim_id=? AND phone=?",
                            i(b, "victimId"),
                            phone
                    );

            if (rows.isEmpty()) {

                return ResponseEntity
                        .status(401)
                        .body(
                                Map.of(
                                        "success", false,
                                        "message",
                                        "Invalid Victim ID or phone number"
                                )
                        );
            }

            return ResponseEntity.ok(
                    Map.of(
                            "success", true,
                            "role", "victim",
                            "user", rows.get(0)
                    )
            );
        }


        return ResponseEntity
                .badRequest()
                .body(
                        Map.of(
                                "success", false,
                                "message", "Unknown role"
                        )
                );
    }


    // =====================================================
    // DASHBOARD
    // =====================================================

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {

        return Map.of(

                "victims",
                count("VICTIM"),

                "pendingSos",
                count(
                        "VICTIM",
                        "status='SOS Pending'"
                ),

                "activeTeams",
                count(
                        "RESCUE_TEAM",
                        "rescue_status='Assigned'"
                ),

                "availableTeams",
                count(
                        "RESCUE_TEAM",
                        "rescue_status='Available'"
                ),

                "availableBeds",
                number(
                        "SELECT COALESCE(" +
                        "SUM(available_beds),0) " +
                        "FROM SHELTER_MANAGER"
                ),

                "volunteers",
                count("VOLUNTEER")
        );
    }


    // =====================================================
    // VICTIMS
    // =====================================================

    @GetMapping("/victims")
    public List<Map<String, Object>> victims() {

        return db.queryForList(

                "SELECT v.*, " +
                "t.team_name " +
                "FROM VICTIM v " +
                "LEFT JOIN RESCUE_TEAM t " +
                "ON v.team_id=t.team_id " +
                "ORDER BY v.victim_id DESC"
        );
    }


    @PostMapping("/victims")
    public Map<String, Object> registerVictim(
            @RequestBody Map<String, Object> b) {

        db.update(
                "INSERT INTO VICTIM(name,phone,status) " +
                "VALUES(?,?,?)",

                s(b, "name"),
                s(b, "phone"),
                "Registered"
        );

        return Map.of(
                "message",
                "Victim registered successfully",

                "victimId",
                number(
                        "SELECT LAST_INSERT_ID()"
                )
        );
    }


    // =====================================================
    // CREATE SOS
    // =====================================================

    @PutMapping("/victims/{id}/sos")
    public Map<String, Object> sos(
            @PathVariable int id,
            @RequestBody Map<String, Object> b) {

        int n = db.update(

                "UPDATE VICTIM SET " +
                "location=?, " +
                "emergency_type=?, " +
                "severity=?, " +
                "status='SOS Pending' " +
                "WHERE victim_id=?",

                s(b, "location"),
                s(b, "emergencyType"),
                i(b, "severity"),
                id
        );

        return result(
                n,
                "SOS created successfully"
        );
    }


    // =====================================================
    // SINGLE VICTIM
    // =====================================================

    @GetMapping("/victims/{id}")
    public ResponseEntity<?> victim(
            @PathVariable int id) {

        List<Map<String, Object>> rows =
                db.queryForList(
                        "SELECT * FROM VICTIM " +
                        "WHERE victim_id=?",
                        id
                );

        return rows.isEmpty()
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(rows.get(0));
    }


    // =====================================================
    // RESOURCE REQUEST
    // =====================================================

    @PutMapping("/victims/{id}/resource")
    public Map<String, Object> resource(
            @PathVariable int id,
            @RequestBody Map<String, Object> b) {

        String resource =
                s(b, "resource");

        int quantity =
                i(b, "quantity");


        // Check victim exists
        List<Map<String, Object>> victim =
                db.queryForList(
                        "SELECT victim_id " +
                        "FROM VICTIM " +
                        "WHERE victim_id=?",
                        id
                );

        if (victim.isEmpty()) {

            throw new RuntimeException(
                    "Victim not found"
            );
        }


        // Check inventory
        List<Map<String, Object>> inventory =
                db.queryForList(

                        "SELECT resource_id," +
                        "resource_name," +
                        "remaining_quantity " +

                        "FROM RESOURCE_INVENTORY " +

                        "WHERE resource_name=?",

                        resource
                );


        if (inventory.isEmpty()) {

            throw new RuntimeException(
                    "Resource not found in inventory"
            );
        }


        int remaining =
                ((Number)
                        inventory.get(0)
                                .get("remaining_quantity"))
                        .intValue();


        if (quantity <= 0) {

            throw new RuntimeException(
                    "Quantity must be greater than zero"
            );
        }


        if (quantity > remaining) {

            throw new RuntimeException(
                    "Only " +
                    remaining +
                    " units are available"
            );
        }


        String request =
                resource +
                ":" +
                quantity;


        int n = db.update(

                "UPDATE VICTIM SET " +
                "resource_request=?, " +
                "resource_status='Resource Requested' " +
                "WHERE victim_id=?",

                request,
                id
        );


        return result(
                n,
                "Resource request sent"
        );
    }


    // =====================================================
    // FULL VICTIM DETAILS
    // =====================================================

    @GetMapping("/victim/{id}/full")
    public ResponseEntity<?> victimFull(
            @PathVariable int id) {

        List<Map<String, Object>> rows =
                db.queryForList(

                        "SELECT " +
                        "v.*, " +

                        "t.team_name, " +
                        "t.leader, " +
                        "t.vehicle, " +

                        "s.shelter_name, " +
                        "s.address, " +
                        "s.available_beds " +

                        "FROM VICTIM v " +

                        "LEFT JOIN RESCUE_TEAM t " +
                        "ON v.team_id=t.team_id " +

                        "LEFT JOIN SHELTER_MANAGER s " +
                        "ON v.shelter_id=s.shelter_id " +

                        "WHERE v.victim_id=?",

                        id
                );

        return rows.isEmpty()
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(rows.get(0));
    }


    // =====================================================
    // SHELTERS
    // =====================================================

    @GetMapping("/shelters")
    public List<Map<String, Object>> shelters() {

        return db.queryForList(

                "SELECT * " +
                "FROM SHELTER_MANAGER " +
                "WHERE available_beds>0 " +
                "ORDER BY available_beds DESC"
        );
    }


    // =====================================================
    // ADMIN SOS
    // =====================================================

    @GetMapping("/admin/sos")
    public List<Map<String, Object>> sosList() {

        return db.queryForList(

                "SELECT " +
                "victim_id, " +
                "name, " +
                "phone, " +
                "location, " +
                "emergency_type, " +
                "severity, " +
                "status, " +
                "team_id " +

                "FROM VICTIM " +

                "WHERE status='SOS Pending' " +

                "ORDER BY severity DESC, victim_id ASC"
        );
    }


    // =====================================================
    // ADMIN RESOURCE REQUESTS
    // =====================================================

    @GetMapping("/admin/resources")
    public List<Map<String, Object>> resourceRequests() {

        return db.queryForList(

                "SELECT " +
                "victim_id, " +
                "name, " +
                "resource_request, " +
                "resource_status " +

                "FROM VICTIM " +

                "WHERE resource_status=" +
                "'Resource Requested'"
        );
    }


    // =====================================================
    // RESOURCE INVENTORY
    // =====================================================

    @GetMapping("/resources/inventory")
    public List<Map<String, Object>> inventory() {

        return db.queryForList(

                "SELECT " +
                "resource_id, " +
                "resource_name, " +
                "total_quantity, " +
                "allocated_quantity, " +
                "remaining_quantity " +

                "FROM RESOURCE_INVENTORY " +

                "ORDER BY resource_name"
        );
    }


    // =====================================================
    // AVAILABLE INVENTORY FOR DROPDOWNS
    // =====================================================

    @GetMapping("/resources/inventory/available")
    public List<Map<String, Object>>
    availableResourceInventory() {

        return db.queryForList(

                "SELECT " +
                "resource_id, " +
                "resource_name, " +
                "remaining_quantity " +

                "FROM RESOURCE_INVENTORY " +

                "WHERE remaining_quantity > 0 " +

                "ORDER BY resource_name"
        );
    }


    // =====================================================
    // ADD NEW INVENTORY RESOURCE
    // =====================================================

    @PostMapping("/admin/resources/inventory")
    public Map<String, Object> addInventoryResource(
            @RequestBody Map<String, Object> b) {

        String name =
                s(b, "resourceName");

        int quantity =
                i(b, "quantity");


        if (name.isBlank()) {

            throw new RuntimeException(
                    "Resource name is required"
            );
        }


        if (quantity <= 0) {

            throw new RuntimeException(
                    "Quantity must be greater than zero"
            );
        }


        int n = db.update(

                "INSERT INTO RESOURCE_INVENTORY " +
                "(resource_name,total_quantity," +
                "allocated_quantity,remaining_quantity) " +

                "VALUES(?,?,0,?)",

                name,
                quantity,
                quantity
        );


        return result(
                n,
                "Resource added successfully"
        );
    }


    // =====================================================
    // ADD STOCK
    // =====================================================

    @PutMapping(
            "/admin/resources/inventory/{id}/add-stock"
    )
    public Map<String, Object> addStock(
            @PathVariable int id,
            @RequestBody Map<String, Object> b) {

        int quantity =
                i(b, "quantity");


        if (quantity <= 0) {

            throw new RuntimeException(
                    "Quantity must be greater than zero"
            );
        }


        int n = db.update(

                "UPDATE RESOURCE_INVENTORY " +

                "SET total_quantity = " +
                "total_quantity + ?, " +

                "remaining_quantity = " +
                "remaining_quantity + ? " +

                "WHERE resource_id=?",

                quantity,
                quantity,
                id
        );


        return result(
                n,
                "Stock added successfully"
        );
    }


    // =====================================================
    // ALLOCATE RESOURCE
    // =====================================================

    @PutMapping("/admin/resources/{id}")
    public Map<String, Object> allocate(
            @PathVariable int id) {

        try (
                var con =
                        db.getDataSource()
                                .getConnection()
        ) {

            con.setAutoCommit(false);

            try {

                // -----------------------------------------
                // Get victim/resource request
                // -----------------------------------------

                String resourceRequest;

                try (
                        var ps =
                                con.prepareStatement(

                                        "SELECT " +
                                        "resource_request " +

                                        "FROM VICTIM " +

                                        "WHERE victim_id=? " +
                                        "AND resource_status=" +
                                        "'Resource Requested' " +

                                        "FOR UPDATE"
                                )
                ) {

                    ps.setInt(1, id);

                    try (var rs =
                                 ps.executeQuery()) {

                        if (!rs.next()) {

                            throw new RuntimeException(
                                    "Resource request not found"
                            );
                        }

                        resourceRequest =
                                rs.getString(
                                        "resource_request"
                                );
                    }
                }


                // -----------------------------------------
                // Parse resource + quantity
                // -----------------------------------------

                String resourceName =
                        resourceRequest.trim();

                int quantity = 1;


                if (resourceRequest.contains(":")) {

                    String[] parts =
                            resourceRequest
                                    .split(":", 2);

                    resourceName =
                            parts[0].trim();

                    quantity =
                            Integer.parseInt(
                                    parts[1].trim()
                            );
                }


                if (quantity <= 0) {

                    throw new RuntimeException(
                            "Invalid resource quantity"
                    );
                }


                // -----------------------------------------
                // Lock inventory row
                // -----------------------------------------

                int resourceId;
                int remaining;


                try (
                        var ps =
                                con.prepareStatement(

                                        "SELECT " +
                                        "resource_id, " +
                                        "remaining_quantity " +

                                        "FROM RESOURCE_INVENTORY " +

                                        "WHERE resource_name=? " +

                                        "FOR UPDATE"
                                )
                ) {

                    ps.setString(
                            1,
                            resourceName
                    );

                    try (var rs =
                                 ps.executeQuery()) {

                        if (!rs.next()) {

                            throw new RuntimeException(
                                    "Resource not found in inventory"
                            );
                        }

                        resourceId =
                                rs.getInt(
                                        "resource_id"
                                );

                        remaining =
                                rs.getInt(
                                        "remaining_quantity"
                                );
                    }
                }


                // -----------------------------------------
                // Check stock
                // -----------------------------------------

                if (quantity > remaining) {

                    throw new RuntimeException(

                            "Not enough stock. " +

                            "Only " +
                            remaining +
                            " units of " +
                            resourceName +
                            " are available."
                    );
                }


                // -----------------------------------------
                // Deduct inventory
                // -----------------------------------------

                try (
                        var ps =
                                con.prepareStatement(

                                        "UPDATE RESOURCE_INVENTORY " +

                                        "SET allocated_quantity = " +
                                        "allocated_quantity + ?, " +

                                        "remaining_quantity = " +
                                        "remaining_quantity - ? " +

                                        "WHERE resource_id=? " +

                                        "AND remaining_quantity>=?"
                                )
                ) {

                    ps.setInt(
                            1,
                            quantity
                    );

                    ps.setInt(
                            2,
                            quantity
                    );

                    ps.setInt(
                            3,
                            resourceId
                    );

                    ps.setInt(
                            4,
                            quantity
                    );


                    if (ps.executeUpdate() == 0) {

                        throw new RuntimeException(
                                "Resource allocation failed"
                        );
                    }
                }


                // -----------------------------------------
                // Update victim
                // -----------------------------------------

                try (
                        var ps =
                                con.prepareStatement(

                                        "UPDATE VICTIM " +

                                        "SET resource_status=" +
                                        "'Resources Allocated' " +

                                        "WHERE victim_id=?"
                                )
                ) {

                    ps.setInt(1, id);

                    if (ps.executeUpdate() == 0) {

                        throw new RuntimeException(
                                "Victim not found"
                        );
                    }
                }


                con.commit();


                return Map.of(

                        "success",
                        true,

                        "message",
                        "Resources allocated successfully",

                        "resource",
                        resourceName,

                        "quantity",
                        quantity
                );

            } catch (Exception e) {

                con.rollback();

                throw e;
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    e.getMessage()
            );
        }
    }


    // =====================================================
    // RESCUE TEAMS
    // IMPORTANT:
    // team_id + victim_id are both returned
    // =====================================================

    @GetMapping("/teams")
    public List<Map<String, Object>> teams() {

        return db.queryForList(

                "SELECT " +

                "rt.team_id, " +
                "rt.team_name, " +
                "rt.leader, " +
                "rt.vehicle, " +
                "rt.rescue_status, " +
                "rt.victim_id, " +

                "v.name AS victim_name " +

                "FROM RESCUE_TEAM rt " +

                "LEFT JOIN VICTIM v " +
                "ON rt.victim_id=v.victim_id " +

                "ORDER BY rt.team_id"
        );
    }


    // =====================================================
    // AVAILABLE RESCUE TEAMS
    // =====================================================

    @GetMapping("/teams/available")
    public List<Map<String, Object>>
    availableTeams() {

        return db.queryForList(

                "SELECT " +
                "team_id, " +
                "team_name, " +
                "leader, " +
                "vehicle, " +
                "rescue_status, " +
                "victim_id " +

                "FROM RESCUE_TEAM " +

                "WHERE rescue_status='Available' " +

                "ORDER BY team_id"
        );
    }


    // =====================================================
    // REGISTER RESCUE TEAM
    // =====================================================

    @PostMapping("/admin/teams")
    public Map<String, Object> registerTeam(
            @RequestBody Map<String, Object> b) {

        int teamId =
                i(b, "teamId");

        String teamName =
                s(b, "teamName");

        String leader =
                s(b, "leader");

        String vehicle =
                s(b, "vehicle");


        int n = db.update(

                "INSERT INTO RESCUE_TEAM " +
                "(team_id,team_name,leader,vehicle," +
                "rescue_status,victim_id) " +

                "VALUES(?,?,?,?,?,NULL)",

                teamId,
                teamName,
                leader,
                vehicle,
                "Available"
        );


        return result(
                n,
                "Rescue team registered successfully"
        );
    }


    // =====================================================
    // ASSIGN RESCUE TEAM
    //
    // VICTIM.team_id = assigned team
    // RESCUE_TEAM.victim_id = assigned victim
    // =====================================================

    @PutMapping("/admin/assign-team")
    public Map<String, Object> assignTeam(
            @RequestBody Map<String, Object> b) {

        int victimId =
                i(b, "victimId");

        int teamId =
                i(b, "teamId");


        try (
                var con =
                        db.getDataSource()
                                .getConnection()
        ) {

            con.setAutoCommit(false);

            try {

                // -----------------------------------------
                // Check victim
                // -----------------------------------------

                try (
                        var ps =
                                con.prepareStatement(

                                        "SELECT victim_id " +
                                        "FROM VICTIM " +
                                        "WHERE victim_id=? " +
                                        "FOR UPDATE"
                                )
                ) {

                    ps.setInt(
                            1,
                            victimId
                    );

                    try (var rs =
                                 ps.executeQuery()) {

                        if (!rs.next()) {

                            throw new RuntimeException(
                                    "Victim not found"
                            );
                        }
                    }
                }


                // -----------------------------------------
                // Check rescue team
                // -----------------------------------------

                try (
                        var ps =
                                con.prepareStatement(

                                        "SELECT team_id " +
                                        "FROM RESCUE_TEAM " +
                                        "WHERE team_id=? " +
                                        "FOR UPDATE"
                                )
                ) {

                    ps.setInt(
                            1,
                            teamId
                    );

                    try (var rs =
                                 ps.executeQuery()) {

                        if (!rs.next()) {

                            throw new RuntimeException(
                                    "Rescue team not found"
                            );
                        }
                    }
                }


                // -----------------------------------------
                // Remove this team from any old victim
                // -----------------------------------------

                try (
                        var ps =
                                con.prepareStatement(

                                        "UPDATE VICTIM " +

                                        "SET team_id=NULL " +

                                        "WHERE team_id=? " +
                                        "AND victim_id<>?"
                                )
                ) {

                    ps.setInt(
                            1,
                            teamId
                    );

                    ps.setInt(
                            2,
                            victimId
                    );

                    ps.executeUpdate();
                }


                // -----------------------------------------
                // Assign team to victim
                // -----------------------------------------

                try (
                        var ps =
                                con.prepareStatement(

                                        "UPDATE VICTIM " +

                                        "SET team_id=?, " +
                                        "status='Approved' " +

                                        "WHERE victim_id=?"
                                )
                ) {

                    ps.setInt(
                            1,
                            teamId
                    );

                    ps.setInt(
                            2,
                            victimId
                    );

                    if (ps.executeUpdate() == 0) {

                        throw new RuntimeException(
                                "Victim not found"
                        );
                    }
                }


                // -----------------------------------------
                // Assign victim to rescue team
                // -----------------------------------------

                try (
                        var ps =
                                con.prepareStatement(

                                        "UPDATE RESCUE_TEAM " +

                                        "SET victim_id=?, " +
                                        "rescue_status='Assigned' " +

                                        "WHERE team_id=?"
                                )
                ) {

                    ps.setInt(
                            1,
                            victimId
                    );

                    ps.setInt(
                            2,
                            teamId
                    );

                    if (ps.executeUpdate() == 0) {

                        throw new RuntimeException(
                                "Team not found"
                        );
                    }
                }


                con.commit();


                return Map.of(

                        "success",
                        true,

                        "message",
                        "Rescue team assigned successfully",

                        "victimId",
                        victimId,

                        "teamId",
                        teamId
                );

            } catch (Exception e) {

                con.rollback();

                throw e;
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    e.getMessage()
            );
        }
    }


    // =====================================================
    // VOLUNTEERS
    // =====================================================

    @GetMapping("/volunteers")
    public List<Map<String, Object>> volunteers() {

        return db.queryForList(

                "SELECT " +
                "v.*, " +
                "s.shelter_name " +

                "FROM VOLUNTEER v " +

                "LEFT JOIN SHELTER_MANAGER s " +
                "ON v.shelter_id=s.shelter_id " +

                "ORDER BY v.volunteer_id DESC"
        );
    }


    @PostMapping("/volunteers")
    public Map<String, Object> addVolunteer(
            @RequestBody Map<String, Object> b) {

        Integer shelter =
                iNullable(
                        b,
                        "shelterId"
                );

        db.update(

                "INSERT INTO VOLUNTEER " +
                "(name,phone,skill,shelter_id,availability) " +

                "VALUES(?,?,?,?,?)",

                s(b, "name"),
                s(b, "phone"),
                s(b, "skill"),
                shelter,
                "Available"
        );

        return Map.of(
                "message",
                "Volunteer added"
        );
    }


    @PutMapping("/volunteers/{id}/availability")
    public Map<String, Object> volunteerAvailability(
            @PathVariable int id,
            @RequestBody Map<String, Object> b) {

        return result(

                db.update(

                        "UPDATE VOLUNTEER " +
                        "SET availability=? " +
                        "WHERE volunteer_id=?",

                        s(b, "availability"),
                        id
                ),

                "Availability updated"
        );
    }


    // =====================================================
    // ADMIN
    // =====================================================

    @PostMapping("/admin")
    public Map<String, Object> admin(
            @RequestBody Map<String, Object> b) {

        db.update(

                "INSERT INTO ADMIN " +
                "(name,email,password,phone) " +
                "VALUES(?,?,?,?)",

                s(b, "name"),
                s(b, "email"),
                s(b, "password"),
                s(b, "phone")
        );

        return Map.of(
                "message",
                "Admin registered"
        );
    }


    @GetMapping("/admins")
    public List<Map<String, Object>> admins() {

        return db.queryForList(

                "SELECT " +
                "admin_id,name,email,phone " +

                "FROM ADMIN " +

                "ORDER BY admin_id DESC"
        );
    }


    // =====================================================
    // DISASTER EVENT
    // =====================================================

    @PutMapping("/admin/disaster")
    public Map<String, Object> disaster(
            @RequestBody Map<String, Object> b) {

        return result(

                db.update(

                        "UPDATE VICTIM SET " +
                        "emergency_type=?, " +
                        "location=?, " +
                        "status='Emergency Active' " +
                        "WHERE victim_id=?",

                        s(b, "emergencyType"),
                        s(b, "location"),
                        i(b, "victimId")
                ),

                "Disaster event updated"
        );
    }


    // =====================================================
    // ASSIGN SHELTER
    // =====================================================

    @PutMapping("/admin/shelter")
    public Map<String, Object> shelter(
            @RequestBody Map<String, Object> b) {

        int victimId =
                i(b, "victimId");

        int shelterId =
                i(b, "shelterId");


        int n = db.update(

                "UPDATE VICTIM SET " +
                "shelter_id=?, " +
                "status='Shelter Assigned' " +
                "WHERE victim_id=?",

                shelterId,
                victimId
        );


        if (n > 0) {

            db.update(

                    "UPDATE SHELTER_MANAGER " +

                    "SET available_beds=" +
                    "GREATEST(available_beds-1,0) " +

                    "WHERE shelter_id=? " +
                    "AND available_beds>0",

                    shelterId
            );
        }


        return result(
                n,
                "Shelter assigned"
        );
    }


    // =====================================================
    // REPORT
    // =====================================================

    @GetMapping("/report")
    public List<Map<String, Object>> report() {

        return db.queryForList(

                "SELECT " +

                "v.victim_id, " +
                "v.name, " +
                "v.location, " +
                "v.emergency_type, " +
                "v.severity, " +
                "v.status, " +
                "v.resource_status, " +

                "v.team_id, " +

                "t.team_name, " +
                "s.shelter_name " +

                "FROM VICTIM v " +

                "LEFT JOIN RESCUE_TEAM t " +
                "ON v.team_id=t.team_id " +

                "LEFT JOIN SHELTER_MANAGER s " +
                "ON v.shelter_id=s.shelter_id " +

                "ORDER BY v.victim_id DESC"
        );
    }


    // =====================================================
    // FCFS
    // =====================================================

    @PostMapping("/scheduling/fcfs")
    public Map<String, Object> fcfs(
            @RequestBody List<Map<String, Object>> jobs) {

        jobs.sort(
                Comparator.comparingInt(
                        x -> i(x, "arrival")
                )
        );


        int time = 0;

        double totalWaiting = 0;
        double totalTurnaround = 0;

        List<Map<String, Object>> out =
                new ArrayList<>();


        for (var j : jobs) {

            int arrival =
                    i(j, "arrival");

            int burst =
                    i(j, "burst");


            time =
                    Math.max(
                            time,
                            arrival
                    ) + burst;


            int completion =
                    time;

            int turnaround =
                    completion - arrival;

            int waiting =
                    turnaround - burst;


            totalWaiting += waiting;
            totalTurnaround += turnaround;


            out.add(
                    Map.of(

                            "id",
                            s(j, "id"),

                            "arrival",
                            arrival,

                            "burst",
                            burst,

                            "completion",
                            completion,

                            "waiting",
                            waiting,

                            "turnaround",
                            turnaround
                    )
            );
        }


        return Map.of(

                "jobs",
                out,

                "avgWaiting",
                jobs.isEmpty()
                        ? 0
                        : totalWaiting / jobs.size(),

                "avgTurnaround",
                jobs.isEmpty()
                        ? 0
                        : totalTurnaround / jobs.size()
        );
    }


    // =====================================================
    // PRIORITY
    // =====================================================

    @PostMapping("/scheduling/priority")
    public Map<String, Object> priority(
            @RequestBody List<Map<String, Object>> jobs) {

        jobs.sort(
                (a, b) ->
                        Integer.compare(
                                i(b, "priority"),
                                i(a, "priority")
                        )
        );


        int time = 0;

        double totalWaiting = 0;
        double totalTurnaround = 0;

        List<Map<String, Object>> out =
                new ArrayList<>();


        for (var j : jobs) {

            int burst =
                    i(j, "burst");

            time += burst;


            int completion =
                    time;

            int turnaround =
                    completion;

            int waiting =
                    turnaround - burst;


            totalWaiting += waiting;
            totalTurnaround += turnaround;


            out.add(
                    Map.of(

                            "id",
                            s(j, "id"),

                            "burst",
                            burst,

                            "priority",
                            i(j, "priority"),

                            "completion",
                            completion,

                            "waiting",
                            waiting,

                            "turnaround",
                            turnaround
                    )
            );
        }


        return Map.of(

                "jobs",
                out,

                "avgWaiting",
                jobs.isEmpty()
                        ? 0
                        : totalWaiting / jobs.size(),

                "avgTurnaround",
                jobs.isEmpty()
                        ? 0
                        : totalTurnaround / jobs.size()
        );
    }


    // =====================================================
    // HELPER METHODS
    // =====================================================

    private int count(String table) {

        return number(
                "SELECT COUNT(*) FROM " +
                table
        );
    }


    private int count(
            String table,
            String where) {

        return number(

                "SELECT COUNT(*) FROM " +
                table +
                " WHERE " +
                where
        );
    }


    private int number(String sql) {

        Integer x =
                db.queryForObject(
                        sql,
                        Integer.class
                );

        return x == null
                ? 0
                : x;
    }


    private static String s(
            Map<String, Object> b,
            String key) {

        return Objects.toString(
                b.get(key),
                ""
        );
    }


    private static int i(
            Map<String, Object> b,
            String key) {

        Object value =
                b.get(key);


        if (value instanceof Number) {

            return ((Number) value)
                    .intValue();
        }


        return Integer.parseInt(
                Objects.toString(
                        value,
                        "0"
                )
        );
    }


    private static Integer iNullable(
            Map<String, Object> b,
            String key) {

        Object value =
                b.get(key);


        if (
                value == null ||
                Objects.toString(
                        value,
                        ""
                ).isBlank() ||
                "0".equals(
                        Objects.toString(
                                value
                        )
                )
        ) {

            return null;
        }


        return i(b, key);
    }


    private static Map<String, Object> result(
            int n,
            String message) {

        return n > 0

                ? Map.of(
                        "success",
                        true,
                        "message",
                        message
                )

                : Map.of(
                        "success",
                        false,
                        "message",
                        "Record not found"
                );
    }
}
