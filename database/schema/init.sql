-- Initial schema creation for RecruitShield
-- (Docker will run this file on initialization if we use the docker-compose setup)

CREATE DATABASE IF NOT EXISTS recruitshield;
USE recruitshield;

CREATE TABLE IF NOT EXISTS scan_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    offer_text TEXT NOT NULL,
    risk_score INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
