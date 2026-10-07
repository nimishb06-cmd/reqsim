# RESQSIM — Complete Website Version

RESQSIM (Disaster Response Resource Allocation System) converted from the original console Java project into a browser-based disaster-response command center.

## Architecture

Browser (HTML/CSS/JavaScript) → Spring Boot REST API → JDBC → MySQL

## Features

### Admin Command Center
- Admin login
- Operations dashboard
- Victim management
- SOS emergency queue sorted by severity
- Rescue team monitoring and dispatch
- Volunteer management and availability
- Shelter capacity monitoring and victim assignment
- Resource request allocation
- FCFS scheduling
- Priority scheduling
- SQL operational report
- MySQL connection health indicator

### Victim Portal
- Victim login using Victim ID + registered phone
- Emergency status tracking
- Send SOS
- View assigned rescue team
- View assigned shelter
- Request resources
- View available shelters

## Requirements

- Java 17 or newer
- Maven 3.8+
- MySQL 8.x
- MySQL Workbench (recommended)

## 1. Create the database

Open `schema.sql` in MySQL Workbench and run it.

The default database is:

`disaster_db`

The schema includes:

- ADMIN
- VICTIM
- RESCUE_TEAM
- VOLUNTEER
- SHELTER_MANAGER

Demo admin:

- Email: `admin@resqsim.local`
- Password: `admin123`

## 2. Check MySQL port

This project is configured for the user's existing setup:

`127.0.0.1:3307`

If MySQL uses the normal port 3306, edit:

`src/main/resources/application.properties`

Change:

`spring.datasource.url=jdbc:mysql://127.0.0.1:3307/disaster_db`

to:

`spring.datasource.url=jdbc:mysql://127.0.0.1:3306/disaster_db`

Also change the username/password if your MySQL account is different.

## 3. Run on Windows

Open PowerShell in the project directory:

```powershell
cd resqsim-web
mvn spring-boot:run
```

Or build first:

```powershell
mvn clean package
java -jar target/resqsim-web-1.0.0.jar
```

Then open:

`http://localhost:8080`

## 4. Login

### Admin

Email:
`admin@resqsim.local`

Password:
`admin123`

### Victim

Register a victim from the Admin dashboard, note the generated Victim ID, then use:

- Victim ID
- Registered phone number

## Main REST endpoints

- `POST /api/login`
- `GET /api/health`
- `GET /api/dashboard`
- `GET /api/victims`
- `POST /api/victims`
- `PUT /api/victims/{id}/sos`
- `PUT /api/victims/{id}/resource`
- `GET /api/victim/{id}/full`
- `GET /api/admin/sos`
- `GET /api/admin/resources`
- `PUT /api/admin/resources/{id}`
- `GET /api/teams`
- `PUT /api/admin/assign-team`
- `GET /api/volunteers`
- `POST /api/volunteers`
- `PUT /api/volunteers/{id}/availability`
- `GET /api/shelters`
- `PUT /api/admin/shelter`
- `GET /api/report`
- `POST /api/scheduling/fcfs`
- `POST /api/scheduling/priority`

## Important college-project note

The login is intentionally simple for a demonstration project. Passwords are stored as plain text in this version. For a production system, use password hashing and Spring Security with session/JWT authentication.

## Updated Admin Rescue-Team Integration

The web Admin dashboard now follows the same rescue-team logic as the new Java Admin backend:

- Register a new rescue team from **Admin → Rescue Teams**.
- View all rescue teams and their `Available` / `Assigned` status.
- Assign an available rescue team to a pending SOS.
- Assignment runs as one database transaction:
  - `VICTIM.team_id` is set to the selected team.
  - `VICTIM.status` changes from `SOS Pending` to `Approved`.
  - `RESCUE_TEAM.rescue_status` changes from `Available` to `Assigned`.
  - If either update fails, the transaction is rolled back.
- The old `RESCUE_TEAM.victim_id` field is no longer used.
- SOS severity is 1–4 to match the Java project.

### Existing database migration

If you already created the database using an older RESQSIM version, see `migration_new_admin.sql` before restarting the application. If your old table contains `victim_id`, either leave it as an unused nullable column or remove it with the migration script. The application now uses `VICTIM.team_id` for the relationship.

### New REST endpoint

`POST /api/admin/teams`

Example request:

```json
{
  "teamId": 104,
  "teamName": "Emergency Response Team",
  "leader": "Rahul Verma",
  "vehicle": "Rescue Van 04"
}
```
