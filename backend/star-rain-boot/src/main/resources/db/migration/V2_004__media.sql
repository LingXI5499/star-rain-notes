-- =====================================================================
-- 星雨笔录 V2.0 · star-rain-media 数据库迁移
-- 覆盖：MED-001 ~ MED-007
-- 数据库：MySQL 8.x / InnoDB / utf8mb4
-- 关系策略：逻辑外键，不建立物理 FOREIGN KEY
--
-- 说明：
-- 1. MediaAsset 是媒体业务身份；数据库不保存服务器绝对路径。
-- 2. MediaReference 保存跨模块逻辑引用，是归档保护与反向查询的唯一依据。
-- 3. storage_provider 当前实现为 LOCAL，未来允许 MINIO/OSS/S3。
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. 媒体资产
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sr_media_asset (
    id                      BIGINT NOT NULL AUTO_INCREMENT COMMENT '媒体资产ID',

    original_name           VARCHAR(255) NOT NULL COMMENT '用户上传时的原始文件名',
    media_type              VARCHAR(30) NOT NULL COMMENT 'IMAGE/DOCUMENT/AUDIO/OTHER',
    mime_type               VARCHAR(100) NOT NULL COMMENT 'MIME类型',
    file_extension          VARCHAR(20) NULL COMMENT '规范化扩展名，不含点',

    size_bytes              BIGINT NOT NULL COMMENT '文件大小，字节',
    sha256                  CHAR(64) NOT NULL COMMENT '文件内容SHA-256',

    storage_provider        VARCHAR(30) NOT NULL DEFAULT 'LOCAL'
                            COMMENT 'LOCAL/MINIO/OSS/S3',
    storage_key             VARCHAR(512) NOT NULL
                            COMMENT '存储系统内部Key/相对路径，禁止保存服务器绝对路径',

    width                   INT NULL COMMENT '图片宽度，非图片可为空',
    height                  INT NULL COMMENT '图片高度，非图片可为空',

    access_level            VARCHAR(20) NOT NULL DEFAULT 'PROTECTED'
                            COMMENT 'PUBLIC/PROTECTED',

    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                            COMMENT 'ACTIVE/ARCHIVED',

    uploaded_by_account_id  BIGINT NOT NULL COMMENT '上传者账户ID，逻辑关联 sr_account.id',

    archived_at             DATETIME(3) NULL COMMENT '归档时间',

    created_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                            ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),

    UNIQUE KEY uk_sr_media_storage_object (
        storage_provider,
        storage_key
    ),

    KEY idx_sr_media_asset_sha256 (sha256),
    KEY idx_sr_media_asset_type_status (media_type, status),
    KEY idx_sr_media_asset_access_status (access_level, status),
    KEY idx_sr_media_asset_uploader_created (
        uploaded_by_account_id,
        created_at
    ),
    KEY idx_sr_media_asset_created_at (created_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='媒体资产';

-- ---------------------------------------------------------------------
-- 2. 跨业务模块媒体引用
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sr_media_reference (
    id              BIGINT NOT NULL AUTO_INCREMENT COMMENT '引用ID',

    media_asset_id  BIGINT NOT NULL COMMENT '逻辑关联 sr_media_asset.id',

    source_module   VARCHAR(50) NOT NULL
                    COMMENT '来源模块，如 TUTORIAL/BLOG/PORTFOLIO/PROFILE',
    source_type     VARCHAR(50) NOT NULL
                    COMMENT '业务对象类型，如 CHAPTER/POST/WORK',
    source_id       BIGINT NOT NULL COMMENT '来源业务对象ID',

    usage_code      VARCHAR(100) NOT NULL
                    COMMENT '业务用途，如 blog.cover/tutorial.chapter-content',

    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),

    UNIQUE KEY uk_sr_media_reference_identity (
        media_asset_id,
        source_module,
        source_type,
        source_id,
        usage_code
    ),

    KEY idx_sr_media_reference_asset (
        media_asset_id,
        created_at
    ),

    KEY idx_sr_media_reference_source (
        source_module,
        source_type,
        source_id
    ),

    KEY idx_sr_media_reference_source_usage (
        source_module,
        source_type,
        source_id,
        usage_code
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='媒体跨模块业务引用';

-- =====================================================================
-- Media 模块自身的业务权限
-- 媒体模块只登记自己的权限语义，角色分配由 RBAC 关系完成。
-- =====================================================================
INSERT INTO sr_permission(code, resource, action, name, description, status)
VALUES
('media:read',           'media', 'read',           '查看媒体库',   '查询与浏览媒体资产',           'ENABLED'),
('media:upload',         'media', 'upload',         '上传媒体',     '上传并登记新的媒体资产',       'ENABLED'),
('media:archive',        'media', 'archive',        '归档媒体',     '在无业务引用时归档媒体资产',   'ENABLED'),
('media:restore',        'media', 'restore',        '恢复媒体',     '把已归档媒体恢复为可用状态',   'ENABLED'),
('media:access-manage',  'media', 'access-manage',  '管理访问级别', '调整媒体的 PUBLIC/PROTECTED',  'ENABLED'),
('media:reference-read', 'media', 'reference-read', '查看引用情况', '查看媒体被哪些业务对象引用',   'ENABLED')
ON DUPLICATE KEY UPDATE
    resource = VALUES(resource),
    action = VALUES(action),
    name = VALUES(name),
    description = VALUES(description),
    status = VALUES(status);

-- ADMIN 与 SUPER_ADMIN 取得 Media 模块业务权限。
-- USER 与匿名访问者不取得任何 media:* 权限。
INSERT IGNORE INTO sr_role_permission(role_id, permission_id)
SELECT r.id, p.id
FROM sr_role r
JOIN sr_permission p
WHERE r.code IN ('ADMIN', 'SUPER_ADMIN')
  AND p.code IN (
      'media:read',
      'media:upload',
      'media:archive',
      'media:restore',
      'media:access-manage',
      'media:reference-read'
  );

-- =====================================================================
-- 说明
-- 1. 不建立到 sr_account、sr_blog_post、sr_tutorial_chapter 等表的物理外键。
-- 2. 同一 MediaAsset 可被多个业务对象引用。
-- 3. 存在 sr_media_reference 时禁止归档 MediaAsset。
-- 4. V2 不提供 force delete。
-- 5. 原始文件名允许重复，storage_key 必须唯一。
-- 6. sha256 用于重复检测与完整性检查，不作为 MediaAsset 业务唯一键。
-- =====================================================================
