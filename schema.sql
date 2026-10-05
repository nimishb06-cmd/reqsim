CREATE DATABASE IF NOT EXISTS disaster_db;
USE disaster_db;

CREATE TABLE IF NOT EXISTS ADMIN (
  admin_id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(150) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  phone VARCHAR(20)
);
CREATE TABLE IF NOT EXISTS SHELTER_MANAGER (
  shelter_id INT AUTO_INCREMENT PRIMARY KEY,
  shelter_name VARCHAR(150) NOT NULL,
  address VARCHAR(255) NOT NULL,
  capacity INT NOT NULL DEFAULT 0,
  available_beds INT NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS RESCUE_TEAM (
  team_id INT AUTO_INCREMENT PRIMARY KEY,
  team_name VARCHAR(120) NOT NULL,
  leader VARCHAR(100),
  vehicle VARCHAR(100),
  rescue_status VARCHAR(30) NOT NULL DEFAULT 'Available',
  victim_id INT NULL
);
CREATE TABLE IF NOT EXISTS VOLUNTEER (
  volunteer_id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  phone VARCHAR(20),
  skill VARCHAR(120),
  shelter_id INT NULL,
  availability VARCHAR(30) NOT NULL DEFAULT 'Available',
  FOREIGN KEY (shelter_id) REFERENCES SHELTER_MANAGER(shelter_id) ON DELETE SET NULL
);
CREATE TABLE IF NOT EXISTS VICTIM (
  victim_id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  phone VARCHAR(20),
  location VARCHAR(255),
  emergency_type VARCHAR(100),
  severity INT DEFAULT 1,
  status VARCHAR(50) NOT NULL DEFAULT 'Registered',
  team_id INT NULL,
  shelter_id INT NULL,
  admin_id INT NULL,
  resource_request VARCHAR(255),
  resource_status VARCHAR(50),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (team_id) REFERENCES RESCUE_TEAM(team_id) ON DELETE SET NULL,
  FOREIGN KEY (shelter_id) REFERENCES SHELTER_MANAGER(shelter_id) ON DELETE SET NULL,
  FOREIGN KEY (admin_id) REFERENCES ADMIN(admin_id) ON DELETE SET NULL
);

INSERT INTO SHELTER_MANAGER(shelter_name,address,capacity,available_beds) VALUES
('City Relief Shelter','Rajpur Road, Dehradun',100,76),
('Community Hall Shelter','Clock Tower, Dehradun',70,42),
('School Emergency Shelter','Prem Nagar, Dehradun',120,95);
INSERT INTO RESCUE_TEAM(team_name,leader,vehicle,rescue_status) VALUES
('Alpha Rescue','Aman Rawat','Rescue Van 01','Available'),
('Bravo Rescue','Rohit Negi','Ambulance 02','Available'),
('Charlie Rescue','Vikas Bisht','Rescue Truck 03','Available');
INSERT INTO ADMIN(name,email,password,phone) VALUES('System Admin','admin@resqsim.local','admin123','9999999999');
