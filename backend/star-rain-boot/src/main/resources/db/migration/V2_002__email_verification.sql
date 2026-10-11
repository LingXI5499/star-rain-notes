CREATE TABLE sr_email_verification (
    email VARCHAR(128) NOT NULL COMMENT '规范化的账户邮箱',
    code_hash VARCHAR(100) NULL COMMENT '验证码 BCrypt 哈希，不保存明文',
    expires_at DATETIME(6) NULL,
    consumed_at DATETIME(6) NULL,
    sent_at DATETIME(6) NULL,
    window_started_at DATETIME(6) NULL,
    send_count INT NOT NULL DEFAULT 0,
    failed_attempts INT NOT NULL DEFAULT 0,
    PRIMARY KEY (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='账户邮箱验证码与发送限流';

ALTER TABLE sr_account ADD COLUMN email_verified_at DATETIME(6) NULL COMMENT '邮箱验证完成时间；旧账户保持未验证';
