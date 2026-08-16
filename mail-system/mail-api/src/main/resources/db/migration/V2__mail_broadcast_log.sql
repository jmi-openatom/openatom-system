-- Broadcast send history (manual admin sends and automatic main-site broadcasts).
CREATE TABLE IF NOT EXISTS mail_broadcast_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  source VARCHAR(16) NOT NULL COMMENT 'manual | auto',
  kind VARCHAR(32) NOT NULL DEFAULT '' COMMENT 'notification | activity | recruitment | approval | interview | manual',
  subject VARCHAR(255) NOT NULL DEFAULT '',
  sender VARCHAR(255) NOT NULL DEFAULT '',
  recipients INT NOT NULL DEFAULT 0,
  batches INT NOT NULL DEFAULT 0,
  message_ids TEXT COMMENT 'comma-separated Resend message ids',
  status VARCHAR(16) NOT NULL COMMENT 'sent | failed',
  error VARCHAR(500) DEFAULT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_broadcast_log_created (created_at)
);
