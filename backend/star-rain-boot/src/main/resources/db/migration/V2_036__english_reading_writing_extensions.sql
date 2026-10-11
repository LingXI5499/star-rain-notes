-- V2_036__english_reading_writing_extensions.sql [IMPLEMENTATION TEMPLATE]
-- 必须检查本地已存在对象后使用；本轮按逻辑外键，不创建跨 Maven 域物理 FK。
ALTER TABLE sr_english_reading_article
  ADD COLUMN translation_zh_markdown LONGTEXT NULL,
  ADD COLUMN primary_topic_id BIGINT NULL,
  ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0,
  ADD COLUMN content_origin VARCHAR(20) NOT NULL DEFAULT 'EXTERNAL',
  ADD COLUMN level_assessed TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN deleted_at DATETIME(3) NULL;
CREATE INDEX idx_sr_reading_topic_public ON sr_english_reading_article(primary_topic_id,publish_status,id);

CREATE TABLE sr_english_reading_tag (
  article_id BIGINT NOT NULL, term_id BIGINT NOT NULL,
  PRIMARY KEY(article_id,term_id), KEY idx_sr_english_reading_tag_term(term_id,article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sr_english_reading_rights (
  article_id BIGINT NOT NULL PRIMARY KEY,
  source_name VARCHAR(200) NULL, source_url VARCHAR(500) NULL,
  original_author VARCHAR(200) NULL,
  rights_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  rights_basis VARCHAR(1000) NULL, license_notice TEXT NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_sr_english_reading_rights_state(rights_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 不强制每句话/每段都录入。group_key 对应多片段，一个组至少有 EN、ZH 两种片段，由服务层校验。
CREATE TABLE sr_english_reading_alignment (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  article_id BIGINT NOT NULL,
  group_key VARCHAR(64) NOT NULL,
  language VARCHAR(2) NOT NULL,
  paragraph_index INT NOT NULL,
  range_start INT NOT NULL, range_end INT NOT NULL,
  expected_text TEXT NOT NULL,
  paragraph_hash CHAR(64) NOT NULL,
  source_markdown_hash CHAR(64) NOT NULL,
  alignment_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_sr_reading_alignment_group(article_id,group_key),
  KEY idx_sr_reading_alignment_paragraph(article_id,language,paragraph_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sr_english_reading_annotation (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  article_id BIGINT NOT NULL,
  paragraph_index INT NOT NULL,
  range_start INT NOT NULL, range_end INT NOT NULL,
  expected_text TEXT NOT NULL,
  paragraph_hash CHAR(64) NOT NULL,
  source_markdown_hash CHAR(64) NOT NULL,
  analysis_markdown LONGTEXT NOT NULL,
  annotation_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_sr_reading_annotation(article_id,annotation_status,sort_order,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sr_english_reading_vocabulary (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  article_id BIGINT NOT NULL,
  word VARCHAR(200) NOT NULL,
  part_of_speech VARCHAR(30) NOT NULL,
  meaning_zh VARCHAR(500) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_sr_reading_vocabulary(article_id,part_of_speech,sort_order,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- 本文词汇刻意不提供 sr_english_vocabulary_word_id，不允许联到复习系统。

CREATE TABLE sr_english_writing_article (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  owner_account_id BIGINT NOT NULL,
  title VARCHAR(200) NOT NULL DEFAULT 'Untitled Article',
  slug VARCHAR(150) NULL,
  summary VARCHAR(1000) NULL,
  body_markdown LONGTEXT NOT NULL,
  translation_zh_markdown LONGTEXT NULL,
  primary_topic_id BIGINT NULL,
  state VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
  visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE',
  public_eligible TINYINT(1) NOT NULL DEFAULT 0,
  keywords_json JSON NULL,
  row_version BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  published_at DATETIME(3) NULL,
  deleted_at DATETIME(3) NULL,
  UNIQUE KEY uk_sr_writing_article_slug(slug),
  KEY idx_sr_writing_owner_list(owner_account_id,deleted_at,updated_at,id),
  KEY idx_sr_writing_public(visibility,state,published_at,id),
  KEY idx_sr_writing_primary_topic(primary_topic_id,visibility,state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sr_english_writing_article_tag (
  article_id BIGINT NOT NULL, term_id BIGINT NOT NULL,
  PRIMARY KEY(article_id,term_id), KEY idx_sr_writing_tag_term(term_id,article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sr_english_reading_revision (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  article_id BIGINT NOT NULL, revision_no BIGINT NOT NULL,
  snapshot_json JSON NOT NULL, editor_account_id BIGINT NOT NULL,
  change_note VARCHAR(500) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_sr_reading_revision(article_id,revision_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sr_english_writing_revision (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  article_id BIGINT NOT NULL, revision_no BIGINT NOT NULL,
  snapshot_json JSON NOT NULL, editor_account_id BIGINT NOT NULL,
  change_note VARCHAR(500) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_sr_writing_revision(article_id,revision_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- 状态白名单、owner 校验、关联存在性与来源合规均在事务 Service 中强制执行。
-- V2 上线前确认已有公开阅读文章的来源权益状态，再决定旧内容回填/撤回方式。

-- Preserve an explicit unresolved-rights record for every pre-existing article.
INSERT INTO sr_english_reading_rights(article_id,source_name,source_url,rights_status)
SELECT id,source_name,source_url,'PENDING' FROM sr_english_reading_article;
INSERT INTO sr_permission(code,resource,action,name,description,status)
VALUES ('english:content-publish','english','content-publish','发布英语原创内容','只允许发布具备管理权限的作者原创文章','ENABLED');
INSERT IGNORE INTO sr_role_permission(role_id,permission_id)
SELECT r.id,p.id FROM sr_role r CROSS JOIN sr_permission p
WHERE r.code IN ('ADMIN','SUPER_ADMIN') AND p.code='english:content-publish';
