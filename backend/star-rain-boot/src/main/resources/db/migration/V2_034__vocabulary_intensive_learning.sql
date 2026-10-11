-- Extend the current V2 tables. Legacy counts and unrated logs remain untouched.
ALTER TABLE sr_english_vocabulary_memory
    ADD COLUMN mastery_score SMALLINT NOT NULL DEFAULT 0,
    ADD COLUMN mastery_rank VARCHAR(24) NULL,
    ADD COLUMN first_rated_at DATETIME(3) NULL,
    ADD COLUMN last_rated_at DATETIME(3) NULL,
    ADD COLUMN last_rating VARCHAR(12) NULL,
    ADD COLUMN version INT NOT NULL DEFAULT 0;

ALTER TABLE sr_english_vocabulary_review_log
    ADD COLUMN rating VARCHAR(12) NULL,
    ADD COLUMN source VARCHAR(12) NULL,
    ADD COLUMN plan_revision BIGINT NULL,
    ADD COLUMN audio_played BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN old_step INT NULL,
    ADD COLUMN new_step INT NULL,
    ADD COLUMN old_score DECIMAL(8,4) NULL,
    ADD COLUMN new_score DECIMAL(8,4) NULL,
    ADD COLUMN result_json MEDIUMTEXT NULL,
    ADD KEY idx_sr_vocab_rated_history (account_id, word_id, reviewed_at, id);

CREATE TABLE sr_english_vocabulary_mode_memory (
    account_id BIGINT NOT NULL,
    word_id BIGINT NOT NULL,
    direction VARCHAR(16) NOT NULL,
    rating_count INT NOT NULL DEFAULT 0,
    ema_score DECIMAL(8,4) NOT NULL DEFAULT 0,
    review_step INT NOT NULL DEFAULT -1,
    recent_good_count INT NOT NULL DEFAULT 0,
    last_rating VARCHAR(12) NULL,
    first_rated_at DATETIME(3) NULL,
    last_rated_at DATETIME(3) NULL,
    next_review_at DATETIME(3) NULL,
    audio_verified BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (account_id, word_id, direction),
    KEY idx_sr_vocab_mode_due (account_id, next_review_at, word_id, direction),
    CONSTRAINT chk_sr_vocab_mode_direction CHECK (direction IN ('EN_TO_ZH','ZH_TO_EN','AUDIO_TO_BOTH'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- One row per actual rating day and direction; no fabricated legacy rating days.
CREATE TABLE sr_english_vocabulary_rating_day (
    account_id BIGINT NOT NULL,
    word_id BIGINT NOT NULL,
    rating_day DATE NOT NULL,
    direction VARCHAR(16) NOT NULL,
    PRIMARY KEY (account_id, word_id, rating_day, direction)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Persistent slot doubles as the account command mutex. NONE keeps its revision after deletion.
CREATE TABLE sr_english_vocabulary_plan (
    account_id BIGINT NOT NULL,
    revision BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'NONE',
    name VARCHAR(200) NOT NULL DEFAULT '',
    source_theme_id BIGINT NULL,
    source_name VARCHAR(200) NULL,
    batch_size INT NOT NULL DEFAULT 20,
    total_words INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NULL,
    updated_at DATETIME(3) NULL,
    PRIMARY KEY (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sr_english_vocabulary_plan_item (
    account_id BIGINT NOT NULL,
    word_id BIGINT NOT NULL,
    sort_no INT NOT NULL,
    group_no INT NOT NULL,
    done_mask INT NOT NULL DEFAULT 0,
    started_at DATETIME(3) NULL,
    word_json MEDIUMTEXT NOT NULL,
    PRIMARY KEY (account_id, word_id),
    UNIQUE KEY uk_sr_vocab_plan_order (account_id, sort_no),
    KEY idx_sr_vocab_plan_group (account_id, group_no, sort_no),
    CONSTRAINT chk_sr_vocab_done_mask CHECK (done_mask BETWEEN 0 AND 7)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Legacy due words get one calibration direction with ZERO ratings and ZERO proficiency.
INSERT INTO sr_english_vocabulary_mode_memory (account_id, word_id, direction, next_review_at)
SELECT account_id, word_id, 'EN_TO_ZH', next_review_at
FROM sr_english_vocabulary_memory
WHERE learning_status = 'ACTIVE' AND next_review_at IS NOT NULL;

UPDATE sr_english_vocabulary_study_setting SET review_direction = 'EN_TO_ZH'
WHERE review_direction = 'MIXED';
