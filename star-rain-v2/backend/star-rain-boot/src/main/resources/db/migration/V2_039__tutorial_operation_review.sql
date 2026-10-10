-- Proposals are separate from live tutorial content, including draft creation.
CREATE TABLE sr_tutorial_operation (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    operation VARCHAR(64) NOT NULL,
    arguments_json JSON NOT NULL,
    baseline_json JSON NOT NULL,
    scope_key VARCHAR(100) NOT NULL,
    pending_scope VARCHAR(100) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    display_name VARCHAR(255) NOT NULL,
    applicant_account_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tutorial_operation_pending (pending_scope),
    KEY idx_tutorial_operation_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
