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