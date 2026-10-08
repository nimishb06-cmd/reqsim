package com.resqsim.web;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/* ============================================================
   RESQSIM CONTROLLER
   Disaster Response & Resource Management System
   ============================================================ */

@RestController
@RequestMapping("/api")
@CrossOrigin
public class ResqsimController {

    private final JdbcTemplate db;

    public ResqsimController(JdbcTemplate db) {
        this.db = db;
    }


    /* =========================================================
       HEALTH CHECK
       ========================================================= */

    @GetMapping("/health")
    public Map<String, Object> health() {

        try {

            db.queryForObject(
                    "SELECT 1",
                    Integer.class
            );

            return Map.of(
                    "success", true,
                    "database", "connected"
            );

        } catch (Exception e) {

            return Map.of(
                    "success", false,
                    "database", "disconnected",
                    "message", e.getMessage()
            );
        }
    }


    /* =========================================================
       LOGIN
       ========================================================= */

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody Map<String, Object> b) {

        String role = s(b, "role");

        /* -----------------------------------------------------
           ADMIN LOGIN
           ----------------------------------------------------- */

        if ("admin".equalsIgnoreCase(role)) {

            String email = s(b, "email");
            String password = s(b, "password");

            try {

                List<Map<String, Object>> rows =
                        db.queryForList(
                                "SELECT * FROM ADMIN " +
                                "WHERE email=? AND password=?",
                                email,
                                password
                        );

                if (rows.isEmpty()) {

                    return ResponseEntity
                            .status(401)
                            .body(Map.of(
                                    "success", false,
                                    "message",
                                    "Invalid admin credentials"
                            ));
                }

                Map<String, Object> admin = rows.get(0);

                return ResponseEntity.ok(
                        Map.of(
                                "success", true,
                                "role", "admin",
                                "user", admin
                        )
                );

            } catch (Exception e) {

                return ResponseEntity
                        .internalServerError()
                        .body(Map.of(
                                "success", false,
                                "message", e.getMessage()
                        ));
            }
        }


        /* -----------------------------------------------------
           VICTIM LOGIN
           ----------------------------------------------------- */

        if ("victim".equalsIgnoreCase(role)) {

            int victimId = i(b, "victimId");
            String phone = s(b, "phone");

            try {

                List<Map<String, Object>> rows =
                        db.queryForList(
                                "SELECT * FROM VICTIM " +
                                "WHERE victim_id=? AND phone=?",
                                victimId,
                                phone
                        );

                if (rows.isEmpty()) {

                    return ResponseEntity
                            .status(401)
                            .body(Map.of(
                                    "success", false,
                                    "message",
                                    "Invalid victim credentials"
                            ));
                }

                Map<String, Object> victim = rows.get(0);

                return ResponseEntity.ok(
                        Map.of(
                                "success", true,
                                "role", "victim",
                                "user", victim
                        )
                );

            } catch (Exception e) {

                return ResponseEntity
                        .internalServerError()
                        .body(Map.of(
                                "success", false,
                                "message", e.getMessage()
                        ));
            }
        }


        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "success", false,
                        "message", "Invalid role"
                ));
    }


    /* =========================================================
       DASHBOARD
       ========================================================= */

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {

        int victims =
                count(
                        "SELECT COUNT(*) FROM VICTIM"
                );

        int pendingSOS =
                count(
                        "SELECT COUNT(*) FROM VICTIM " +
                        "WHERE status='SOS Pending'"
                );

        int activeTeams =
                count(
                        "SELECT COUNT(*) FROM RESCUE_TEAM " +
                        "WHERE rescue_status='Assigned'"
                );

        int availableTeams =
                count(
                        "SELECT COUNT(*) FROM RESCUE_TEAM " +
                        "WHERE rescue_status='Available'"
                );

        int volunteers =
                count(
                        "SELECT COUNT(*) FROM VOLUNTEER"
                );

        int shelterBeds = 0;

        try {

            shelterBeds =
                    count(
                            "SELECT COALESCE(" +
                            "SUM(available_beds),0) " +
                            "FROM SHELTER_MANAGER"
                    );

        } catch (Exception ignored) {
        }

        return Map.of(
                "victims", victims,
                "pendingSOS", pendingSOS,
                "activeTeams", activeTeams,
                "availableTeams", availableTeams,
                "volunteers", volunteers,
                "shelterBeds", shelterBeds
        );
    }


    /* =========================================================
       GET ALL VICTIMS
       ========================================================= */

    @GetMapping("/victims")
    public List<Map<String, Object>> getVictims() {

        try {

            return db.queryForList(
                    "SELECT " +
                    "v.victim_id, " +
                    "v.name, " +
                    "v.phone, " +
                    "v.location, " +
                    "v.emergency_type, " +
                    "v.severity, " +
                    "v.status, " +
                    "v.admin_id, " +
                    "v.team_id, " +
                    "rt.team_name " +
                    "FROM VICTIM v " +
                    "LEFT JOIN RESCUE_TEAM rt " +
                    "ON v.team_id=rt.team_id " +
                    "ORDER BY v.victim_id DESC"
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    e.getMessage()
            );
        }
    }


    /* =========================================================
       REGISTER VICTIM
       ========================================================= */

    @PostMapping("/victims")
    public Map<String, Object> registerVictim(
            @RequestBody Map<String, Object> b) {

        try {

            String name = s(b, "name");
            String phone = s(b, "phone");
            String location = s(b, "location");
            String emergencyType = s(b, "emergencyType");

            int severity = i(b, "severity");

            int updated =
                    db.update(
                            "INSERT INTO VICTIM " +
                            "(name, phone, location, " +
                            "emergency_type, severity, status) " +
                            "VALUES (?, ?, ?, ?, ?, ?)",
                            name,
                            phone,
                            location,
                            emergencyType,
                            severity,
                            "Registered"
                    );

            return Map.of(
                    "success", true,
                    "updated", updated,
                    "message",
                    "Victim registered successfully"
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    e.getMessage()
            );
        }
    }


    /* =========================================================
       GET SINGLE VICTIM
       ========================================================= */

    @GetMapping("/victims/{id}")
    public ResponseEntity<?> getVictim(
            @PathVariable int id) {

        try {

            List<Map<String, Object>> rows =
                    db.queryForList(
                            "SELECT * FROM VICTIM " +
                            "WHERE victim_id=?",
                            id
                    );

            if (rows.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(rows.get(0));

        } catch (Exception e) {

            return ResponseEntity
                    .internalServerError()
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }


    /* =========================================================
       COMPLETE VICTIM INFORMATION
       ========================================================= */

    @GetMapping("/victim/{id}/full")
    public ResponseEntity<?> fullVictim(
            @PathVariable int id) {

        try {

            List<Map<String, Object>> rows =
                    db.queryForList(
                            "SELECT " +
                            "v.*, " +
                            "rt.team_name, " +
                            "rt.leader AS team_leader, " +
                            "rt.vehicle, " +
                            "rt.rescue_status, " +
                            "sm.shelter_name, " +
                            "sm.address AS shelter_address " +
                            "FROM VICTIM v " +
                            "LEFT JOIN RESCUE_TEAM rt " +
                            "ON v.team_id=rt.team_id " +
                            "LEFT JOIN SHELTER_MANAGER sm " +
                            "ON v.shelter_id=sm.shelter_id " +
                            "WHERE v.victim_id=?",
                            id
                    );

            if (rows.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(rows.get(0));

        } catch (Exception e) {

            return ResponseEntity
                    .internalServerError()
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }


    /* =========================================================
       CREATE SOS
       POST VERSION
       ========================================================= */

    @PostMapping("/victims/sos")
    public Map<String, Object> createSos(
            @RequestBody Map<String, Object> b) {

        int victimId = i(b, "victimId");
        String location = s(b, "location");
        String emergencyType = s(b, "emergencyType");
        int severity = i(b, "severity");

        int updated =
                db.update(
                        "UPDATE VICTIM SET " +
                        "location=?, " +
                        "emergency_type=?, " +
                        "severity=?, " +
                        "status='SOS Pending' " +
                        "WHERE victim_id=?",
                        location,
                        emergencyType,
                        severity,
                        victimId
                );

        return result(
                updated,
                "SOS request created successfully"
        );
    }


    /* =========================================================
       CREATE SOS
       PUT VERSION
       ========================================================= */

    @PutMapping("/victims/{id}/sos")
    public Map<String, Object> createSosPut(
            @PathVariable int id,
            @RequestBody Map<String, Object> b) {

        String location = s(b, "location");
        String emergencyType = s(b, "emergencyType");
        int severity = i(b, "severity");

        int updated =
                db.update(
                        "UPDATE VICTIM SET " +
                        "location=?, " +
                        "emergency_type=?, " +
                        "severity=?, " +
                        "status='SOS Pending' " +
                        "WHERE victim_id=?",
                        location,
                        emergencyType,
                        severity,
                        id
                );

        return result(
                updated,
                "SOS request created successfully"
        );
    }


    /* =========================================================
       REQUEST RESOURCE
       ========================================================= */

    @PutMapping("/victims/{id}/resource")
    public Map<String, Object> requestResource(
            @PathVariable int id,
            @RequestBody Map<String, Object> b) {

        String resource = s(b, "resource");

        int quantity = i(b, "quantity");

        if (quantity <= 0) {
            quantity = 1;
        }

        String requestText =
                resource + ": " + quantity;

        int updated =
                db.update(
                        "UPDATE VICTIM SET " +
                        "resource_request=?, " +
                        "resource_status='Pending' " +
                        "WHERE victim_id=?",
                        requestText,
                        id
                );

        return result(
                updated,
                "Resource request submitted"
        );
    }


    /* =========================================================
       ADMIN RESOURCES
       
       IMPORTANT:
       Only PENDING resource requests are returned.
       
       Allocated requests will NOT appear here.
       ========================================================= */

    @GetMapping("/admin/resources")
    public List<Map<String, Object>> adminResources() {

        try {

            return db.queryForList(
                    "SELECT " +
                    "victim_id, " +
                    "name, " +
                    "location, " +
                    "resource_request, " +
                    "resource_status " +
                    "FROM VICTIM " +
                    "WHERE resource_request IS NOT NULL " +
                    "AND TRIM(resource_request) <> '' " +
                    "AND resource_status='Pending' " +
                    "ORDER BY victim_id DESC"
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    e.getMessage()
            );
        }
    }


    /* =========================================================
       TRACK VICTIM REQUEST
       ========================================================= */

    @GetMapping("/victims/{id}/track")
    public ResponseEntity<?> trackVictim(
            @PathVariable int id) {

        try {

            List<Map<String, Object>> rows =
                    db.queryForList(
                            "SELECT " +
                            "victim_id, " +
                            "name, " +
                            "location, " +
                            "status, " +
                            "team_id, " +
                            "resource_request, " +
                            "resource_status " +
                            "FROM VICTIM " +
                            "WHERE victim_id=?",
                            id
                    );

            if (rows.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(rows.get(0));

        } catch (Exception e) {

            return ResponseEntity
                    .internalServerError()
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }


    /* =========================================================
       SHELTERS
       ========================================================= */

    @GetMapping("/shelters")
    public List<Map<String, Object>> shelters() {

        return db.queryForList(
                "SELECT * FROM SHELTER_MANAGER " +
                "ORDER BY shelter_id"
        );
    }


    /* =========================================================
       ADMIN SOS QUEUE
       ========================================================= */

    @GetMapping("/admin/sos")
    public List<Map<String, Object>> adminSos() {

        return db.queryForList(
                "SELECT " +
                "v.victim_id, " +
                "v.name, " +
                "v.phone, " +
                "v.location, " +
                "v.emergency_type, " +
                "v.severity, " +
                "v.status, " +
                "v.team_id, " +
                "rt.team_name " +
                "FROM VICTIM v " +
                "LEFT JOIN RESCUE_TEAM rt " +
                "ON v.team_id=rt.team_id " +
                "WHERE v.status='SOS Pending' " +
                "ORDER BY v.severity DESC, " +
                "v.victim_id ASC"
        );
    }


    /* =========================================================
       RESOURCE REQUESTS
       
       IMPORTANT:
       Only PENDING requests are returned.
       
       Allocated requests are excluded.
       ========================================================= */

    @GetMapping("/admin/resource-requests")
    public List<Map<String, Object>> resourceRequests() {

        return db.queryForList(
                "SELECT " +
                "victim_id, " +
                "name, " +
                "phone, " +
                "location, " +
                "emergency_type, " +
                "severity, " +
                "resource_request, " +
                "resource_status " +
                "FROM VICTIM " +
                "WHERE resource_request IS NOT NULL " +
                "AND TRIM(resource_request) <> '' " +
                "AND resource_status='Pending' " +
                "ORDER BY severity DESC, " +
                "victim_id ASC"
        );
    }


    /* =========================================================
       RESOURCE INVENTORY
       ========================================================= */

    @GetMapping("/admin/inventory")
    public List<Map<String, Object>> inventory() {

        return db.queryForList(
                "SELECT " +
                "resource_id, " +
                "resource_name, " +
                "total_quantity, " +
                "allocated_quantity, " +
                "(total_quantity - " +
                "COALESCE(allocated_quantity,0)) " +
                "AS remaining_quantity " +
                "FROM RESOURCE_INVENTORY " +
                "ORDER BY resource_id"
        );
    }


    /* =========================================================
       ADD INVENTORY
       ========================================================= */

    @PostMapping("/admin/inventory")
    public Map<String, Object> addInventory(
            @RequestBody Map<String, Object> b) {

        String resourceName = s(b, "resourceName");
        int quantity = i(b, "quantity");

        int updated =
                db.update(
                        "INSERT INTO RESOURCE_INVENTORY " +
                        "(resource_name, total_quantity, " +
                        "allocated_quantity) " +
                        "VALUES (?, ?, 0)",
                        resourceName,
                        quantity
                );

        return result(
                updated,
                "Resource added to inventory"
        );
    }


    /* =========================================================
       ADD STOCK
       ========================================================= */

    @PutMapping("/admin/inventory/{id}/stock")
    public Map<String, Object> addStock(
            @PathVariable int id,
            @RequestBody Map<String, Object> b) {

        int quantity = i(b, "quantity");

        int updated =
                db.update(
                        "UPDATE RESOURCE_INVENTORY " +
                        "SET total_quantity=" +
                        "total_quantity+? " +
                        "WHERE resource_id=?",
                        quantity,
                        id
                );

        return result(
                updated,
                "Stock added successfully"
        );
    }


    /* =========================================================
       ALLOCATE RESOURCE
       ========================================================= */

    @PutMapping("/admin/resources/{id}")
    public Map<String, Object> allocateResource(
            @PathVariable int id) {

        try {

            List<Map<String, Object>> rows =
                    db.queryForList(
                            "SELECT resource_request " +
                            "FROM VICTIM " +
                            "WHERE victim_id=?",
                            id
                    );

            if (rows.isEmpty()) {

                return Map.of(
                        "success", false,
                        "message", "Victim not found"
                );
            }

            String request =
                    s(
                            rows.get(0),
                            "resource_request"
                    );

            if (request == null ||
                request.isBlank()) {

                return Map.of(
                        "success", false,
                        "message",
                        "No resource request found"
                );
            }

            String resourceName = request;
            int quantity = 1;

            if (request.contains(":")) {

                String[] parts =
                        request.split(":");

                resourceName =
                        parts[0].trim();

                try {

                    quantity =
                            Integer.parseInt(
                                    parts[1].trim()
                            );

                } catch (Exception ignored) {
                }
            }

            List<Map<String, Object>> inv =
                    db.queryForList(
                            "SELECT " +
                            "resource_id, " +
                            "total_quantity, " +
                            "allocated_quantity " +
                            "FROM RESOURCE_INVENTORY " +
                            "WHERE resource_name=? " +
                            "FOR UPDATE",
                            resourceName
                    );

            if (inv.isEmpty()) {

                return Map.of(
                        "success", false,
                        "message",
                        "Resource not found in inventory"
                );
            }

            Map<String, Object> resource =
                    inv.get(0);

            int total =
                    iNullable(
                            resource,
                            "total_quantity"
                    );

            int allocated =
                    iNullable(
                            resource,
                            "allocated_quantity"
                    );

            int remaining =
                    total - allocated;

            if (remaining < quantity) {

                return Map.of(
                        "success", false,
                        "message",
                        "Insufficient resource stock"
                );
            }

            int updatedInventory =
                    db.update(
                            "UPDATE RESOURCE_INVENTORY " +
                            "SET allocated_quantity=" +
                            "allocated_quantity+? " +
                            "WHERE resource_id=?",
                            quantity,
                            resource.get(
                                    "resource_id"
                            )
                    );

            if (updatedInventory == 0) {

                return Map.of(
                        "success", false,
                        "message",
                        "Inventory update failed"
                );
            }

            db.update(
                    "UPDATE VICTIM SET " +
                    "resource_status='Allocated' " +
                    "WHERE victim_id=?",
                    id
            );

            return Map.of(
                    "success", true,
                    "message",
                    "Resource allocated successfully"
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    e.getMessage()
            );
        }
    }


    /* =========================================================
       RESCUE TEAM REGISTRATION
       ========================================================= */

    @PostMapping("/admin/teams")
    public Map<String, Object> registerTeam(
            @RequestBody Map<String, Object> b) {

        int teamId = i(b, "teamId");
        String teamName = s(b, "teamName");
        String leader = s(b, "leader");
        String vehicle = s(b, "vehicle");

        int updated =
                db.update(
                        "INSERT INTO RESCUE_TEAM " +
                        "(team_id, team_name, leader, " +
                        "vehicle, rescue_status) " +
                        "VALUES (?, ?, ?, ?, 'Available')",
                        teamId,
                        teamName,
                        leader,
                        vehicle
                );

        return result(
                updated,
                "Rescue team registered successfully"
        );
    }


    /* =========================================================
       GET ALL RESCUE TEAMS
       ========================================================= */

    @GetMapping("/teams")
    public List<Map<String, Object>> teams() {

        return db.queryForList(
                "SELECT * FROM RESCUE_TEAM " +
                "ORDER BY team_id"
        );
    }


    /* =========================================================
       AVAILABLE TEAMS
       ========================================================= */

    @GetMapping("/teams/available")
    public List<Map<String, Object>> availableTeams() {

        return db.queryForList(
                "SELECT * FROM RESCUE_TEAM " +
                "WHERE rescue_status='Available' " +
                "ORDER BY team_id"
        );
    }


    /* =========================================================
       RESCUE TEAM FEATURE 1

       VIEW RESCUE REQUESTS

       OS CONCEPT:
       PRIORITY SCHEDULING

       Higher severity = higher priority
       ========================================================= */

    @GetMapping("/teams/rescue-requests")
    public List<Map<String, Object>> viewRescueRequests() {

        return db.queryForList(
                "SELECT " +
                "v.victim_id, " +
                "v.name, " +
                "v.phone, " +
                "v.location, " +
                "v.emergency_type, " +
                "v.severity, " +
                "v.status, " +
                "v.team_id " +
                "FROM VICTIM v " +
                "WHERE v.status='SOS Pending' " +
                "ORDER BY v.severity DESC, " +
                "v.victim_id ASC"
        );
    }


    /* =========================================================
       RESCUE TEAM FEATURE 2

       ACCEPT RESCUE OPERATION

       OS CONCEPT:
       PROCESS SYNCHRONIZATION /
       MUTUAL EXCLUSION

       synchronized prevents concurrent access.
       FOR UPDATE locks database rows.
       Transaction provides atomic operation.
       ========================================================= */

    @PutMapping("/teams/accept")
    public synchronized Map<String, Object>
    acceptRescueOperation(
            @RequestBody Map<String, Object> b) {

        int victimId = i(b, "victimId");
        int teamId = i(b, "teamId");

        try {

            var con =
                    db.getDataSource()
                            .getConnection();

            try {

                con.setAutoCommit(false);


                /* -----------------------------------------
                   LOCK VICTIM
                   ----------------------------------------- */

                try (
                        var ps =
                                con.prepareStatement(
                                        "SELECT " +
                                        "victim_id, " +
                                        "status, " +
                                        "team_id " +
                                        "FROM VICTIM " +
                                        "WHERE victim_id=? " +
                                        "FOR UPDATE"
                                )
                ) {

                    ps.setInt(1, victimId);

                    try (
                            var rs =
                                    ps.executeQuery()
                    ) {

                        if (!rs.next()) {

                            con.rollback();

                            return Map.of(
                                    "success", false,
                                    "message",
                                    "Victim not found"
                            );
                        }

                        String status =
                                rs.getString(
                                        "status"
                                );

                        int existingTeam =
                                rs.getInt(
                                        "team_id"
                                );

                        if (rs.wasNull()) {
                            existingTeam = 0;
                        }

                        if (!"SOS Pending"
                                .equalsIgnoreCase(
                                        status
                                )) {

                            con.rollback();

                            return Map.of(
                                    "success", false,
                                    "message",
                                    "Victim is no longer pending"
                            );
                        }

                        if (existingTeam != 0) {

                            con.rollback();

                            return Map.of(
                                    "success", false,
                                    "message",
                                    "Victim already has a rescue team"
                            );
                        }
                    }
                }


                /* -----------------------------------------
                   LOCK RESCUE TEAM
                   ----------------------------------------- */

                try (
                        var ps =
                                con.prepareStatement(
                                        "SELECT " +
                                        "team_id, " +
                                        "rescue_status " +
                                        "FROM RESCUE_TEAM " +
                                        "WHERE team_id=? " +
                                        "FOR UPDATE"
                                )
                ) {

                    ps.setInt(1, teamId);

                    try (
                            var rs =
                                    ps.executeQuery()
                    ) {

                        if (!rs.next()) {

                            con.rollback();

                            return Map.of(
                                    "success", false,
                                    "message",
                                    "Rescue team not found"
                            );
                        }

                        String status =
                                rs.getString(
                                        "rescue_status"
                                );

                        if (!"Available"
                                .equalsIgnoreCase(
                                        status
                                )) {

                            con.rollback();

                            return Map.of(
                                    "success", false,
                                    "message",
                                    "Rescue team is not available"
                            );
                        }
                    }
                }


                /* -----------------------------------------
                   ASSIGN VICTIM
                   ----------------------------------------- */

                try (
                        var ps =
                                con.prepareStatement(
                                        "UPDATE VICTIM SET " +
                                        "team_id=?, " +
                                        "status='Approved' " +
                                        "WHERE victim_id=?"
                                )
                ) {

                    ps.setInt(1, teamId);
                    ps.setInt(2, victimId);

                    ps.executeUpdate();
                }


                /* -----------------------------------------
                   ASSIGN TEAM
                   ----------------------------------------- */

                try (
                        var ps =
                                con.prepareStatement(
                                        "UPDATE RESCUE_TEAM SET " +
                                        "victim_id=?, " +
                                        "rescue_status='Assigned' " +
                                        "WHERE team_id=?"
                                )
                ) {

                    ps.setInt(1, victimId);
                    ps.setInt(2, teamId);

                    ps.executeUpdate();
                }


                con.commit();

                return Map.of(
                        "success", true,
                        "message",
                        "Rescue operation accepted",
                        "victimId",
                        victimId,
                        "teamId",
                        teamId
                );

            } catch (Exception e) {

                con.rollback();

                throw e;

            } finally {

                con.close();
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    e.getMessage()
            );
        }
    }


    /* =========================================================
       RESCUE TEAM FEATURE 3

       UPDATE RESCUE STATUS

       OS CONCEPT:
       MULTITHREADING

       ExecutorService creates a worker thread.
       ========================================================= */

    @PutMapping("/teams/{teamId}/status")
    public Map<String, Object>
    updateRescueStatus(
            @PathVariable int teamId,
            @RequestBody Map<String, Object> b) {

        String newStatus =
                s(b, "status");

        ExecutorService executor =
                Executors.newSingleThreadExecutor();

        try {

            Future<Integer> future =
                    executor.submit(
                            () ->
                                    db.update(
                                            "UPDATE RESCUE_TEAM " +
                                            "SET rescue_status=? " +
                                            "WHERE team_id=?",
                                            newStatus,
                                            teamId
                                    )
                    );

            int updated =
                    future.get();

            return result(
                    updated,
                    "Rescue status updated successfully"
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    e.getMessage()
            );

        } finally {

            executor.shutdown();
        }
    }


    /* =========================================================
       RESCUE TEAM FEATURE 4

       ACCESS VICTIM LOCATION

       OS CONCEPT:
       SYSTEM-CALL / OS-MANAGED DATA ACCESS CONCEPT

       JDBC performs controlled access to database data.
       ========================================================= */

    @GetMapping("/teams/{teamId}/victim-location")
    public ResponseEntity<?> accessVictimLocation(
            @PathVariable int teamId) {

        try {

            List<Map<String, Object>> rows =
                    db.queryForList(
                            "SELECT " +
                            "rt.team_id, " +
                            "rt.team_name, " +
                            "rt.victim_id, " +
                            "v.name AS victim_name, " +
                            "v.phone, " +
                            "v.location, " +
                            "v.emergency_type, " +
                            "v.severity, " +
                            "v.status " +
                            "FROM RESCUE_TEAM rt " +
                            "INNER JOIN VICTIM v " +
                            "ON rt.victim_id=v.victim_id " +
                            "WHERE rt.team_id=?",
                            teamId
                    );

            if (rows.isEmpty()) {

                return ResponseEntity
                        .notFound()
                        .build();
            }

            return ResponseEntity.ok(
                    rows.get(0)
            );

        } catch (Exception e) {

            return ResponseEntity
                    .internalServerError()
                    .body(Map.of(
                            "success", false,
                            "message", e.getMessage()
                    ));
        }
    }


    /* =========================================================
       ADMIN ASSIGN TEAM
       ========================================================= */

    @PutMapping("/admin/assign-team")
    public Map<String, Object> assignTeam(
            @RequestBody Map<String, Object> b) {

        int victimId = i(b, "victimId");
        int teamId = i(b, "teamId");

        try {

            int victimUpdated =
                    db.update(
                            "UPDATE VICTIM SET " +
                            "team_id=?, " +
                            "status='Approved' " +
                            "WHERE victim_id=?",
                            teamId,
                            victimId
                    );

            int teamUpdated =
                    db.update(
                            "UPDATE RESCUE_TEAM SET " +
                            "victim_id=?, " +
                            "rescue_status='Assigned' " +
                            "WHERE team_id=?",
                            victimId,
                            teamId
                    );

            return Map.of(
                    "success",
                    victimUpdated > 0 &&
                    teamUpdated > 0,

                    "message",
                    "Rescue team assigned successfully"
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    e.getMessage()
            );
        }
    }


    /* =========================================================
       VOLUNTEERS
       ========================================================= */

    @GetMapping("/volunteers")
    public List<Map<String, Object>> volunteers() {

        return db.queryForList(
                "SELECT " +
                "v.*, " +
                "s.shelter_name " +
                "FROM VOLUNTEER v " +
                "LEFT JOIN SHELTER_MANAGER s " +
                "ON v.shelter_id=s.shelter_id " +
                "ORDER BY v.volunteer_id"
        );
    }


    /* =========================================================
       ADD VOLUNTEER
       ========================================================= */

    @PostMapping("/volunteers")
    public Map<String, Object> addVolunteer(
            @RequestBody Map<String, Object> b) {

        String name = s(b, "name");
        String phone = s(b, "phone");
        String skill = s(b, "skill");

        Object shelterObject =
                b.get("shelterId");

        int updated;

        if (shelterObject == null) {

            updated =
                    db.update(
                            "INSERT INTO VOLUNTEER " +
                            "(name, phone, skill, " +
                            "availability) " +
                            "VALUES (?, ?, ?, 'Available')",
                            name,
                            phone,
                            skill
                    );

        } else {

            int shelterId =
                    i(b, "shelterId");

            updated =
                    db.update(
                            "INSERT INTO VOLUNTEER " +
                            "(name, phone, skill, " +
                            "availability, shelter_id) " +
                            "VALUES (?, ?, ?, " +
                            "'Available', ?)",
                            name,
                            phone,
                            skill,
                            shelterId
                    );
        }

        return result(
                updated,
                "Volunteer added successfully"
        );
    }


    /* =========================================================
       VOLUNTEER AVAILABILITY
       ========================================================= */

    @PutMapping("/volunteers/{id}/availability")
    public Map<String, Object>
    updateVolunteerAvailability(
            @PathVariable int id,
            @RequestBody Map<String, Object> b) {

        String availability =
                s(b, "availability");

        int updated =
                db.update(
                        "UPDATE VOLUNTEER " +
                        "SET availability=? " +
                        "WHERE volunteer_id=?",
                        availability,
                        id
                );

        return result(
                updated,
                "Volunteer availability updated"
        );
    }


    /* =========================================================
       ADMIN DISASTER EVENT
       ========================================================= */

    @PutMapping("/admin/disaster")
    public Map<String, Object> disaster(
            @RequestBody Map<String, Object> b) {

        return Map.of(
                "success", true,
                "message",
                "Disaster event information updated"
        );
    }


    /* =========================================================
       ADMIN SHELTER ASSIGNMENT
       ========================================================= */

    @PutMapping("/admin/shelter")
    public Map<String, Object> assignShelter(
            @RequestBody Map<String, Object> b) {

        int victimId = i(b, "victimId");
        int shelterId = i(b, "shelterId");

        int updated =
                db.update(
                        "UPDATE VICTIM SET " +
                        "shelter_id=?, " +
                        "status='Shelter Assigned' " +
                        "WHERE victim_id=?",
                        shelterId,
                        victimId
                );

        if (updated > 0) {

            try {

                db.update(
                        "UPDATE SHELTER_MANAGER " +
                        "SET available_beds=" +
                        "available_beds-1 " +
                        "WHERE shelter_id=? " +
                        "AND available_beds>0",
                        shelterId
                );

            } catch (Exception ignored) {
            }
        }

        return result(
                updated,
                "Shelter assigned successfully"
        );
    }


    /* =========================================================
       REPORT
       ========================================================= */

    @GetMapping("/report")
    public List<Map<String, Object>> report() {

        return db.queryForList(
                "SELECT " +
                "v.victim_id, " +
                "v.name AS victim_name, " +
                "v.location, " +
                "v.emergency_type, " +
                "v.severity, " +
                "v.status, " +
                "rt.team_name, " +
                "sm.shelter_name, " +
                "v.resource_status " +
                "FROM VICTIM v " +
                "LEFT JOIN RESCUE_TEAM rt " +
                "ON v.team_id=rt.team_id " +
                "LEFT JOIN SHELTER_MANAGER sm " +
                "ON v.shelter_id=sm.shelter_id " +
                "ORDER BY v.victim_id DESC"
        );
    }


    /* =========================================================
       FCFS SCHEDULING
       ========================================================= */

    @PostMapping("/scheduling/fcfs")
    public Map<String, Object> fcfs(
            @RequestBody List<Map<String, Object>> jobs) {

        if (jobs == null ||
            jobs.isEmpty()) {

            return Map.of(
                    "success", false,
                    "message",
                    "No scheduling jobs supplied"
            );
        }

        List<Map<String, Object>> sorted =
                new ArrayList<>(jobs);

        sorted.sort(
                Comparator.comparingInt(
                        (Map<String, Object> x) ->
                                i(x, "arrival")
                )
        );

        int currentTime = 0;

        double totalWaiting = 0;
        double totalTurnaround = 0;

        List<Map<String, Object>> result =
                new ArrayList<>();

        for (Map<String, Object> job : sorted) {

            int pid = i(job, "pid");
            int arrival = i(job, "arrival");
            int burst = i(job, "burst");

            if (currentTime < arrival) {
                currentTime = arrival;
            }

            int start = currentTime;

            int completion =
                    start + burst;

            int turnaround =
                    completion - arrival;

            int waiting =
                    turnaround - burst;

            totalWaiting += waiting;
            totalTurnaround += turnaround;

            Map<String, Object> row =
                    new LinkedHashMap<>();

            row.put("pid", pid);
            row.put("arrival", arrival);
            row.put("burst", burst);
            row.put("start", start);
            row.put("completion", completion);
            row.put("waiting", waiting);
            row.put("turnaround", turnaround);

            result.add(row);

            currentTime = completion;
        }

        return Map.of(
                "success", true,
                "jobs", result,
                "averageWaitingTime",
                totalWaiting / result.size(),
                "averageTurnaroundTime",
                totalTurnaround / result.size()
        );
    }


    /* =========================================================
       PRIORITY SCHEDULING

       Higher priority number = higher priority

       IMPORTANT:
       This version intentionally uses normal loops instead
       of stream Comparator lambdas so that Java 17 does not
       produce the Object -> Map<String,Object> compilation
       error.
       ========================================================= */

    @PostMapping("/scheduling/priority")
    public Map<String, Object> priority(
            @RequestBody List<Map<String, Object>> jobs) {

        if (jobs == null ||
            jobs.isEmpty()) {

            return Map.of(
                    "success", false,
                    "message",
                    "No scheduling jobs supplied"
            );
        }

        List<Map<String, Object>> pending =
                new ArrayList<>(jobs);

        List<Map<String, Object>> result =
                new ArrayList<>();

        int currentTime = 0;

        double totalWaiting = 0;

        double totalTurnaround = 0;


        while (!pending.isEmpty()) {

            /*
             * Find all processes that have arrived.
             */

            List<Map<String, Object>> available =
                    new ArrayList<>();

            for (Map<String, Object> job : pending) {

                int arrival =
                        i(job, "arrival");

                if (arrival <= currentTime) {

                    available.add(job);
                }
            }


            /*
             * If no process has arrived,
             * move time to the earliest arrival.
             */

            if (available.isEmpty()) {

                int earliestArrival =
                        Integer.MAX_VALUE;

                Map<String, Object> earliestJob =
                        null;

                for (Map<String, Object> job : pending) {

                    int arrival =
                            i(job, "arrival");

                    if (arrival < earliestArrival) {

                        earliestArrival =
                                arrival;

                        earliestJob =
                                job;
                    }
                }

                if (earliestJob == null) {
                    break;
                }

                currentTime =
                        earliestArrival;

                available.clear();

                for (Map<String, Object> job : pending) {

                    int arrival =
                            i(job, "arrival");

                    if (arrival <= currentTime) {

                        available.add(job);
                    }
                }
            }


            /*
             * Select highest-priority process.
             *
             * Higher priority number wins.
             *
             * If priority is equal,
             * earlier arrival wins.
             */

            Map<String, Object> selected =
                    available.get(0);

            for (Map<String, Object> job : available) {

                int jobPriority =
                        i(job, "priority");

                int selectedPriority =
                        i(selected, "priority");

                int jobArrival =
                        i(job, "arrival");

                int selectedArrival =
                        i(selected, "arrival");


                if (jobPriority > selectedPriority) {

                    selected = job;

                } else if (
                        jobPriority ==
                                selectedPriority
                        &&
                        jobArrival <
                                selectedArrival
                ) {

                    selected = job;
                }
            }


            /*
             * Remove selected process.
             */

            pending.remove(selected);


            /*
             * Process information.
             */

            int pid =
                    i(selected, "pid");

            int arrival =
                    i(selected, "arrival");

            int burst =
                    i(selected, "burst");

            int priority =
                    i(selected, "priority");


            /*
             * Start time.
             */

            int start =
                    currentTime;


            /*
             * Completion time.
             */

            int completion =
                    start + burst;


            /*
             * Turnaround Time:
             *
             * TAT = CT - AT
             */

            int turnaround =
                    completion - arrival;


            /*
             * Waiting Time:
             *
             * WT = TAT - BT
             */

            int waiting =
                    turnaround - burst;


            totalWaiting += waiting;

            totalTurnaround += turnaround;


            /*
             * Create result row.
             */

            Map<String, Object> row =
                    new LinkedHashMap<>();

            row.put(
                    "pid",
                    pid
            );

            row.put(
                    "arrival",
                    arrival
            );

            row.put(
                    "burst",
                    burst
            );

            row.put(
                    "priority",
                    priority
            );

            row.put(
                    "start",
                    start
            );

            row.put(
                    "completion",
                    completion
            );

            row.put(
                    "waiting",
                    waiting
            );

            row.put(
                    "turnaround",
                    turnaround
            );

            result.add(row);


            /*
             * Move CPU time forward.
             */

            currentTime =
                    completion;
        }


        /*
         * Return result.
         */

        return Map.of(
                "success", true,
                "jobs", result,
                "averageWaitingTime",
                totalWaiting / result.size(),
                "averageTurnaroundTime",
                totalTurnaround / result.size()
        );
    }


    /* =========================================================
       HELPER: COUNT
       ========================================================= */

    private int count(String sql) {

        try {

            Integer value =
                    db.queryForObject(
                            sql,
                            Integer.class
                    );

            return value == null
                    ? 0
                    : value;

        } catch (Exception e) {

            return 0;
        }
    }


    /* =========================================================
       HELPER: INTEGER
       ========================================================= */

    private int i(
            Map<String, Object> map,
            String key) {

        Object value =
                map.get(key);

        if (value == null) {
            return 0;
        }

        if (value instanceof Number) {

            return ((Number) value)
                    .intValue();
        }

        try {

            return Integer.parseInt(
                    value.toString()
            );

        } catch (Exception e) {

            return 0;
        }
    }


    /* =========================================================
       HELPER: NULLABLE INTEGER
       ========================================================= */

    private int iNullable(
            Map<String, Object> map,
            String key) {

        Object value =
                map.get(key);

        if (value == null) {
            return 0;
        }

        if (value instanceof Number) {

            return ((Number) value)
                    .intValue();
        }

        try {

            return Integer.parseInt(
                    value.toString()
            );

        } catch (Exception e) {

            return 0;
        }
    }


    /* =========================================================
       HELPER: STRING
       ========================================================= */

    private String s(
            Map<String, Object> map,
            String key) {

        Object value =
                map.get(key);

        return value == null
                ? ""
                : value.toString();
    }


    /* =========================================================
       HELPER: RESULT
       ========================================================= */

    private Map<String, Object> result(
            int updated,
            String message) {

        return Map.of(
                "success",
                updated > 0,

                "updated",
                updated,

                "message",
                updated > 0
                        ? message
                        : "No record was updated"
        );
    }
}
