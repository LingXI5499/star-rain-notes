-- =====================================================================
-- 星雨笔录 V2.0 · star-rain-tutorial 数据库脚本
-- 目标：支撑 TUT-001 ~ TUT-014 + LRN-001 ~ LRN-025
-- 数据库：MySQL 8.x / InnoDB / utf8mb4
-- 关系策略：逻辑外键，不建立物理 FOREIGN KEY
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- A. Tutorial Content Domain
-- ---------------------------------------------------------------------

-- V1 的知识体系是教程目录的入口，不改变 Tutorial → Group → Chapter 的课程内部层级。
CREATE TABLE IF NOT EXISTS sr_tutorial_category (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    name            VARCHAR(100) NOT NULL,
    slug            VARCHAR(160) NOT NULL COMMENT '系统生成的稳定公开地址标识',
    sort_order      INT NOT NULL DEFAULT 0,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                    ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_tutorial_category_slug (slug),
    KEY idx_sr_tutorial_category_order (sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='教程知识体系分类';

CREATE TABLE IF NOT EXISTS sr_tutorial (
    id                      BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Tutorial ID',
    category_id             BIGINT NOT NULL COMMENT '逻辑关联 sr_tutorial_category.id',
    slug                    VARCHAR(160) NOT NULL COMMENT '公开稳定路径标识',
    title                   VARCHAR(255) NOT NULL COMMENT '工作区当前标题',
    summary                 VARCHAR(1000) NULL COMMENT '工作区当前摘要',
    sort_order              INT NOT NULL DEFAULT 0,
    cover_media_asset_id    BIGINT NULL COMMENT '封面媒体ID，逻辑关联 MediaAsset',

    publication_status      VARCHAR(30) NOT NULL DEFAULT 'NEVER_PUBLISHED'
                            COMMENT 'NEVER_PUBLISHED/PUBLISHED/WITHDRAWN',
    editing_status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                            COMMENT 'DRAFT/IN_REVIEW',

    published_revision_id   BIGINT NULL COMMENT '当前公开 Revision，逻辑关联 sr_tutorial_revision.id',
    published_at            DATETIME(3) NULL,
    withdrawn_at            DATETIME(3) NULL,

    created_by_account_id   BIGINT NOT NULL COMMENT '创建者账户ID',
    updated_by_account_id   BIGINT NOT NULL COMMENT '最后编辑者账户ID',
    created_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                            ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_tutorial_slug (slug),
    KEY idx_sr_tutorial_category (category_id, sort_order, id),
    KEY idx_sr_tutorial_publication (publication_status, published_at),
    KEY idx_sr_tutorial_editing (editing_status, updated_at),
    KEY idx_sr_tutorial_published_revision (published_revision_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='教程工作区根对象';

CREATE TABLE IF NOT EXISTS sr_tutorial_group (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    tutorial_id     BIGINT NOT NULL COMMENT '逻辑关联 sr_tutorial.id',
    title           VARCHAR(255) NOT NULL,
    description     VARCHAR(1000) NULL,
    sort_order      INT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                    COMMENT 'ACTIVE/ARCHIVED',
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                    ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    KEY idx_sr_tutorial_group_tutorial (tutorial_id, status, sort_order),
    KEY idx_sr_tutorial_group_sort (tutorial_id, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='教程分组';

CREATE TABLE IF NOT EXISTS sr_tutorial_chapter (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    tutorial_id     BIGINT NOT NULL COMMENT '冗余逻辑关联，便于模块内查询',
    group_id        BIGINT NOT NULL COMMENT '逻辑关联 sr_tutorial_group.id',
    slug            VARCHAR(160) NOT NULL COMMENT '教程内稳定章节路径标识',
    title           VARCHAR(255) NOT NULL,
    summary         VARCHAR(1000) NULL,
    body_markdown   LONGTEXT NOT NULL,
    sort_order      INT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                    COMMENT 'ACTIVE/ARCHIVED',
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                    ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_tutorial_chapter_slug (tutorial_id, slug),
    KEY idx_sr_tutorial_chapter_group (group_id, status, sort_order),
    KEY idx_sr_tutorial_chapter_tutorial (tutorial_id, status, group_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='教程章节工作区';

CREATE TABLE IF NOT EXISTS sr_tutorial_knowledge_card (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    chapter_id      BIGINT NOT NULL COMMENT '逻辑关联 sr_tutorial_chapter.id',
    front_text      TEXT NOT NULL,
    back_markdown   LONGTEXT NOT NULL,
    sort_order      INT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'ENABLED'
                    COMMENT 'ENABLED/DISABLED',
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                    ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    KEY idx_sr_tutorial_card_chapter (chapter_id, status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='章节知识卡片';

CREATE TABLE IF NOT EXISTS sr_tutorial_question (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    chapter_id          BIGINT NOT NULL COMMENT '逻辑关联 sr_tutorial_chapter.id',
    question_text       TEXT NOT NULL,
    reference_answer    LONGTEXT NOT NULL,
    sort_order          INT NOT NULL DEFAULT 0,
    status              VARCHAR(20) NOT NULL DEFAULT 'ENABLED'
                        COMMENT 'ENABLED/DISABLED',
    created_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                        ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    KEY idx_sr_tutorial_question_chapter (chapter_id, status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='章节简答题与参考答案';

CREATE TABLE IF NOT EXISTS sr_tutorial_revision (
    id                      BIGINT NOT NULL AUTO_INCREMENT,
    tutorial_id             BIGINT NOT NULL COMMENT '逻辑关联 sr_tutorial.id',
    revision_no             INT NOT NULL,
    revision_ref            VARCHAR(128) NOT NULL COMMENT 'Review 使用的不可变引用',
    snapshot_json           JSON NOT NULL COMMENT 'Tutorial/Group/Chapter/Card/Question 不可变快照',
    content_sha256          CHAR(64) NOT NULL COMMENT '快照内容校验摘要',
    revision_reason         VARCHAR(500) NULL,
    created_by_account_id   BIGINT NOT NULL,
    created_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_tutorial_revision_no (tutorial_id, revision_no),
    UNIQUE KEY uk_sr_tutorial_revision_ref (revision_ref),
    KEY idx_sr_tutorial_revision_created (tutorial_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Tutorial 不可变版本快照';

-- ---------------------------------------------------------------------
-- 教程内容权限：普通 ADMIN 可编辑并提交审核，只有 SUPER_ADMIN 能直接公开与撤回。
INSERT INTO sr_permission(code, resource, action, name, description, status)
VALUES
('tutorial:read-admin', 'tutorial', 'read-admin', '查看教程工作区', '读取教程草稿与预览', 'ENABLED'),
('tutorial:edit', 'tutorial', 'edit', '编辑教程', '维护教程、分组、章节与教学内容', 'ENABLED'),
('tutorial:submit', 'tutorial', 'submit', '提交教程审核', '冻结版本并提交审核', 'ENABLED'),
('tutorial:publish', 'tutorial', 'publish', '直接发布教程', '跳过审核直接公开冻结版本', 'ENABLED'),
('tutorial:withdraw', 'tutorial', 'withdraw', '撤回教程', '撤回或恢复已发布教程', 'ENABLED')
ON DUPLICATE KEY UPDATE resource = VALUES(resource), action = VALUES(action),
    name = VALUES(name), description = VALUES(description), status = VALUES(status);

INSERT IGNORE INTO sr_role_permission(role_id, permission_id)
SELECT r.id, p.id FROM sr_role r JOIN sr_permission p
WHERE r.code IN ('ADMIN', 'SUPER_ADMIN')
  AND p.code IN ('tutorial:read-admin', 'tutorial:edit', 'tutorial:submit');

INSERT IGNORE INTO sr_role_permission(role_id, permission_id)
SELECT r.id, p.id FROM sr_role r JOIN sr_permission p
WHERE r.code = 'SUPER_ADMIN'
  AND p.code IN ('tutorial:publish', 'tutorial:withdraw');
