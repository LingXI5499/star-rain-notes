-- Star Rain Notes — V24__account_learner_profile_association.sql
-- 阶段四：learner profile 关联账号型档案（ACCOUNT），保留旧匿名档案（LEGACY_ANONYMOUS）。
-- 「账号型档案必须有 account_id」由服务层保证（账号型档案由 ensureAccountProfile 同时写入），
-- 不建 CHECK——同一列不能同时用于 CHECK 与 FK ON DELETE SET NULL（MySQL 3823）。
ALTER TABLE english_learner_profile
    MODIFY learner_key_hash CHAR(64) NULL,
    ADD COLUMN account_id BIGINT NULL,
    ADD COLUMN profile_type VARCHAR(30) NOT NULL DEFAULT 'LEGACY_ANONYMOUS',
    ADD COLUMN claimed_at DATETIME(6) NULL,
    ADD UNIQUE KEY uk_learner_profile_account (account_id),
    ADD CONSTRAINT fk_learner_profile_account FOREIGN KEY (account_id)
        REFERENCES user_account(id) ON DELETE SET NULL;