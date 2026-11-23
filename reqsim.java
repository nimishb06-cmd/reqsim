package com.resqsim.web;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;


/* ============================================================
   RESQSIM CONTROLLER
   Disaster Response & Resource Management System
   ============================================================ */

@RestController
@RequestMapping("/api")
@CrossOrigin
public class ResqsimController {

    private final JdbcTemplate db;
    // OS synchronization: serialize shelter-bed updates inside this app instance.
    private final Semaphore shelterSemaphore = new Semaphore(1, true);

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

        int resolvedSOS =
                count(
                        "SELECT COUNT(*) FROM VICTIM " +
                        "WHERE status IN ('Resolved','Completed')"
                );

        int highPrioritySOS =
                count(
                        "SELECT COUNT(*) FROM VICTIM " +
                        "WHERE status='SOS Pending' " +
                        "AND severity >= 4"
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
                "resolvedSOS", resolvedSOS,
                "highPrioritySOS", highPrioritySOS,
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