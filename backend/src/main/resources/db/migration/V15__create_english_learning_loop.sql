-- English learning loop: anonymous learner profile, progress records and writing drafts.
CREATE TABLE english_learner_profile (
    id BIGINT NOT NULL AUTO_INCREMENT,
    learner_key_hash CHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_seen_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_english_learner_key_hash (learner_key_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE english_learning_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    learner_id BIGINT NOT NULL,
    content_type VARCHAR(20) NOT NULL,
    content_id BIGINT NOT NULL,
    content_slug VARCHAR(150) NOT NULL,
    cefr_level VARCHAR(2) NULL,
    completion_status VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    score DECIMAL(5,2) NULL,
    time_spent_seconds INT NOT NULL DEFAULT 0,
    attempts INT NOT NULL DEFAULT 0,
    weak_points_json JSON NOT NULL,
    mastery_level DECIMAL(5,4) NULL,
    next_review_at DATETIME(6) NULL,
    last_attempt_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_learning_record_content (learner_id,content_type,content_id),
    KEY idx_learning_record_review (learner_id,next_review_at),
    CONSTRAINT ck_learning_record_type CHECK (content_type IN ('GRAMMAR','READING','LISTENING','WRITING')),
    CONSTRAINT ck_learning_record_status CHECK (completion_status IN ('NOT_STARTED','IN_PROGRESS','COMPLETED')),
    CONSTRAINT ck_learning_record_score CHECK (score IS NULL OR (score >= 0 AND score <= 100)),
    CONSTRAINT ck_learning_record_time CHECK (time_spent_seconds >= 0 AND attempts >= 0),
    CONSTRAINT ck_learning_record_mastery CHECK (mastery_level IS NULL OR (mastery_level >= 0 AND mastery_level <= 1)),
    CONSTRAINT fk_learning_record_learner FOREIGN KEY (learner_id) REFERENCES english_learner_profile(id) ON DELETE CASCADE,
    CONSTRAINT fk_learning_record_cefr FOREIGN KEY (cefr_level) REFERENCES english_cefr_standard(level) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE english_writing_submission (
    id BIGINT NOT NULL AUTO_INCREMENT,
    learner_id BIGINT NOT NULL,
    prompt_id BIGINT NOT NULL,
    body_text LONGTEXT NOT NULL,
    word_count INT NOT NULL DEFAULT 0,
    submission_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    self_score DECIMAL(5,2) NULL,
    submitted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_writing_submission_prompt (learner_id,prompt_id),
    KEY idx_writing_submission_prompt (prompt_id,updated_at),
    CONSTRAINT ck_writing_submission_status CHECK (submission_status IN ('DRAFT','SUBMITTED')),
    CONSTRAINT ck_writing_submission_words CHECK (word_count >= 0),
    CONSTRAINT ck_writing_submission_score CHECK (self_score IS NULL OR (self_score >= 0 AND self_score <= 100)),
    CONSTRAINT ck_writing_submission_lifecycle CHECK (
        (submission_status='DRAFT' AND submitted_at IS NULL)
        OR (submission_status='SUBMITTED' AND submitted_at IS NOT NULL)
    ),
    CONSTRAINT fk_writing_submission_learner FOREIGN KEY (learner_id) REFERENCES english_learner_profile(id) ON DELETE CASCADE,
    CONSTRAINT fk_writing_submission_prompt FOREIGN KEY (prompt_id) REFERENCES english_writing_prompt(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
