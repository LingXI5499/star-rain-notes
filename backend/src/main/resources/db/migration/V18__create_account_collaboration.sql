-- =====================================================================
-- Star Rain Notes — V18__create_account_collaboration.sql
-- 账号与协同 · 阶段一「账号与邮箱安全」
-- user_account · admin_invitation · email_verification_challenge。
-- MySQL 8.0 / InnoDB / utf8mb4 / utf8mb4_0900_ai_ci / DATETIME(6)。
-- 仅追加，不修改任何既有迁移；不动摇既有 admin_user 登录路径。
-- =====================================================================

CREATE TABLE user_account (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NULL,
    role VARCHAR(20) NOT NULL,
    account_status VARCHAR(30) NOT NULL DEFAULT 'PENDING_ACTIVATION',
    email_verified_at DATETIME(6) NULL,
    activated_at DATETIME(6) NULL,
    failed_login_count INT NOT NULL DEFAULT 0,
    locked_until DATETIME(6) NULL,
    auth_version INT NOT NULL DEFAULT 1,
    last_login_at DATETIME(6) NULL,
    password_changed_at DATETIME(6) NULL,
    disabled_at DATETIME(6) NULL,
    disabled_by BIGINT NULL,
    disabled_reason VARCHAR(500) NULL,
    is_super TINYINT(1) GENERATED ALWAYS AS (IF(role = 'SUPER_ADMIN', 1, NULL)) STORED,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_account_email (email),
    UNIQUE KEY uk_user_account_is_super (is_super),
    KEY idx_user_account_status (account_status, role),
    CONSTRAINT ck_user_account_role CHECK (role IN ('SUPER_ADMIN','ADMIN')),
    CONSTRAINT ck_user_account_status CHECK (
        account_status IN ('PENDING_ACTIVATION','ACTIVE','DISABLED')
    ),
    CONSTRAINT ck_user_account_password CHECK (
        (account_status = 'PENDING_ACTIVATION' AND password_hash IS NULL)
        OR
        (account_status IN ('ACTIVE','DISABLED') AND password_hash IS NOT NULL)
    ),
    CONSTRAINT ck_user_account_lock CHECK (failed_login_count >= 0),
    CONSTRAINT fk_user_account_disabled_by FOREIGN KEY (disabled_by)
        REFERENCES user_account(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE admin_invitation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(254) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    invited_by BIGINT NOT NULL,
    accepted_account_id BIGINT NULL,
    expires_at DATETIME(6) NOT NULL,
    sent_at DATETIME(6) NULL,
    accepted_at DATETIME(6) NULL,
    revoked_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_invitation_email (email),
    KEY idx_invitation_token (token_hash),
    CONSTRAINT ck_invitation_status CHECK (
        status IN ('PENDING','ACCEPTED','REVOKED','EXPIRED')
    ),
    CONSTRAINT fk_invitation_invited_by FOREIGN KEY (invited_by)
        REFERENCES user_account(id) ON DELETE RESTRICT,
    CONSTRAINT fk_invitation_accepted_account FOREIGN KEY (accepted_account_id)
        REFERENCES user_account(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE email_verification_challenge (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(254) NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    invitation_id BIGINT NULL,
    code_hash CHAR(64) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    expires_at DATETIME(6) NOT NULL,
    consumed_at DATETIME(6) NULL,
    request_ip_hash CHAR(64) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_challenge_email (email, purpose, created_at),
    KEY idx_challenge_invitation (invitation_id),
    CONSTRAINT ck_challenge_purpose CHECK (
        purpose IN ('SUPER_ADMIN_ACTIVATION','ADMIN_REGISTRATION','PASSWORD_RESET')
    ),
    CONSTRAINT ck_challenge_attempts CHECK (attempt_count >= 0),
    CONSTRAINT fk_challenge_invitation FOREIGN KEY (invitation_id)
        REFERENCES admin_invitation(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;