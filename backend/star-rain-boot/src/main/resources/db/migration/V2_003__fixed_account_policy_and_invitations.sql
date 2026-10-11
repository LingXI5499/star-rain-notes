-- Account 的内置角色权限由迁移管理，不提供在线任意授权入口。
DELETE rp FROM sr_role_permission rp JOIN sr_permission p ON p.id = rp.permission_id
WHERE p.code = 'account:manage-role';
UPDATE sr_permission SET status = 'DISABLED' WHERE code = 'account:manage-role';
INSERT INTO sr_permission(code, resource, action, name, description, status)
VALUES ('account:revoke-admin', 'account', 'revoke-admin', '取消管理员身份', '取消受邀管理员身份并使旧会话失效', 'ENABLED');
DELETE rp FROM sr_role_permission rp
JOIN sr_permission p ON p.id = rp.permission_id JOIN sr_role r ON r.id = rp.role_id
WHERE p.resource = 'account' AND r.code <> 'SUPER_ADMIN';
INSERT IGNORE INTO sr_role_permission(role_id, permission_id)
SELECT r.id, p.id FROM sr_role r CROSS JOIN sr_permission p
WHERE r.code = 'SUPER_ADMIN' AND p.resource = 'account' AND p.status = 'ENABLED';
UPDATE sr_account SET auth_version = auth_version + 1;

ALTER TABLE sr_admin_invitation
    ADD COLUMN last_sent_at DATETIME(3) NULL COMMENT '最近一次提交邮件服务的时间',
    ADD COLUMN mail_message_id VARCHAR(255) NULL COMMENT '邮件追踪标识，不代表送达',
    ADD COLUMN mail_submission_status VARCHAR(30) NOT NULL DEFAULT 'UNKNOWN' COMMENT 'UNKNOWN/SUBMITTED';
