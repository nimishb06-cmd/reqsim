CREATE DATABASE IF NOT EXISTS resqsim;
USE resqsim;

CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    phone VARCHAR(20),
    password VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL
);

CREATE TABLE disaster (
    disaster_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    location VARCHAR(150),
    description VARCHAR(255),
    status VARCHAR(30) DEFAULT 'ACTIVE'
);

CREATE TABLE victim (
    victim_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE rescue_team (
    team_id INT AUTO_INCREMENT PRIMARY KEY,
    team_name VARCHAR(100) NOT NULL,
    location VARCHAR(100),
    status VARCHAR(30) DEFAULT 'AVAILABLE'
);

CREATE TABLE volunteer (
    volunteer_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    skill VARCHAR(100),
    availability VARCHAR(30) DEFAULT 'AVAILABLE',
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE shelter (
    shelter_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    location VARCHAR(150),
    capacity INT NOT NULL,
    occupied INT DEFAULT 0,
    CHECK (occupied >= 0),
    CHECK (occupied <= capacity)
);

CREATE TABLE resource (
    resource_id INT AUTO_INCREMENT PRIMARY KEY,
    resource_name VARCHAR(100) NOT NULL,
    quantity INT DEFAULT 0,
    CHECK (quantity >= 0)
);

CREATE TABLE rescue_request (
    request_id INT AUTO_INCREMENT PRIMARY KEY,
    victim_id INT NOT NULL,
    disaster_id INT,
    location VARCHAR(150),
    emergency_type VARCHAR(100),
    priority INT NOT NULL,
    status VARCHAR(30) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (victim_id)
        REFERENCES victim(victim_id),

    FOREIGN KEY (disaster_id)
        REFERENCES disaster(disaster_id),

    CHECK (priority BETWEEN 1 AND 5)
);

CREATE INDEX idx_request_priority
ON rescue_request(priority);

CREATE INDEX idx_request_status
ON rescue_request(status);

CREATE TABLE request_team (
    request_id INT,
    team_id INT,

    PRIMARY KEY(request_id, team_id),

    FOREIGN KEY(request_id)
        REFERENCES rescue_request(request_id),

    FOREIGN KEY(team_id)
        REFERENCES rescue_team(team_id)
);

CREATE TABLE task (
    task_id INT AUTO_INCREMENT PRIMARY KEY,
    volunteer_id INT,
    task_description VARCHAR(255),
    status VARCHAR(30) DEFAULT 'ASSIGNED',

    FOREIGN KEY(volunteer_id)
        REFERENCES volunteer(volunteer_id)
);

CREATE TABLE resource_allocation (
    allocation_id INT AUTO_INCREMENT PRIMARY KEY,
    resource_id INT,
    request_id INT,
    quantity INT,

    FOREIGN KEY(resource_id)
        REFERENCES resource(resource_id),

    FOREIGN KEY(request_id)
        REFERENCES rescue_request(request_id)
);

CREATE TABLE shelter_victim (
    shelter_id INT,
    victim_id INT,

    PRIMARY KEY(shelter_id, victim_id),

    FOREIGN KEY(shelter_id)
        REFERENCES shelter(shelter_id),

    FOREIGN KEY(victim_id)
        REFERENCES victim(victim_id)
);