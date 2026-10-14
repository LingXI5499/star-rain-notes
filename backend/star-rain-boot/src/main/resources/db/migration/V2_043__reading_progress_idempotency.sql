ALTER TABLE sr_learning_progress ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0;
CREATE TABLE sr_learning_progress_event (
  account_id BIGINT NOT NULL,
  event_id VARCHAR(64) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(account_id,event_id),
  KEY idx_progress_event_created(created_at)
) COMMENT='阅读进度重试去重；旧版本请求不得覆盖新位置';
