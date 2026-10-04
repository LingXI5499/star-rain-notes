-- =====================================================================
-- 星雨笔录 V2.0 · star-rain-search 数据库脚本
-- 目标：支撑 SEA-001 ~ SEA-005
-- MySQL 8.x；使用 InnoDB FULLTEXT + ngram 作为中文内容初版全文检索实现
-- =====================================================================
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sr_search_document (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    content_type        VARCHAR(30) NOT NULL COMMENT 'TUTORIAL/CHAPTER/BLOG/PORTFOLIO/PROFILE',
    content_id          BIGINT NOT NULL,
    document_key        VARCHAR(120) NOT NULL COMMENT '稳定键，如 BLOG:57',
    title               VARCHAR(255) NOT NULL,
    summary             VARCHAR(1000) NULL,
    searchable_text     LONGTEXT NOT NULL COMMENT '从公开正文提取的纯文本检索内容',
    route_path          VARCHAR(500) NOT NULL,
    published_at        DATETIME(3) NULL,
    source_updated_at   DATETIME(3) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/REMOVED',
    indexed_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_search_document_key (document_key),
    UNIQUE KEY uk_sr_search_content (content_type, content_id),
    KEY idx_sr_search_type_status (content_type, status, published_at),
    KEY idx_sr_search_status_time (status, source_updated_at),
    FULLTEXT KEY ft_sr_search_document (title, summary, searchable_text) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='全站公开内容派生搜索索引';

-- =====================================================================
-- 说明
-- 1. SearchDocument 是派生索引，不是业务内容 source of truth。
-- 2. Withdraw/Delete 通过 status=REMOVED 或重建删除，Public 搜索只查 ACTIVE。
-- 3. 可以从 Tutorial/Blog/Portfolio/Profile 的模块 API 全量 rebuild。
-- =====================================================================

INSERT INTO sr_permission(code, resource, action, name, description, status)
VALUES ('search:rebuild', 'search', 'rebuild', '重建搜索索引', '从公开内容重新生成搜索索引', 'ENABLED');

INSERT IGNORE INTO sr_role_permission(role_id, permission_id)
SELECT r.id, p.id FROM sr_role r CROSS JOIN sr_permission p
WHERE r.code = 'SUPER_ADMIN' AND p.code = 'search:rebuild';
