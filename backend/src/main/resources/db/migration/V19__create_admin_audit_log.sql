-- Star Rain Notes — V19__create_admin_audit_log.sql
-- 账号与协同 · 审计日志（追加式，只记录 event，不记密码/验证码/Markdown 正文）。
CREATE TABLE admin_audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_id BIGINT NULL,
    action VARCHAR(50) NOT NULL,
    target_type VARCHAR(50) NULL,
    target_id BIGINT NULL,
    result VARCHAR(20) NOT NULL,
    request_ip_hash CHAR(64) NULL,
    user_agent_summary VARCHAR(200) NULL,
    metadata_json JSON NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_audit_actor (actor_id, created_at),
    KEY idx_audit_action (action, created_at),
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES user_account(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;