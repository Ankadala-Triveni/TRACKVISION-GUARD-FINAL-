-- ============================================================
-- TRACKVISION GUARD — Database Schema & Initial Data
-- Railway Track Defect Detection and Safety Monitoring System
-- Database: railguard_db
-- ============================================================

CREATE DATABASE IF NOT EXISTS `railguard_db` 
  DEFAULT CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

USE `railguard_db`;

-- ------------------------------------------------------------
-- Table: users
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `users` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `full_name` VARCHAR(100) NOT NULL,
  `username` VARCHAR(50) NOT NULL UNIQUE,
  `email` VARCHAR(100) NOT NULL UNIQUE,
  `password` VARCHAR(255) NOT NULL,
  `role` VARCHAR(30) NOT NULL DEFAULT 'INSPECTOR',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Table: inspections
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `inspections` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `inspection_number` VARCHAR(50) NOT NULL UNIQUE,
  `track_section` VARCHAR(150),
  `railway_zone` VARCHAR(100),
  `division` VARCHAR(100),
  `state` VARCHAR(100),
  `location` VARCHAR(255),
  `latitude` DOUBLE,
  `longitude` DOUBLE,
  `image_path` VARCHAR(500),
  `status` VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
  `risk_level` VARCHAR(20) NOT NULL DEFAULT 'LOW',
  `risk_score` DOUBLE DEFAULT 0.0,
  `primary_defect` VARCHAR(100),
  `recommended_action` VARCHAR(255),
  `model_version` VARCHAR(50) DEFAULT 'YOLO11-RailGuard-v3.0',
  `analysis_error` TEXT,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `inspection_date` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Table: detections
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `detections` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `inspection_id` BIGINT NOT NULL,
  `defect_class` VARCHAR(100) NOT NULL,
  `confidence` DOUBLE NOT NULL,
  `severity` VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
  `x_min` DOUBLE,
  `y_min` DOUBLE,
  `x_max` DOUBLE,
  `y_max` DOUBLE,
  `model_version` VARCHAR(50),
  `detected_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_detections_inspection` FOREIGN KEY (`inspection_id`) REFERENCES `inspections` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Table: maintenance_tasks
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `maintenance_tasks` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `work_order_number` VARCHAR(50) NOT NULL UNIQUE,
  `inspection_id` BIGINT NOT NULL,
  `task_description` TEXT NOT NULL,
  `priority` VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
  `assigned_crew` VARCHAR(100),
  `assigned_engineer` VARCHAR(100),
  `status` VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
  `scheduled_date` DATE,
  `completed_date` DATE,
  `notes` TEXT,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `fk_maintenance_inspection` FOREIGN KEY (`inspection_id`) REFERENCES `inspections` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- Seed Data: Default Admin User (Password: admin123)
-- BCrypt: $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEfOi
-- ------------------------------------------------------------
INSERT INTO `users` (`full_name`, `username`, `email`, `password`, `role`)
VALUES 
('Chief Track Engineer', 'admin', 'admin@railguard.internal', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEfOi', 'ADMIN')
ON DUPLICATE KEY UPDATE `full_name` = VALUES(`full_name`);
