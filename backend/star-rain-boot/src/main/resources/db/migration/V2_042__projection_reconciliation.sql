CREATE TABLE sr_projection_repair (
  projection_name VARCHAR(20) NOT NULL PRIMARY KEY,
  requested_version BIGINT NOT NULL DEFAULT 1,
  completed_version BIGINT NOT NULL DEFAULT 0,
  lease_token VARCHAR(36) NULL,
  lease_until DATETIME(3) NULL,
  next_attempt_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  attempts INT NOT NULL DEFAULT 0,
  last_error VARCHAR(100) NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
) COMMENT='事务内记录的派生数据修复水位';
INSERT INTO sr_projection_repair(projection_name) VALUES ('SEARCH'), ('SEO');
