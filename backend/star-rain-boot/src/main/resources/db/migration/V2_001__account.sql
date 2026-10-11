-- =====================================================================
-- 星雨笔录 V2.0 · star-rain-account 数据库脚本
-- 目标：支撑 ACC-001 ~ ACC-011
-- 数据库：MySQL 8.x / InnoDB / utf8mb4
-- 关系策略：逻辑外键，不建立物理 FOREIGN KEY
-- 注意：生产环境的首个 SUPER_ADMIN 不写死在 SQL 中，应通过受控 Bootstrap 创建。
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. 账户
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sr_account (
    id              BIGINT NOT NULL AUTO_INCREMENT COMMENT '账户ID',
    username        VARCHAR(50)  NOT NULL COMMENT '用户名',
    email           VARCHAR(128) NOT NULL COMMENT '邮箱',
    display_name    VARCHAR(80)  NOT NULL COMMENT '显示名称',
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                    COMMENT '账户状态：ACTIVE/DISABLED',
    auth_version    INT NOT NULL DEFAULT 1
                    COMMENT '认证版本；密码修改、账户停用等安全事件后递增',
    last_login_at   DATETIME(3) NULL COMMENT '最近一次成功登录时间',
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                    ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_account_username (username),
    UNIQUE KEY uk_sr_account_email (email),
    KEY idx_sr_account_status (status),
    KEY idx_sr_account_created_at (created_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='账户';

-- ---------------------------------------------------------------------
-- 2. 账户凭证
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sr_account_credential (
    id                    BIGINT NOT NULL AUTO_INCREMENT,
    account_id            BIGINT NOT NULL COMMENT '逻辑关联 sr_account.id',
    password_hash         VARCHAR(255) NOT NULL COMMENT '密码哈希；禁止存储明文密码',
    password_changed_at   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_at            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at            DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                          ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_credential_account (account_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='账户认证凭证';

-- ---------------------------------------------------------------------
-- 3. 角色
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sr_role (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    code            VARCHAR(50) NOT NULL COMMENT 'USER/ADMIN/SUPER_ADMIN',
    name            VARCHAR(80) NOT NULL COMMENT '角色名称',
    description     VARCHAR(255) NULL COMMENT '角色说明',
    builtin         TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否系统内置角色',
    status          VARCHAR(20) NOT NULL DEFAULT 'ENABLED'
                    COMMENT 'ENABLED/DISABLED',
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                    ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_role_code (code)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='角色';

-- ---------------------------------------------------------------------
-- 4. 权限
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sr_permission (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    code            VARCHAR(100) NOT NULL COMMENT 'resource:action',
    resource        VARCHAR(50) NOT NULL COMMENT '资源，如 account/tutorial/media',
    action          VARCHAR(50) NOT NULL COMMENT '动作，如 read/update/publish',
    name            VARCHAR(100) NOT NULL COMMENT '权限名称',
    description     VARCHAR(255) NULL COMMENT '权限说明',
    status          VARCHAR(20) NOT NULL DEFAULT 'ENABLED'
                    COMMENT 'ENABLED/DISABLED',
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                    ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_permission_code (code),
    KEY idx_sr_permission_resource (resource)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='权限';

-- ---------------------------------------------------------------------
-- 5. 账户-角色关系
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sr_account_role (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    account_id      BIGINT NOT NULL COMMENT '逻辑关联 sr_account.id',
    role_id         BIGINT NOT NULL COMMENT '逻辑关联 sr_role.id',
    granted_by      BIGINT NULL COMMENT '授权人账户ID；系统初始化可为空',
    granted_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_account_role (account_id, role_id),
    KEY idx_sr_account_role_role (role_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='账户角色关联';

-- ---------------------------------------------------------------------
-- 6. 角色-权限关系
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sr_role_permission (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    role_id         BIGINT NOT NULL COMMENT '逻辑关联 sr_role.id',
    permission_id   BIGINT NOT NULL COMMENT '逻辑关联 sr_permission.id',
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_role_permission (role_id, permission_id),
    KEY idx_sr_role_permission_permission (permission_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='角色权限关联';

-- ---------------------------------------------------------------------
-- 7. 管理员邀请
-- V2 约束：邀请目标绑定现有 USER 账户，避免“任意邮箱即管理员”的歧义。
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sr_admin_invitation (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    target_account_id   BIGINT NOT NULL COMMENT '被邀请账户，逻辑关联 sr_account.id',
    email_snapshot      VARCHAR(128) NOT NULL COMMENT '邀请时邮箱快照',
    target_role_code    VARCHAR(50) NOT NULL DEFAULT 'ADMIN',
    token_hash          CHAR(64) NOT NULL COMMENT '邀请Token的SHA-256摘要',
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                        COMMENT 'PENDING/ACCEPTED/EXPIRED/REVOKED',
    invited_by          BIGINT NOT NULL COMMENT '邀请人账户ID',
    expires_at          DATETIME(3) NOT NULL,
    accepted_by         BIGINT NULL COMMENT '实际接受邀请的账户ID',
    accepted_at         DATETIME(3) NULL,
    revoked_at          DATETIME(3) NULL,
    created_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_admin_invitation_token (token_hash),
    KEY idx_sr_admin_invitation_target_status (target_account_id, status),
    KEY idx_sr_admin_invitation_expires_at (expires_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='管理员邀请';

-- ---------------------------------------------------------------------
-- 8. 密码重置挑战
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sr_password_reset (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    account_id      BIGINT NOT NULL COMMENT '逻辑关联 sr_account.id',
    token_hash      CHAR(64) NOT NULL COMMENT '重置Token的SHA-256摘要',
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    COMMENT 'PENDING/USED/EXPIRED',
    expires_at      DATETIME(3) NOT NULL,
    used_at         DATETIME(3) NULL,
    request_ip      VARCHAR(45) NULL,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_password_reset_token (token_hash),
    KEY idx_sr_password_reset_account_status (account_id, status),
    KEY idx_sr_password_reset_expires_at (expires_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='密码重置挑战';

-- ---------------------------------------------------------------------
-- 9. 账户安全审计
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sr_account_audit (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    actor_account_id    BIGINT NULL COMMENT '操作者账户ID；匿名事件可为空',
    target_account_id   BIGINT NULL COMMENT '目标账户ID',
    action_code         VARCHAR(80) NOT NULL COMMENT '审计动作编码',
    result              VARCHAR(20) NOT NULL COMMENT 'SUCCESS/FAILED',
    detail_json         JSON NULL COMMENT '脱敏后的必要上下文；禁止密码/Token/SessionId',
    ip_address          VARCHAR(45) NULL,
    user_agent          VARCHAR(500) NULL,
    request_id          VARCHAR(64) NULL,
    created_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    KEY idx_sr_account_audit_actor (actor_account_id, created_at),
    KEY idx_sr_account_audit_target (target_account_id, created_at),
    KEY idx_sr_account_audit_action (action_code, created_at),
    KEY idx_sr_account_audit_created_at (created_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_ai_ci
  COMMENT='账户安全审计';

-- =====================================================================
-- 内置角色初始化
-- =====================================================================
INSERT INTO sr_role(code, name, description, builtin, status)
VALUES
('USER', '普通用户', '普通注册用户', 1, 'ENABLED'),
('ADMIN', '管理员', '被授权的教程内容协作管理员', 1, 'ENABLED'),
('SUPER_ADMIN', '超级管理员', '全站最高管理角色', 1, 'ENABLED')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    builtin = VALUES(builtin),
    status = VALUES(status);

-- =====================================================================
-- Account 模块自身的管理权限
-- 自助账户操作（/me、修改本人密码）由“已认证用户”规则保护，
-- 不额外要求 account:* 管理权限。
-- =====================================================================
INSERT INTO sr_permission(code, resource, action, name, description, status)
VALUES
('account:read',         'account', 'read',         '查看账户管理信息', '查看后台账户列表与详情', 'ENABLED'),
('account:disable',      'account', 'disable',      '启用/停用账户',   '控制账户登录状态',       'ENABLED'),
('account:invite-admin', 'account', 'invite-admin', '邀请管理员',       '向可信普通用户发出管理员邀请', 'ENABLED'),
('account:manage-role',  'account', 'manage-role',  '管理角色与权限',   '维护账户角色和角色权限关系', 'ENABLED'),
('account:audit-read',   'account', 'audit-read',   '查看账户审计',     '查看账户和权限安全审计', 'ENABLED')
ON DUPLICATE KEY UPDATE
    resource = VALUES(resource),
    action = VALUES(action),
    name = VALUES(name),
    description = VALUES(description),
    status = VALUES(status);

-- SUPER_ADMIN 获得 Account 模块全部后台管理权限。
INSERT IGNORE INTO sr_role_permission(role_id, permission_id)
SELECT r.id, p.id
FROM sr_role r
JOIN sr_permission p
WHERE r.code = 'SUPER_ADMIN'
  AND p.code IN (
      'account:read',
      'account:disable',
      'account:invite-admin',
      'account:manage-role',
      'account:audit-read'
  );

-- =====================================================================
-- 说明
-- 1. 不在 SQL 中写死首个超级管理员账号与密码。
-- 2. 首个 SUPER_ADMIN 由应用 Bootstrap 流程创建。
-- 3. 其他业务模块（tutorial/media/...）各自登记其 Permission；
--    Account 模块负责认证与授权关系，不替业务模块定义其业务权限。
-- =====================================================================
