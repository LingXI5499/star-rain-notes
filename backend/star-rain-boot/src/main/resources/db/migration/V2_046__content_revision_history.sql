CREATE TABLE sr_content_revision (
  content_type VARCHAR(20) NOT NULL,
  content_id BIGINT NOT NULL,
  row_version BIGINT NOT NULL,
  snapshot_json JSON NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(content_type,content_id,row_version)
) COMMENT='编辑前的文章和作品主记录；每条内容保留最近100版本';
