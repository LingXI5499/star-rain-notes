-- =====================================================================
-- Star Rain Notes — V12__create_english_listening.sql
-- 英语听力系统 · 阶段3「听力系统」
-- 听力材料 · 时间片段 · 标签关联 · 练习关联 · 学习组合条目 ·
-- 阅读听力配对 · 语音规则。
-- MySQL 8.0 / InnoDB / utf8mb4 / utf8mb4_0900_ai_ci / DATETIME(6)。
-- 本迁移在 V11 基础上追加，不修改任何既有迁移。
-- =====================================================================

-- ---------------------------------------------------------------
-- 1. english_listening_item — 听力材料
-- ---------------------------------------------------------------
CREATE TABLE english_listening_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(150) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    transcript_markdown LONGTEXT NULL,
    cefr_level VARCHAR(2) NOT NULL,
    listening_level TINYINT NOT NULL,
    audio_media_id BIGINT NULL,
    cover_media_id BIGINT NULL,
    duration_seconds INT NOT NULL DEFAULT 0,
    source_name VARCHAR(200) NULL,
    source_url VARCHAR(500) NULL,
    copyright_note VARCHAR(500) NULL,
    publish_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    sort_order INT NOT NULL DEFAULT 0,
    published_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_listening_item_slug (slug),
    KEY idx_listening_item_public (publish_status, cefr_level, updated_at, id),
    KEY idx_listening_item_level (listening_level, publish_status, sort_order, id),
    CONSTRAINT ck_listening_level CHECK (listening_level IN (1, 2, 3)),
    CONSTRAINT ck_listening_publish_status CHECK (publish_status IN ('DRAFT','PUBLISHED','WITHDRAWN')),
    CONSTRAINT ck_listening_lifecycle CHECK (
        (publish_status = 'DRAFT' AND published_at IS NULL)
        OR
        (publish_status IN ('PUBLISHED','WITHDRAWN') AND published_at IS NOT NULL)
    ),
    CONSTRAINT ck_listening_duration CHECK (duration_seconds >= 0),
    CONSTRAINT fk_listening_cefr FOREIGN KEY (cefr_level)
        REFERENCES english_cefr_standard(level) ON DELETE RESTRICT,
    CONSTRAINT fk_listening_audio FOREIGN KEY (audio_media_id)
        REFERENCES media_asset(id) ON DELETE RESTRICT,
    CONSTRAINT fk_listening_cover FOREIGN KEY (cover_media_id)
        REFERENCES media_asset(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------
-- 2. english_listening_segment — 时间片段（逐句回放/高亮/翻译切换）
-- ---------------------------------------------------------------
CREATE TABLE english_listening_segment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    listening_item_id BIGINT NOT NULL,
    start_ms INT NOT NULL,
    end_ms INT NOT NULL,
    transcript_text VARCHAR(2000) NOT NULL,
    translation_text VARCHAR(2000) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_listening_segment_order (listening_item_id, sort_order),
    KEY idx_listening_segment_time (listening_item_id, start_ms, id),
    CONSTRAINT ck_listening_segment_range CHECK (start_ms >= 0 AND end_ms > start_ms),
    CONSTRAINT fk_listening_segment_item FOREIGN KEY (listening_item_id)
        REFERENCES english_listening_item(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------
-- 3. english_listening_item_tag — 材料 ↔ 共享语义标签
-- ---------------------------------------------------------------
CREATE TABLE english_listening_item_tag (
    listening_item_id BIGINT NOT NULL,
    term_id BIGINT NOT NULL,
    tag_role VARCHAR(20) NOT NULL DEFAULT 'TAG',
    PRIMARY KEY (listening_item_id, term_id),
    KEY idx_listening_item_tag_term (term_id, listening_item_id),
    CONSTRAINT ck_listening_item_tag_role CHECK (tag_role IN ('TAG', 'PRIMARY')),
    CONSTRAINT fk_listening_tag_item FOREIGN KEY (listening_item_id)
        REFERENCES english_listening_item(id) ON DELETE CASCADE,
    CONSTRAINT fk_listening_tag_term FOREIGN KEY (term_id)
        REFERENCES english_taxonomy_term(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------
-- 4. english_listening_item_exercise — 材料 ↔ 练习（唯一归属）
-- ---------------------------------------------------------------
CREATE TABLE english_listening_item_exercise (
    listening_item_id BIGINT NOT NULL,
    exercise_id BIGINT NOT NULL,
    PRIMARY KEY (listening_item_id, exercise_id),
    UNIQUE KEY uk_listening_exercise_single_owner (exercise_id),
    CONSTRAINT fk_listening_ex_item FOREIGN KEY (listening_item_id)
        REFERENCES english_listening_item(id) ON DELETE CASCADE,
    CONSTRAINT fk_listening_ex_exercise FOREIGN KEY (exercise_id)
        REFERENCES english_exercise(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------
-- 5. english_learning_bundle_listening_item — 学习组合听力条目
-- ---------------------------------------------------------------
CREATE TABLE english_learning_bundle_listening_item (
    bundle_id BIGINT NOT NULL,
    listening_item_id BIGINT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (bundle_id, listening_item_id),
    KEY idx_bundle_listening_item (listening_item_id, bundle_id),
    CONSTRAINT fk_bundle_listening_bundle FOREIGN KEY (bundle_id)
        REFERENCES english_learning_bundle(id) ON DELETE CASCADE,
    CONSTRAINT fk_bundle_listening_item FOREIGN KEY (listening_item_id)
        REFERENCES english_listening_item(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------
-- 6. english_reading_listening_pair — 阅读 ↔ 听力显式配对
-- ---------------------------------------------------------------
CREATE TABLE english_reading_listening_pair (
    reading_article_id BIGINT NOT NULL,
    listening_item_id BIGINT NOT NULL,
    relation_type VARCHAR(30) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (reading_article_id, listening_item_id),
    KEY idx_reading_listening_pair_listening (listening_item_id, reading_article_id),
    CONSTRAINT ck_reading_listening_pair_type CHECK (
        relation_type IN ('SAME_CONTENT','SAME_TOPIC','EXTENDED_TRAINING')
    ),
    CONSTRAINT fk_reading_listening_pair_reading FOREIGN KEY (reading_article_id)
        REFERENCES english_reading_article(id) ON DELETE CASCADE,
    CONSTRAINT fk_reading_listening_pair_listening FOREIGN KEY (listening_item_id)
        REFERENCES english_listening_item(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------
-- 7. english_listening_pronunciation_rule — 语音规则教学支线
-- ---------------------------------------------------------------
CREATE TABLE english_listening_pronunciation_rule (
    id BIGINT NOT NULL AUTO_INCREMENT,
    rule_type VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(150) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    body_markdown LONGTEXT NOT NULL,
    audio_media_id BIGINT NULL,
    publish_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    sort_order INT NOT NULL DEFAULT 0,
    published_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_pronunciation_rule_slug (slug),
    KEY idx_pronunciation_rule_public (rule_type, publish_status, sort_order, id),
    CONSTRAINT ck_pronunciation_rule_type CHECK (
        rule_type IN ('LINKING','WEAK_FORM','ASSIMILATION','ELISION','STRESS','INTONATION')
    ),
    CONSTRAINT ck_pronunciation_rule_status CHECK (publish_status IN ('DRAFT','PUBLISHED','WITHDRAWN')),
    CONSTRAINT ck_pronunciation_rule_lifecycle CHECK (
        (publish_status = 'DRAFT' AND published_at IS NULL)
        OR
        (publish_status IN ('PUBLISHED','WITHDRAWN') AND published_at IS NOT NULL)
    ),
    CONSTRAINT fk_pronunciation_rule_audio FOREIGN KEY (audio_media_id)
        REFERENCES media_asset(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
