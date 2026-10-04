CREATE TABLE sr_message (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NULL,
    author_display_name VARCHAR(100) NOT NULL,
    contact_email VARCHAR(128) NULL,
    content VARCHAR(3000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    submitted_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    moderated_by_account_id BIGINT NULL,
    moderated_at DATETIME(3) NULL,
    reject_reason VARCHAR(1000) NULL,
    hidden_at DATETIME(3) NULL,
    deleted_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_sr_message_status_submitted (status, submitted_at),
    KEY idx_sr_message_account (account_id, submitted_at),
    KEY idx_sr_message_moderator (moderated_by_account_id, moderated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sr_message_action (
    id BIGINT NOT NULL AUTO_INCREMENT,
    message_id BIGINT NOT NULL,
    action_type VARCHAR(30) NOT NULL,
    actor_account_id BIGINT NULL,
    actor_type VARCHAR(20) NOT NULL,
    note VARCHAR(1000) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_sr_message_action_message (message_id, created_at),
    KEY idx_sr_message_action_actor (actor_account_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO sr_permission(code, resource, action, name, description, status)
VALUES
('message:read-admin', 'message', 'read-admin', '查看留言后台', '读取待审核和历史留言', 'ENABLED'),
('message:moderate', 'message', 'moderate', '审核留言', '通过或拒绝留言', 'ENABLED'),
('message:hide', 'message', 'hide', '管理公开留言', '隐藏或恢复公开留言', 'ENABLED'),
('message:delete', 'message', 'delete', '删除留言', '软删除公开留言', 'ENABLED');

INSERT IGNORE INTO sr_role_permission(role_id, permission_id)
SELECT r.id, p.id FROM sr_role r CROSS JOIN sr_permission p
WHERE r.code = 'SUPER_ADMIN'
  AND p.code IN ('message:read-admin', 'message:moderate', 'message:hide', 'message:delete');
