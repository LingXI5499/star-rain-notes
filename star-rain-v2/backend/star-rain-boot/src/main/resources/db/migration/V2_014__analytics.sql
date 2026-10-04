-- =====================================================================
-- 星雨笔录 V2.0 · star-rain-analytics 数据库脚本
-- 目标：支撑 ANA-001 ~ ANA-007
-- =====================================================================
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sr_analytics_event (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    event_type          VARCHAR(30) NOT NULL COMMENT 'PAGE_VIEW/CONTENT_VIEW',
    route_key           VARCHAR(500) NOT NULL COMMENT '规范化公开路由，不保存完整敏感 URL 参数',
    content_type        VARCHAR(30) NULL COMMENT 'TUTORIAL/CHAPTER/BLOG/PORTFOLIO/PROFILE/SITE',
    content_id          BIGINT NULL,
    referrer_category   VARCHAR(20) NOT NULL DEFAULT 'DIRECT'
                        COMMENT 'DIRECT/SEARCH/SOCIAL/INTERNAL/OTHER',
    referrer_host       VARCHAR(255) NULL COMMENT '仅保存 host，避免完整 referrer path/query',
    client_key_hash     CHAR(64) NULL COMMENT '可选短期去重哈希，不是用户身份',
    occurred_at         DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_sr_analytics_event_time (occurred_at),
    KEY idx_sr_analytics_event_type_time (event_type, occurred_at),
    KEY idx_sr_analytics_event_content (content_type, content_id, occurred_at),
    KEY idx_sr_analytics_event_referrer (referrer_category, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公共访问原始事件';

CREATE TABLE IF NOT EXISTS sr_analytics_daily_site (
    stat_date           DATE NOT NULL,
    page_view_count     BIGINT NOT NULL DEFAULT 0,
    content_view_count  BIGINT NOT NULL DEFAULT 0,
    generated_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='每日全站访问聚合';

CREATE TABLE IF NOT EXISTS sr_analytics_daily_content (
    stat_date       DATE NOT NULL,
    content_type    VARCHAR(30) NOT NULL,
    content_id      BIGINT NOT NULL,
    view_count      BIGINT NOT NULL DEFAULT 0,
    generated_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                    ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (stat_date, content_type, content_id),
    KEY idx_sr_analytics_content_hot (content_type, view_count, stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='每日内容热度聚合';

CREATE TABLE IF NOT EXISTS sr_analytics_daily_referrer (
    stat_date           DATE NOT NULL,
    referrer_category   VARCHAR(20) NOT NULL,
    view_count          BIGINT NOT NULL DEFAULT 0,
    generated_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (stat_date, referrer_category),
    KEY idx_sr_analytics_referrer_date (stat_date, view_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='每日基础来源聚合';

-- =====================================================================
-- 说明
-- 1. Analytics 不保存明文 IP，不把 client_key_hash 当账户身份。
-- 2. 每日聚合任务应按日期幂等重算，而不是在旧值上无条件累加。
-- 3. 原始事件可配置保留周期；聚合数据可长期保留。
-- =====================================================================

INSERT INTO sr_permission(code, resource, action, name, description, status)
VALUES ('analytics:read', 'analytics', 'read', '查看访问统计', '查看网站访问趋势、内容热度与来源分布', 'ENABLED');

INSERT IGNORE INTO sr_role_permission(role_id, permission_id)
SELECT r.id, p.id FROM sr_role r CROSS JOIN sr_permission p
WHERE r.code = 'SUPER_ADMIN' AND p.code = 'analytics:read';
