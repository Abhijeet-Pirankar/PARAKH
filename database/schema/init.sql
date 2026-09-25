-- Initial schema creation for RecruitShield / PARAKH
-- (Docker will run this file on initialization if we use the docker-compose setup)

CREATE DATABASE IF NOT EXISTS recruitshield;
USE recruitshield;

CREATE TABLE IF NOT EXISTS offers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    offer_text LONGTEXT NOT NULL,
    company_name VARCHAR(255),
    company_website VARCHAR(500),
    recruiter_email VARCHAR(255),
    received_via VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS risk_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    offer_id BIGINT NOT NULL,
    risk_score INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    summary TEXT,
    red_flags TEXT,
    positive_signals TEXT,
    recommendations TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_risk_reports_offer FOREIGN KEY (offer_id) REFERENCES offers(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS scan_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    offer_text TEXT NOT NULL,
    risk_score INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

