-- =====================================================================
-- 星雨笔录 V2.0 · star-rain-portfolio 数据库脚本
-- 目标：支撑 PORT-001 ~ PORT-010
-- =====================================================================
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sr_portfolio_work (
    id                      BIGINT NOT NULL AUTO_INCREMENT,
    slug                    VARCHAR(180) NOT NULL,
    work_type               VARCHAR(30) NOT NULL COMMENT 'SOFTWARE/VIDEO/MUSIC/WRITING/OTHER',
    title                   VARCHAR(255) NOT NULL,
    summary                 VARCHAR(1000) NULL,
    body_markdown           LONGTEXT NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                            COMMENT 'DRAFT/PUBLISHED/WITHDRAWN',
    published_at            DATETIME(3) NULL,
    withdrawn_at            DATETIME(3) NULL,
    created_by_account_id   BIGINT NOT NULL,
    updated_by_account_id   BIGINT NOT NULL,
    created_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                            ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_portfolio_work_slug (slug),
    KEY idx_sr_portfolio_work_type_status (work_type, status, published_at),
    KEY idx_sr_portfolio_work_status (status, published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作品';

CREATE TABLE IF NOT EXISTS sr_portfolio_work_detail (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    work_id         BIGINT NOT NULL,
    work_type       VARCHAR(30) NOT NULL,
    detail_json     JSON NOT NULL COMMENT '按 workType 校验的类型扩展字段',
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                    ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_portfolio_work_detail (work_id),
    KEY idx_sr_portfolio_work_detail_type (work_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作品类型扩展信息';

CREATE TABLE IF NOT EXISTS sr_portfolio_media (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    work_id         BIGINT NOT NULL,
    media_asset_id  BIGINT NOT NULL,
    usage_type      VARCHAR(30) NOT NULL COMMENT 'COVER/SCREENSHOT/AUDIO/ATTACHMENT',
    caption         VARCHAR(500) NULL,
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                    ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_portfolio_media_identity (work_id, media_asset_id, usage_type),
    KEY idx_sr_portfolio_media_order (work_id, usage_type, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作品媒体编排';

CREATE TABLE IF NOT EXISTS sr_portfolio_link (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    work_id         BIGINT NOT NULL,
    link_type       VARCHAR(30) NOT NULL COMMENT 'GITHUB/DEMO/VIDEO/ARTICLE/OTHER',
    label           VARCHAR(100) NOT NULL,
    url             VARCHAR(1000) NOT NULL,
    sort_order      INT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                    ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_sr_portfolio_link_work (work_id, status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作品外部链接';

-- =====================================================================
-- 说明：逻辑外键；WorkDetail.detail_json 必须由 workType 专属 Validator 校验。
-- sr_portfolio_media 同时调用 MediaReferenceApi 维护跨模块引用。
-- =====================================================================

INSERT INTO sr_permission(code, resource, action, name, description, status)
VALUES
('portfolio:read-admin', 'portfolio', 'read-admin', '查看作品工作区', '读取作品草稿与预览', 'ENABLED'),
('portfolio:edit', 'portfolio', 'edit', '编辑作品', '维护作品、媒体与外链', 'ENABLED'),
('portfolio:publish', 'portfolio', 'publish', '发布作品', '公开作品', 'ENABLED'),
('portfolio:withdraw', 'portfolio', 'withdraw', '撤回作品', '撤回或恢复作品', 'ENABLED');

INSERT IGNORE INTO sr_role_permission(role_id, permission_id)
SELECT r.id, p.id FROM sr_role r CROSS JOIN sr_permission p
WHERE r.code = 'SUPER_ADMIN'
  AND p.code IN ('portfolio:read-admin', 'portfolio:edit', 'portfolio:publish', 'portfolio:withdraw');
