-- Flyway migration: create processed_files table
CREATE TABLE IF NOT EXISTS processed_files (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  file_name VARCHAR(255) NOT NULL,
  total_lines BIGINT,
  sha256 VARCHAR(64),
  processed_at TIMESTAMP,
  duration_ms BIGINT
);

