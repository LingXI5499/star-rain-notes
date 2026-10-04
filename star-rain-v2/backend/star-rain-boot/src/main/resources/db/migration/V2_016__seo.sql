-- =====================================================================
-- 星雨笔录 V2.0 · star-rain-seo 数据库脚本
-- 目标：支撑 SEO-001 ~ SEO-006
-- =====================================================================
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sr_seo_page_snapshot (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    route_path          VARCHAR(500) NOT NULL,
    content_type        VARCHAR(30) NOT NULL COMMENT 'SITE/TUTORIAL/CHAPTER/BLOG/PORTFOLIO/PROFILE',
    content_id          BIGINT NULL,
    canonical_url       VARCHAR(1000) NOT NULL,
    title               VARCHAR(255) NOT NULL,
    description         VARCHAR(1000) NULL,
    robots_directive    VARCHAR(100) NOT NULL DEFAULT 'index,follow',
    structured_data_json JSON NULL,
    html_snapshot       LONGTEXT NOT NULL COMMENT '与公开页面语义一致的可索引 HTML 快照',
    source_version_ref  VARCHAR(128) NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/REMOVED',
    generated_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_seo_page_route (route_path),
    KEY idx_sr_seo_page_status (status, updated_at),
    KEY idx_sr_seo_page_content (content_type, content_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公开页面 SEO 派生快照';

CREATE TABLE IF NOT EXISTS sr_seo_notification_log (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    provider_code       VARCHAR(50) NOT NULL COMMENT '如 INDEXNOW；具体 Provider 由配置启用',
    route_path          VARCHAR(500) NOT NULL,
    canonical_url       VARCHAR(1000) NOT NULL,
    change_type         VARCHAR(20) NOT NULL COMMENT 'UPSERT/DELETE',
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/SUCCESS/FAILED',
    attempt_count       INT NOT NULL DEFAULT 0,
    http_status         INT NULL,
    last_error          VARCHAR(2000) NULL,
    next_retry_at       DATETIME(3) NULL,
    last_attempt_at     DATETIME(3) NULL,
    created_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_sr_seo_notify_pending (status, next_retry_at),
    KEY idx_sr_seo_notify_route (route_path, created_at),
    KEY idx_sr_seo_notify_provider (provider_code, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='搜索引擎变化通知日志';

-- =====================================================================
-- 说明
-- 1. SEO Snapshot 是派生数据，可从公开业务模块重新生成。
-- 2. 通知失败不回滚核心内容发布。
-- 3. Sitemap 只读取 ACTIVE snapshot / public sources，不单独建立 sitemap 事实表。
-- =====================================================================

INSERT INTO sr_permission(code, resource, action, name, description, status)
VALUES ('seo:rebuild', 'seo', 'rebuild', '重建 SEO 快照', '从公开内容重建 SEO 快照', 'ENABLED');

INSERT IGNORE INTO sr_role_permission(role_id, permission_id)
SELECT r.id, p.id FROM sr_role r CROSS JOIN sr_permission p
WHERE r.code = 'SUPER_ADMIN' AND p.code = 'seo:rebuild';
