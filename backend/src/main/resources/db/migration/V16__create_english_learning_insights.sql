-- Append-only learning attempts make activity trends and review recommendations auditable.
CREATE TABLE english_learning_attempt (
    id BIGINT NOT NULL AUTO_INCREMENT,
    learner_id BIGINT NOT NULL,
    content_type VARCHAR(20) NOT NULL,
    content_id BIGINT NOT NULL,
    content_slug VARCHAR(150) NOT NULL,
    cefr_level VARCHAR(2) NULL,
    completion_status VARCHAR(20) NOT NULL,
    score DECIMAL(5,2) NULL,
    time_spent_seconds INT NOT NULL DEFAULT 0,
    weak_points_json JSON NOT NULL,
    mastery_level DECIMAL(5,4) NULL,
    attempted_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_learning_attempt_learner_time (learner_id,attempted_at),
    KEY idx_learning_attempt_content (learner_id,content_type,content_id,attempted_at),
    CONSTRAINT ck_learning_attempt_type CHECK (content_type IN ('GRAMMAR','READING','LISTENING','WRITING')),
    CONSTRAINT ck_learning_attempt_status CHECK (completion_status IN ('NOT_STARTED','IN_PROGRESS','COMPLETED')),
    CONSTRAINT ck_learning_attempt_score CHECK (score IS NULL OR (score >= 0 AND score <= 100)),
    CONSTRAINT ck_learning_attempt_time CHECK (time_spent_seconds >= 0),
    CONSTRAINT ck_learning_attempt_mastery CHECK (mastery_level IS NULL OR (mastery_level >= 0 AND mastery_level <= 1)),
    CONSTRAINT fk_learning_attempt_learner FOREIGN KEY (learner_id) REFERENCES english_learner_profile(id) ON DELETE CASCADE,
    CONSTRAINT fk_learning_attempt_cefr FOREIGN KEY (cefr_level) REFERENCES english_cefr_standard(level) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Preserve a useful first data point for records created before V16.
INSERT INTO english_learning_attempt
    (learner_id,content_type,content_id,content_slug,cefr_level,completion_status,score,
     time_spent_seconds,weak_points_json,mastery_level,attempted_at)
SELECT learner_id,content_type,content_id,content_slug,cefr_level,completion_status,score,
       time_spent_seconds,weak_points_json,mastery_level,COALESCE(last_attempt_at,updated_at)
FROM english_learning_record;
