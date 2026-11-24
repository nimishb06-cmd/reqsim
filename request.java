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

        int updated = db.update(
                "UPDATE VICTIM SET resource_request=?, " +
                "resource_status='Pending' WHERE victim_id=?",
                requestText, id
        );

        if (updated > 0) {
            db.update(
                    "INSERT INTO RESOURCE_REQUEST_HISTORY " +
                    "(victim_id, resource_name, quantity, request_status) " +
                    "VALUES (?, ?, ?, 'Pending')",
                    id, resource, quantity
            );
        }

        return result(updated, "Resource request submitted");
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

            List<Map<String, Object>> rows = db.queryForList(
                    "SELECT v.victim_id, v.name, v.location, " +
                    "v.emergency_type, v.severity, v.status, v.team_id, " +
                    "rt.team_name, rt.leader AS team_leader, " +
                    "rt.vehicle AS team_vehicle, rt.rescue_status, " +
                    "v.resource_request, v.resource_status " +
                    "FROM VICTIM v LEFT JOIN RESCUE_TEAM rt " +
                    "ON v.team_id=rt.team_id WHERE v.victim_id=?",
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
       VICTIM RESOURCE REQUEST HISTORY
       ========================================================= */

    @GetMapping("/victims/{id}/resource-history")
    public ResponseEntity<?> victimResourceHistory(@PathVariable int id) {
        try {
            Integer exists = db.queryForObject(
                    "SELECT COUNT(*) FROM VICTIM WHERE victim_id=?",
                    Integer.class, id
            );
            if (exists == null || exists == 0) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(db.queryForList(
                    "SELECT history_id, victim_id, resource_name, quantity, " +
                    "request_status, requested_at " +
                    "FROM RESOURCE_REQUEST_HISTORY WHERE victim_id=? " +
                    "ORDER BY requested_at DESC, history_id DESC",
                    id
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }


    /* =========================================================
       SHELTERS
       ========================================================= */

    @GetMapping("/shelters")
    public ResponseEntity<?> shelters() {
        // Used by the victim portal's Request Shelter dropdown and shelter list.
        try {
            return ResponseEntity.ok(loadAvailableShelters());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Could not load available shelters. Check the SHELTER_MANAGER table and database connection.",
                    "detail", String.valueOf(e.getMessage())
            ));
        }
    }

    // Return both snake_case and camelCase keys so existing frontend versions
    // can read the same API response without changing the rest of the portal.
    private List<Map<String, Object>> loadAvailableShelters() {
        List<Map<String, Object>> rows = db.queryForList(
                "SELECT shelter_id, shelter_name, address, capacity, available_beds " +
                "FROM SHELTER_MANAGER " +
                "WHERE available_beds > 0 " +
                "ORDER BY available_beds DESC, shelter_name"
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            Object id = row.get("shelter_id");
            Object name = row.get("shelter_name");
            Object address = row.get("address");
            Object capacity = row.get("capacity");
            Object beds = row.get("available_beds");

            item.put("shelter_id", id);
            item.put("shelterId", id);
            item.put("shelter_name", name);
            item.put("shelterName", name);
            item.put("address", address);
            item.put("capacity", capacity);
            item.put("available_beds", beds);
            item.put("availableBeds", beds);
            result.add(item);
        }
        return result;
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
       MARK SOS AS RESOLVED
       ========================================================= */

    @PutMapping("/admin/sos/{id}/resolve")
    public Map<String, Object> resolveSos(
            @PathVariable int id) {

        int updated = db.update(
                "UPDATE VICTIM SET status='Resolved' " +
                "WHERE victim_id=? AND status='SOS Pending'",
                id
        );

        return result(
                updated,
                updated > 0
                        ? "SOS marked as resolved"
                        : "Pending SOS not found for this victim"
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
    @Transactional
    public Map<String, Object> allocateResource(
            @PathVariable int id) {

        try {
            List<Map<String, Object>> rows = db.queryForList(
                    "SELECT resource_request, resource_status " +
                    "FROM VICTIM WHERE victim_id=? FOR UPDATE",
                    id
            );

            if (rows.isEmpty()) {
                return Map.of(
                        "success", false,
                        "message", "Victim not found"
                );
            }

            Map<String, Object> victim = rows.get(0);
            String request = s(victim, "resource_request");
            String status = s(victim, "resource_status");

            if (request == null || request.isBlank()) {
                return Map.of(
                        "success", false,
                        "message", "No resource request found"
                );
            }

            if (!"Pending".equalsIgnoreCase(status)) {
                return Map.of(
                        "success", false,
                        "message", "This resource request is not pending"
                );
            }

            String resourceName = request;
            int quantity = 1;

            if (request.contains(":")) {
                String[] parts = request.split(":", 2);
                resourceName = parts[0].trim();

                try {
                    quantity = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException ex) {
                    return Map.of(
                            "success", false,
                            "message", "Invalid quantity in resource request"
                    );
                }
            }

            if (quantity <= 0) {
                return Map.of(
                        "success", false,
                        "message", "Requested quantity must be greater than zero"
                );
            }

            // Lock the matching inventory row to prevent concurrent over-allocation.
            List<Map<String, Object>> inventoryRows = db.queryForList(
                    "SELECT resource_id, total_quantity, allocated_quantity " +
                    "FROM RESOURCE_INVENTORY " +
                    "WHERE LOWER(TRIM(resource_name))=LOWER(TRIM(?)) " +
                    "FOR UPDATE",
                    resourceName
            );

            if (inventoryRows.isEmpty()) {
                return Map.of(
                        "success", false,
                        "message", "Resource not found in inventory"
                );
            }

            Map<String, Object> resource = inventoryRows.get(0);
            int total = iNullable(resource, "total_quantity");
            int allocated = iNullable(resource, "allocated_quantity");
            int remaining = total - allocated;

            if (remaining < quantity) {
                return Map.of(
                        "success", false,
                        "message", "Insufficient stock. Available: " + Math.max(remaining, 0)
                );
            }

            int updatedInventory = db.update(
                    "UPDATE RESOURCE_INVENTORY " +
                    "SET allocated_quantity=allocated_quantity+? " +
                    "WHERE resource_id=? " +
                    "AND (total_quantity-allocated_quantity)>=?",
                    quantity,
                    resource.get("resource_id"),
                    quantity
            );

            if (updatedInventory == 0) {
                throw new IllegalStateException(
                        "Stock changed during allocation. Please refresh and try again."
                );
            }

            int updatedVictim = db.update(
                    "UPDATE VICTIM SET resource_status='Allocated' " +
                    "WHERE victim_id=? AND resource_status='Pending'",
                    id
            );

            if (updatedVictim > 0) {
                db.update(
                        "UPDATE RESOURCE_REQUEST_HISTORY " +
                        "SET request_status='Allocated' " +
                        "WHERE history_id=(SELECT history_id FROM (" +
                        "SELECT history_id FROM RESOURCE_REQUEST_HISTORY " +
                        "WHERE victim_id=? AND request_status='Pending' " +
                        "ORDER BY requested_at DESC, history_id DESC LIMIT 1" +
                        ") latest_request)",
                        id
                );
            }

            if (updatedVictim == 0) {
                throw new IllegalStateException(
                        "Request status changed during allocation. Transaction cancelled."
                );
            }

            return Map.of(
                    "success", true,
                    "message", quantity + " unit(s) of " +
                            resourceName + " allocated successfully"
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Resource allocation failed: " + e.getMessage(),
                    e
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
                "SELECT " +
                "rt.team_id, " +
                "rt.team_name, " +
                "rt.leader, " +
                "rt.vehicle, " +
                "rt.rescue_status, " +
                "rt.victim_id, " +
                "v.name AS victim_name, " +
                "v.severity AS victim_severity, " +
                "v.location AS victim_location, " +
                "v.emergency_type AS victim_emergency_type, " +
                "v.status AS victim_status " +
                "FROM RESCUE_TEAM rt " +
                "LEFT JOIN VICTIM v " +
                "ON rt.victim_id = v.victim_id " +
                "ORDER BY rt.team_id"
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
       SHELTER MANAGEMENT / VICTIM SHELTER REQUESTS
       OS: semaphore synchronization; DBMS: transactions, row locks,
       foreign keys, joins, aggregation and reporting view.
       ========================================================= */

    @PostMapping("/admin/shelters")
    @Transactional
    public ResponseEntity<Map<String, Object>> registerShelter(@RequestBody Map<String, Object> b) {
        String name = s(b, "shelterName").trim();
        String address = s(b, "address").trim();
        int capacity = i(b, "capacity");
        if (name.isEmpty() || address.isEmpty() || capacity <= 0)
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Shelter name, address and positive capacity are required"));
        db.update("INSERT INTO SHELTER_MANAGER (shelter_name,address,capacity,available_beds) VALUES (?,?,?,?)", name, address, capacity, capacity);
        return ResponseEntity.ok(Map.of("success", true, "message", "Shelter registered successfully"));
    }

    @PutMapping("/admin/shelters/{shelterId}/capacity")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateShelterCapacity(@PathVariable int shelterId, @RequestBody Map<String, Object> b) {
        int capacity = i(b, "capacity");
        if (capacity < 0) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Capacity cannot be negative"));
        boolean acquired = false;
        try {
            shelterSemaphore.acquire(); acquired = true;
            List<Map<String,Object>> rows = db.queryForList("SELECT capacity,available_beds FROM SHELTER_MANAGER WHERE shelter_id=? FOR UPDATE", shelterId);
            if (rows.isEmpty()) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Shelter not found"));
            int oldCapacity = ((Number) rows.get(0).get("capacity")).intValue();
            int available = ((Number) rows.get(0).get("available_beds")).intValue();
            int occupied = oldCapacity - available;
            if (capacity < occupied) return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Capacity cannot be lower than occupied beds (" + occupied + ")"));
            db.update("UPDATE SHELTER_MANAGER SET capacity=?, available_beds=? WHERE shelter_id=?", capacity, capacity - occupied, shelterId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Shelter capacity updated"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); return ResponseEntity.status(503).body(Map.of("success", false, "message", "Capacity update interrupted"));
        } finally { if (acquired) shelterSemaphore.release(); }
    }

    @GetMapping({"/victims/{victimId}/available-shelters", "/victim/{victimId}/available-shelters"})
    public ResponseEntity<?> availableShelters(@PathVariable int victimId) {
        try {
            Integer victimCount = db.queryForObject(
                    "SELECT COUNT(*) FROM VICTIM WHERE victim_id=?",
                    Integer.class, victimId
            );
            if (victimCount == null || victimCount == 0) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Victim not found"
                ));
            }
            return ResponseEntity.ok(loadAvailableShelters());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Could not load available shelters.",
                    "detail", String.valueOf(e.getMessage())
            ));
        }
    }

    @PostMapping("/victims/{victimId}/shelter-request")
    @Transactional
    public ResponseEntity<Map<String,Object>> requestShelter(@PathVariable int victimId, @RequestBody Map<String,Object> b) {
        int shelterId = i(b, "shelterId");
        if (victimId <= 0 || shelterId <= 0) return ResponseEntity.badRequest().body(Map.of("success",false,"message","Valid victim and shelter are required"));
        Integer victimCount = db.queryForObject("SELECT COUNT(*) FROM VICTIM WHERE victim_id=?", Integer.class, victimId);
        Integer shelterCount = db.queryForObject("SELECT COUNT(*) FROM SHELTER_MANAGER WHERE shelter_id=? AND available_beds>0", Integer.class, shelterId);
        if (victimCount == null || victimCount == 0) return ResponseEntity.badRequest().body(Map.of("success",false,"message","Victim not found"));
        if (shelterCount == null || shelterCount == 0) return ResponseEntity.badRequest().body(Map.of("success",false,"message","Shelter not found or full"));
        Integer pending = db.queryForObject("SELECT COUNT(*) FROM SHELTER_REQUEST WHERE victim_id=? AND request_status='Pending'", Integer.class, victimId);
        if (pending != null && pending > 0) return ResponseEntity.badRequest().body(Map.of("success",false,"message","You already have a pending shelter request"));
        db.update("INSERT INTO SHELTER_REQUEST (victim_id,shelter_id,request_status) VALUES (?,?, 'Pending')", victimId, shelterId);
        return ResponseEntity.ok(Map.of("success",true,"message","Shelter request submitted. The command center will review it."));
    }

    @GetMapping("/victims/{victimId}/shelter-requests")
    public List<Map<String,Object>> victimShelterRequests(@PathVariable int victimId) {
        return db.queryForList("SELECT r.request_id,r.shelter_id,r.request_status,r.requested_at,r.processed_at,s.shelter_name,s.address FROM SHELTER_REQUEST r JOIN SHELTER_MANAGER s ON s.shelter_id=r.shelter_id WHERE r.victim_id=? ORDER BY r.requested_at DESC", victimId);
    }

    @GetMapping("/admin/shelter-requests")
    public List<Map<String,Object>> adminShelterRequests() {
        return db.queryForList("SELECT r.request_id,r.victim_id,v.name,v.phone,v.location,r.shelter_id,s.shelter_name,s.address,r.request_status,r.requested_at FROM SHELTER_REQUEST r JOIN VICTIM v ON v.victim_id=r.victim_id JOIN SHELTER_MANAGER s ON s.shelter_id=r.shelter_id WHERE r.request_status='Pending' ORDER BY r.requested_at");
    }

    @PutMapping("/admin/shelter-requests/{requestId}/approve")
    @Transactional
    public ResponseEntity<Map<String,Object>> approveShelterRequest(@PathVariable int requestId) {
        boolean acquired = false;
        try {
            shelterSemaphore.acquire(); acquired = true;
            List<Map<String,Object>> rows = db.queryForList("SELECT request_id,victim_id,shelter_id FROM SHELTER_REQUEST WHERE request_id=? AND request_status='Pending' FOR UPDATE", requestId);
            if (rows.isEmpty()) return ResponseEntity.badRequest().body(Map.of("success",false,"message","Pending request not found"));
            Map<String,Object> req = rows.get(0);
            int victimId = ((Number)req.get("victim_id")).intValue();
            int shelterId = ((Number)req.get("shelter_id")).intValue();
            List<Map<String,Object>> shelterRows = db.queryForList("SELECT available_beds FROM SHELTER_MANAGER WHERE shelter_id=? FOR UPDATE", shelterId);
            if (shelterRows.isEmpty() || ((Number)shelterRows.get(0).get("available_beds")).intValue() <= 0)
                return ResponseEntity.badRequest().body(Map.of("success",false,"message","The selected shelter is now full"));
            List<Map<String,Object>> victimRows = db.queryForList("SELECT shelter_id FROM VICTIM WHERE victim_id=? FOR UPDATE", victimId);
            if (victimRows.isEmpty()) return ResponseEntity.badRequest().body(Map.of("success",false,"message","Victim not found"));
            Object oldValue = victimRows.get(0).get("shelter_id");
            Integer oldShelter = oldValue == null ? null : ((Number)oldValue).intValue();
            if (oldShelter != null && oldShelter == shelterId) {
                db.update("UPDATE SHELTER_REQUEST SET request_status='Approved',processed_at=CURRENT_TIMESTAMP WHERE request_id=?", requestId);
                return ResponseEntity.ok(Map.of("success",true,"message","Victim is already assigned to this shelter"));
            }
            db.update("UPDATE SHELTER_MANAGER SET available_beds=available_beds-1 WHERE shelter_id=? AND available_beds>0", shelterId);
            if (oldShelter != null) db.update("UPDATE SHELTER_MANAGER SET available_beds=available_beds+1 WHERE shelter_id=?", oldShelter);
            db.update("UPDATE VICTIM SET shelter_id=?,status='Shelter Assigned' WHERE victim_id=?", shelterId, victimId);
            db.update("UPDATE SHELTER_REQUEST SET request_status='Approved',processed_at=CURRENT_TIMESTAMP WHERE request_id=?", requestId);
            db.update("UPDATE SHELTER_REQUEST SET request_status='Rejected',processed_at=CURRENT_TIMESTAMP WHERE victim_id=? AND request_id<>? AND request_status='Pending'", victimId, requestId);
            return ResponseEntity.ok(Map.of("success",true,"message","Shelter request approved and bed allocated"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); return ResponseEntity.status(503).body(Map.of("success",false,"message","Approval interrupted"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success",false,"message","Could not approve shelter request: "+e.getMessage()));
        } finally { if (acquired) shelterSemaphore.release(); }
    }

    @PutMapping("/admin/shelter-requests/{requestId}/reject")
    public ResponseEntity<Map<String,Object>> rejectShelterRequest(@PathVariable int requestId) {
        int n = db.update("UPDATE SHELTER_REQUEST SET request_status='Rejected',processed_at=CURRENT_TIMESTAMP WHERE request_id=? AND request_status='Pending'", requestId);
        return n > 0 ? ResponseEntity.ok(Map.of("success",true,"message","Shelter request rejected")) : ResponseEntity.badRequest().body(Map.of("success",false,"message","Pending request not found"));
    }

    @PutMapping("/admin/victims/{victimId}/release-shelter")
    @Transactional
    public ResponseEntity<Map<String,Object>> releaseShelter(@PathVariable int victimId) {
        boolean acquired = false;
        try {
            shelterSemaphore.acquire(); acquired = true;
            List<Map<String,Object>> rows = db.queryForList("SELECT shelter_id FROM VICTIM WHERE victim_id=? FOR UPDATE", victimId);
            if (rows.isEmpty()) return ResponseEntity.badRequest().body(Map.of("success",false,"message","Victim not found"));
            Object value = rows.get(0).get("shelter_id");
            if (value == null) return ResponseEntity.badRequest().body(Map.of("success",false,"message","Victim has no assigned shelter"));
            int shelterId = ((Number)value).intValue();
            db.update("UPDATE VICTIM SET shelter_id=NULL WHERE victim_id=?", victimId);
            db.update("UPDATE SHELTER_MANAGER SET available_beds=LEAST(capacity,available_beds+1) WHERE shelter_id=?", shelterId);
            return ResponseEntity.ok(Map.of("success",true,"message","Shelter bed released"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); return ResponseEntity.status(503).body(Map.of("success",false,"message","Release interrupted"));
        } finally { if (acquired) shelterSemaphore.release(); }
    }

    @GetMapping("/admin/shelter-report")
    public List<Map<String,Object>> shelterReport() {
        return db.queryForList("SELECT * FROM SHELTER_STATUS_REPORT ORDER BY shelter_name");
    }

