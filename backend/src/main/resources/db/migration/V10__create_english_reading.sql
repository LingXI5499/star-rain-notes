-- =====================================================================
-- Star Rain Notes — V10__create_english_reading.sql
-- 英语阅读系统 · 阶段2「阅读系统」
-- 阅读文章 · 标签关联 · 练习关联 · 学习组合阅读条目 · 语法课节关联。
-- MySQL 8.0 / InnoDB / utf8mb4 / utf8mb4_0900_ai_ci / DATETIME(6)。
-- 本迁移在 V9 基础上追加，不修改任何既有迁移。
-- =====================================================================

-- ---------------------------------------------------------------
-- 1. english_reading_article — 阅读文章
-- ---------------------------------------------------------------
CREATE TABLE english_reading_article (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(150) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    body_markdown LONGTEXT NOT NULL,
    cover_media_id BIGINT NULL,
    reading_level TINYINT NOT NULL,
    cefr_level VARCHAR(2) NOT NULL,
    source_name VARCHAR(200) NULL,
    source_url VARCHAR(500) NULL,
    copyright_note VARCHAR(500) NULL,
    word_count INT NOT NULL DEFAULT 0,
    unique_word_count INT NOT NULL DEFAULT 0,
    average_sentence_words DECIMAL(6,2) NOT NULL DEFAULT 0,
    max_sentence_words INT NOT NULL DEFAULT 0,
    estimated_minutes INT NOT NULL DEFAULT 0,
    publish_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    sort_order INT NOT NULL DEFAULT 0,
    published_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_reading_article_slug (slug),
    KEY idx_reading_article_public (publish_status, cefr_level, updated_at, id),
    KEY idx_reading_article_level (reading_level, publish_status, sort_order, id),
    CONSTRAINT ck_reading_level CHECK (reading_level IN (1, 2, 3)),
    CONSTRAINT ck_reading_publish_status CHECK (publish_status IN ('DRAFT','PUBLISHED','WITHDRAWN')),
    CONSTRAINT ck_reading_lifecycle CHECK (
        (publish_status = 'DRAFT' AND published_at IS NULL)
        OR
        (publish_status IN ('PUBLISHED','WITHDRAWN') AND published_at IS NOT NULL)
    ),
    CONSTRAINT ck_reading_stats CHECK (
        word_count >= 0 AND unique_word_count >= 0
        AND average_sentence_words >= 0 AND max_sentence_words >= 0
        AND estimated_minutes >= 0
    ),
    CONSTRAINT fk_reading_cover FOREIGN KEY (cover_media_id)
        REFERENCES media_asset(id) ON DELETE SET NULL,
    CONSTRAINT fk_reading_cefr FOREIGN KEY (cefr_level)
        REFERENCES english_cefr_standard(level) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------
-- 2. english_reading_article_tag — 文章 ↔ 共享语义标签（真实 FK）
--    tag_role 允许标记主主题(PRIMARY)或普通标签(TAG)。
-- ---------------------------------------------------------------
CREATE TABLE english_reading_article_tag (
    article_id BIGINT NOT NULL,
    term_id BIGINT NOT NULL,
    tag_role VARCHAR(20) NOT NULL DEFAULT 'TAG',
    PRIMARY KEY (article_id, term_id),
    KEY idx_reading_article_tag_term (term_id, article_id),
    CONSTRAINT ck_reading_article_tag_role CHECK (tag_role IN ('TAG', 'PRIMARY')),
    CONSTRAINT fk_reading_article_tag_article FOREIGN KEY (article_id)
        REFERENCES english_reading_article(id) ON DELETE CASCADE,
    CONSTRAINT fk_reading_article_tag_term FOREIGN KEY (term_id)
        REFERENCES english_taxonomy_term(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------
-- 3. english_reading_article_exercise — 文章 ↔ 练习（联合唯一，禁止重复/跨模块）
-- ---------------------------------------------------------------
CREATE TABLE english_reading_article_exercise (
    article_id BIGINT NOT NULL,
    exercise_id BIGINT NOT NULL,
    PRIMARY KEY (article_id, exercise_id),
    KEY idx_reading_article_exercise_ex (exercise_id, article_id),
    CONSTRAINT fk_reading_article_exercise_article FOREIGN KEY (article_id)
        REFERENCES english_reading_article(id) ON DELETE CASCADE,
    CONSTRAINT fk_reading_article_exercise_exercise FOREIGN KEY (exercise_id)
        REFERENCES english_exercise(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------
-- 4. english_learning_bundle_reading_item — 学习组合 ↔ 阅读文章条目
-- ---------------------------------------------------------------
CREATE TABLE english_learning_bundle_reading_item (
    bundle_id BIGINT NOT NULL,
    article_id BIGINT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (bundle_id, article_id),
    KEY idx_bundle_reading_item_article (article_id, bundle_id),
    CONSTRAINT fk_bundle_reading_item_bundle FOREIGN KEY (bundle_id)
        REFERENCES english_learning_bundle(id) ON DELETE CASCADE,
    CONSTRAINT fk_bundle_reading_item_article FOREIGN KEY (article_id)
        REFERENCES english_reading_article(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------
-- 5. english_reading_article_grammar_lesson — 文章 ↔ 语文法课节关联
--    （独立映射表，不把语法课节伪装成 taxonomy 标签）
-- ---------------------------------------------------------------
CREATE TABLE english_reading_article_grammar_lesson (
    article_id BIGINT NOT NULL,
    lesson_id BIGINT NOT NULL,
    PRIMARY KEY (article_id, lesson_id),
    KEY idx_reading_grammar_lesson (lesson_id, article_id),
    CONSTRAINT fk_reading_grammar_article FOREIGN KEY (article_id)
        REFERENCES english_reading_article(id) ON DELETE CASCADE,
    CONSTRAINT fk_reading_grammar_lesson FOREIGN KEY (lesson_id)
        REFERENCES english_grammar_lesson(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
