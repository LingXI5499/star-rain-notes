-- 星雨笔录 V2.0 · star-rain-profile 数据库脚本
-- 目标：支撑 PRO-001 ~ PRO-007
-- =====================================================================
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sr_profile (
    id                      BIGINT NOT NULL AUTO_INCREMENT,
    profile_key             VARCHAR(50) NOT NULL DEFAULT 'OWNER',
    display_name            VARCHAR(100) NOT NULL,
    headline                VARCHAR(255) NULL,
    bio_markdown            LONGTEXT NULL,
    location_text           VARCHAR(120) NULL,
    avatar_media_asset_id   BIGINT NULL,
    resume_media_asset_id   BIGINT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PUBLIC' COMMENT 'PUBLIC/HIDDEN',
    created_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                            ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_profile_key (profile_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作者公开 Profile';

CREATE TABLE IF NOT EXISTS sr_profile_experience (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    profile_id      BIGINT NOT NULL,
    experience_type VARCHAR(30) NOT NULL COMMENT 'EDUCATION/PROJECT/CAREER/GROWTH/OTHER',
    title           VARCHAR(255) NOT NULL,
    organization    VARCHAR(255) NULL,
    start_date      DATE NULL,
    end_date        DATE NULL,
    is_current      TINYINT(1) NOT NULL DEFAULT 0,
    description_md  LONGTEXT NULL,
    sort_order      INT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                    ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_sr_profile_experience (profile_id, status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作者教育/经历';

CREATE TABLE IF NOT EXISTS sr_profile_skill (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    profile_id      BIGINT NOT NULL,
    category        VARCHAR(30) NOT NULL COMMENT 'SKILL/DIRECTION/INTEREST',
    name            VARCHAR(120) NOT NULL,
    description     VARCHAR(500) NULL,
    proficiency     VARCHAR(30) NULL COMMENT '可选展示文本，不作为认证等级',
    sort_order      INT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                    ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_profile_skill (profile_id, category, name),
    KEY idx_sr_profile_skill_order (profile_id, category, status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作者技能方向兴趣';

CREATE TABLE IF NOT EXISTS sr_profile_social_link (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    profile_id      BIGINT NOT NULL,
    platform_code   VARCHAR(50) NOT NULL,
    label           VARCHAR(100) NOT NULL,
    url             VARCHAR(1000) NOT NULL,
    sort_order      INT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                    ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_profile_social_platform (profile_id, platform_code),
    KEY idx_sr_profile_social_order (profile_id, status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作者外部社交链接';

CREATE TABLE IF NOT EXISTS sr_profile_featured_content (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    profile_id      BIGINT NOT NULL,
    content_type    VARCHAR(30) NOT NULL COMMENT 'TUTORIAL/BLOG/PORTFOLIO',
    content_id      BIGINT NOT NULL,
    title_override  VARCHAR(255) NULL,
    sort_order      INT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                    ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_profile_featured (profile_id, content_type, content_id),
    KEY idx_sr_profile_featured_order (profile_id, status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作者精选内容引用';

-- =====================================================================
-- 说明：Profile 与 Account 分离；avatar/resume 使用 MediaAsset 逻辑引用；
-- Featured Content 只保存逻辑引用，通过各业务模块 API 验证/读取公开摘要。
-- =====================================================================

INSERT INTO sr_profile(profile_key, display_name, status)
VALUES ('OWNER', '星雨笔录', 'PUBLIC');

INSERT INTO sr_permission(code, resource, action, name, description, status)
VALUES
('profile:read-admin', 'profile', 'read-admin', '查看作者资料编辑区', '读取未公开作者资料', 'ENABLED'),
('profile:edit', 'profile', 'edit', '编辑作者资料', '维护介绍、经历、技能和精选内容', 'ENABLED');

INSERT IGNORE INTO sr_role_permission(role_id, permission_id)
SELECT r.id, p.id FROM sr_role r CROSS JOIN sr_permission p
WHERE r.code = 'SUPER_ADMIN' AND p.code IN ('profile:read-admin', 'profile:edit');
