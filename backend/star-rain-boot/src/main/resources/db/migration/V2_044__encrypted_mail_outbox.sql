CREATE TABLE sr_mail_outbox (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  payload_cipher MEDIUMTEXT NOT NULL COMMENT 'AES-GCM密文；密钥通过服务器配置提供',
  attempts INT NOT NULL DEFAULT 0,
  next_attempt_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  expires_at DATETIME(3) NOT NULL,
  lease_token VARCHAR(36) NULL,
  lease_until DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_mail_pending(next_attempt_at,expires_at)
);
