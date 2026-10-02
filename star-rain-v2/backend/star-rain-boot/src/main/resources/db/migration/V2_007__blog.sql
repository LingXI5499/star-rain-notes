-- =====================================================================
-- 星雨笔录 V2.0 · star-rain-blog 数据库迁移
-- 覆盖：BLOG-001 ~ BLOG-010（浏览列表 / 阅读文章 / 创建编辑 Post / 管理 Tag /
--       管理 Topic / Post 加入移出 Topic / 调整 Topic 内顺序 / 发布 /
--       撤回恢复 / 按 Tag·Topic·时间归档浏览）
-- 数据库：MySQL 8.x / InnoDB / utf8mb4
-- 关系策略：逻辑外键，不建立物理 FOREIGN KEY
--
-- 说明：
-- 1. Blog 是公开文章内容域，V2 由 Super Admin 直接发布，不接入 star-rain-review，
--    因此这里没有 review 相关的状态列，也不写 review 权限。
-- 2. Tag 与 Topic 刻意分成两张表：Tag 是多维分类（无序、可多选），
--    Topic 是人工策展的有序专题（有 sort_order）。合并会让“专题排序”无处安放。
-- 3. cover_media_asset_id 只是逻辑关联，必须经 star-rain-blog 的 Service 调用
--    MediaReferenceApi 登记 blog.cover / blog.content 引用后才算成立。
-- 4. Search / SEO / Site 不直接 JOIN 本模块私表，由发布、撤回、恢复事件派生。
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. 博客文章
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sr_blog_post (
    id                      BIGINT NOT NULL AUTO_INCREMENT COMMENT '文章ID',

    slug                    VARCHAR(180) NOT NULL COMMENT 'URL 短名，全局唯一且不随标题变化',
    title                   VARCHAR(255) NOT NULL COMMENT '标题',
    summary                 VARCHAR(1000) NULL COMMENT '摘要，列表与归档展示用',
    body_markdown           LONGTEXT NOT NULL COMMENT 'Markdown 正文，发布前必须非空',

    cover_media_asset_id    BIGINT NULL COMMENT '逻辑关联 sr_media_asset.id，禁止物理外键',

    status                  VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                            COMMENT 'DRAFT/PUBLISHED/WITHDRAWN',

    published_at            DATETIME(3) NULL COMMENT '首次发布时间，恢复时不覆盖，归档按它排序',
    withdrawn_at            DATETIME(3) NULL COMMENT '撤回时间，恢复时清空',

    created_by_account_id   BIGINT NOT NULL COMMENT '创建者账户ID，逻辑关联 sr_account.id',
    updated_by_account_id   BIGINT NOT NULL COMMENT '最后修改者账户ID',

    created_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                            ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),

    UNIQUE KEY uk_sr_blog_post_slug (slug),

    -- 公开列表与归档的主查询路径：WHERE status='PUBLISHED' ORDER BY published_at DESC
    KEY idx_sr_blog_post_status_published (status, published_at),
    KEY idx_sr_blog_post_updated (updated_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='博客文章';

-- ---------------------------------------------------------------------
-- 2. 博客标签（多维分类，无序）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sr_blog_tag (
    id              BIGINT NOT NULL AUTO_INCREMENT COMMENT '标签ID',

    slug            VARCHAR(100) NOT NULL COMMENT '标签短名，全局唯一',
    name            VARCHAR(100) NOT NULL COMMENT '标签名，全局唯一，避免同名不同 ID 造成归档分裂',
    description     VARCHAR(500) NULL COMMENT '标签说明',

    status          VARCHAR(20) NOT NULL DEFAULT 'ENABLED'
                    COMMENT 'ENABLED/DISABLED，DISABLED 不允许新绑定但保留历史关系',

    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                    ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),

    UNIQUE KEY uk_sr_blog_tag_slug (slug),
    UNIQUE KEY uk_sr_blog_tag_name (name),

    KEY idx_sr_blog_tag_status (status, name)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='博客标签';

-- ---------------------------------------------------------------------
-- 3. 文章标签关系
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sr_blog_post_tag (
    id          BIGINT NOT NULL AUTO_INCREMENT COMMENT '关系ID',

    post_id     BIGINT NOT NULL COMMENT '逻辑关联 sr_blog_post.id',
    tag_id      BIGINT NOT NULL COMMENT '逻辑关联 sr_blog_tag.id',

    created_at  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),

    -- 同一文章同一标签只能绑定一次，重复绑定由唯一键兜住而不是靠先查后写
    UNIQUE KEY uk_sr_blog_post_tag (post_id, tag_id),
    KEY idx_sr_blog_post_tag_tag (tag_id, post_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='博客文章标签关系';

-- ---------------------------------------------------------------------
-- 4. 博客专题（人工策展的有序集合）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sr_blog_topic (
    id              BIGINT NOT NULL AUTO_INCREMENT COMMENT '专题ID',

    slug            VARCHAR(120) NOT NULL COMMENT '专题短名，全局唯一',
    name            VARCHAR(160) NOT NULL COMMENT '专题名，允许重名：专题靠 slug 定位，排序靠 ID',
    description     VARCHAR(1000) NULL COMMENT '专题说明',

    status          VARCHAR(20) NOT NULL DEFAULT 'ENABLED'
                    COMMENT 'ENABLED/DISABLED，DISABLED 不在前台展示但保留成员与顺序',

    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                    ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),

    UNIQUE KEY uk_sr_blog_topic_slug (slug),

    KEY idx_sr_blog_topic_status (status, updated_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='博客专题';

-- ---------------------------------------------------------------------
-- 5. 专题文章关系（带人工顺序）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sr_blog_topic_post (
    id              BIGINT NOT NULL AUTO_INCREMENT COMMENT '关系ID',

    topic_id        BIGINT NOT NULL COMMENT '逻辑关联 sr_blog_topic.id',
    post_id         BIGINT NOT NULL COMMENT '逻辑关联 sr_blog_post.id',

    sort_order      INT NOT NULL DEFAULT 0 COMMENT '专题内顺序，从 1 开始连续编号',

    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                    ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),

    UNIQUE KEY uk_sr_blog_topic_post (topic_id, post_id),

    -- 排序查询与“取专题下一个序号”都走这条索引
    KEY idx_sr_blog_topic_post_order (topic_id, sort_order, id),
    KEY idx_sr_blog_topic_post_post (post_id, topic_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='博客专题文章关系';

-- =====================================================================
-- Blog 模块自身的业务权限
-- 权限语义由 Blog 模块声明，角色分配由 RBAC 关系完成。
-- =====================================================================
INSERT INTO sr_permission(code, resource, action, name, description, status)
VALUES
('blog:read-admin',      'blog', 'read-admin',      '查看后台文章', '读取文章列表、详情与预览',       'ENABLED'),
('blog:edit',            'blog', 'edit',            '编辑文章',     '新建、修改正文与删除文章草稿',   'ENABLED'),
('blog:publish',         'blog', 'publish',         '发布文章',     '把草稿或已撤回文章发布为公开内容', 'ENABLED'),
('blog:withdraw',        'blog', 'withdraw',        '撤回文章',     '把已发布文章撤回为不可见',       'ENABLED'),
('blog:taxonomy-manage', 'blog', 'taxonomy-manage', '管理分类与专题', '维护 Tag / Topic 及专题内文章顺序', 'ENABLED')
ON DUPLICATE KEY UPDATE
    resource = VALUES(resource),
    action = VALUES(action),
    name = VALUES(name),
    description = VALUES(description),
    status = VALUES(status);

-- 注意：Blog 权限只授予 SUPER_ADMIN。
-- V2 的博客是个人内容域，ADMIN（教程内容协作管理员）不参与文章发布，
-- 所以这里刻意不写 ADMIN，否则“Admin 编辑博客应被拒绝”的约束会被迁移悄悄破坏。
INSERT IGNORE INTO sr_role_permission(role_id, permission_id)
SELECT r.id, p.id
FROM sr_role r
JOIN sr_permission p
WHERE r.code = 'SUPER_ADMIN'
  AND p.code IN (
      'blog:read-admin',
      'blog:edit',
      'blog:publish',
      'blog:withdraw',
      'blog:taxonomy-manage'
  );

-- =====================================================================
-- 说明
-- 1. 不建立到 sr_account、sr_media_asset、sr_media_reference 的物理外键。
-- 2. 全表 utf8mb4 + DATETIME(3)，与 account / media 保持一致。
-- 3. WITHDRAWN 不出现在公开列表、归档、Search、Sitemap。
-- 4. 物理删除只允许 DRAFT / WITHDRAWN：公开内容必须先撤回，避免 URL 直接 404 丢内容。
-- 5. 删除文章时同一事务内解除 blog.cover / blog.content 引用，并清理 Tag、Topic 关系。
-- =====================================================================
