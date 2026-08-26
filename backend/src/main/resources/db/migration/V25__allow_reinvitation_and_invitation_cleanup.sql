-- 同一邮箱允许在旧邀请撤回/过期后重新邀请。
-- token 必须唯一；邮箱+状态索引用于快速判断是否已有待处理邀请。
ALTER TABLE admin_invitation
    DROP INDEX uk_invitation_email,
    DROP INDEX idx_invitation_token,
    ADD UNIQUE KEY uk_invitation_token (token_hash),
    ADD KEY idx_invitation_email_status (email, status, created_at);
