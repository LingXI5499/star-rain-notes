-- Star Rain Notes V1 vocabulary review system.
-- Additive only: existing vocabulary, memory counters and account data remain intact.

ALTER TABLE account_vocabulary_memory
    ADD COLUMN review_step INT NOT NULL DEFAULT 0 AFTER memory_count,
    ADD COLUMN review_count INT NOT NULL DEFAULT 0 AFTER review_step,
    ADD COLUMN first_learned_at DATETIME(6) NULL AFTER last_memory_at,
    ADD COLUMN last_reviewed_at DATETIME(6) NULL AFTER first_learned_at,
    ADD COLUMN next_review_at DATETIME(6) NULL AFTER last_reviewed_at,
    ADD COLUMN learning_status VARCHAR(16) NOT NULL DEFAULT 'NEW' AFTER next_review_at,
    ADD CONSTRAINT ck_acct_vocab_review_step CHECK (review_step >= 0),
    ADD CONSTRAINT ck_acct_vocab_review_count CHECK (review_count >= 0),
    ADD CONSTRAINT ck_acct_vocab_learning_status CHECK (learning_status IN ('NEW','ACTIVE','PAUSED')),
    ADD KEY idx_acct_vocab_due (account_id, learning_status, next_review_at, word_id);

-- Old +1 counters are historical evidence, not fabricated review events. Put them
-- into a one-time recalibration queue while preserving every original value.
UPDATE account_vocabulary_memory
SET learning_status = CASE WHEN memory_count > 0 THEN 'ACTIVE' ELSE 'NEW' END,
    review_step = 0,
    review_count = 0,
    first_learned_at = CASE WHEN memory_count > 0 THEN last_memory_at ELSE NULL END,
    last_reviewed_at = CASE WHEN memory_count > 0 THEN last_memory_at ELSE NULL END,
    next_review_at = CASE WHEN memory_count > 0 THEN UTC_TIMESTAMP(6) ELSE NULL END;

CREATE TABLE account_vocabulary_review_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_id BIGINT NOT NULL,
    word_id BIGINT NOT NULL,
    review_session_id CHAR(36) NOT NULL,
    direction VARCHAR(16) NOT NULL,
    review_number INT NOT NULL,
    scheduled_at DATETIME(6) NULL,
    reviewed_at DATETIME(6) NOT NULL,
    interval_seconds BIGINT NOT NULL,
    timing_status VARCHAR(16) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_vocab_review_session (account_id, review_session_id),
    KEY idx_vocab_review_history (account_id, word_id, reviewed_at, id),
    KEY idx_vocab_review_daily (account_id, reviewed_at, id),
    CONSTRAINT ck_vocab_review_direction CHECK (direction IN ('EN_TO_ZH','ZH_TO_EN')),
    CONSTRAINT ck_vocab_review_number CHECK (review_number > 0),
    CONSTRAINT ck_vocab_review_interval CHECK (interval_seconds > 0),
    CONSTRAINT ck_vocab_review_timing CHECK (timing_status IN ('EARLY','ON_TIME','OVERDUE','NEW')),
    CONSTRAINT fk_vocab_review_account FOREIGN KEY (account_id)
        REFERENCES user_account(id) ON DELETE CASCADE,
    CONSTRAINT fk_vocab_review_word FOREIGN KEY (word_id)
        REFERENCES vocabulary_word(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE account_vocabulary_study_setting (
    account_id BIGINT NOT NULL,
    show_english BOOLEAN NOT NULL DEFAULT TRUE,
    show_chinese BOOLEAN NOT NULL DEFAULT TRUE,
    review_direction VARCHAR(16) NOT NULL DEFAULT 'MIXED',
    daily_new_limit INT NOT NULL DEFAULT 20,
    daily_review_limit INT NOT NULL DEFAULT 200,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (account_id),
    CONSTRAINT ck_vocab_setting_visible CHECK (show_english OR show_chinese),
    CONSTRAINT ck_vocab_setting_direction CHECK (review_direction IN ('EN_TO_ZH','ZH_TO_EN','MIXED')),
    CONSTRAINT ck_vocab_setting_new_limit CHECK (daily_new_limit BETWEEN 0 AND 200),
    CONSTRAINT ck_vocab_setting_review_limit CHECK (daily_review_limit BETWEEN 1 AND 1000),
    CONSTRAINT fk_vocab_setting_account FOREIGN KEY (account_id)
        REFERENCES user_account(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE account_vocabulary_card_preference (
    account_id BIGINT NOT NULL,
    word_id BIGINT NOT NULL,
    display_mode VARCHAR(16) NOT NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (account_id, word_id),
    CONSTRAINT ck_vocab_card_display CHECK (display_mode IN ('BILINGUAL','ENGLISH_ONLY','CHINESE_ONLY')),
    CONSTRAINT fk_vocab_card_pref_account FOREIGN KEY (account_id)
        REFERENCES user_account(id) ON DELETE CASCADE,
    CONSTRAINT fk_vocab_card_pref_word FOREIGN KEY (word_id)
        REFERENCES vocabulary_word(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
