-- English V2 basic content: vocabulary, grammar, reading, listening, writing.
-- Learning bundles, word families and derived part-of-speech graphs are deliberately absent.
CREATE TABLE sr_english_vocabulary_theme (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    layer VARCHAR(100) NOT NULL,
    layer_order INT NOT NULL DEFAULT 0,
    name VARCHAR(200) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY idx_english_theme_order (layer_order, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sr_english_vocabulary_word (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    theme_id BIGINT NOT NULL,
    word VARCHAR(200) NOT NULL,
    part_of_speech VARCHAR(50) NOT NULL DEFAULT '',
    phonetic_us VARCHAR(100) NULL,
    phonetic_uk VARCHAR(100) NULL,
    translation VARCHAR(1000) NOT NULL,
    examples JSON NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY idx_english_word_theme (theme_id, sort_order, id),
    KEY idx_english_word_lookup (word)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sr_english_grammar_course (
    id BIGINT NOT NULL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    subtitle VARCHAR(300) NULL,
    summary VARCHAR(1000) NULL,
    introduction TEXT NULL,
    roadmap_markdown TEXT NULL,
    publish_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT ck_sr_english_grammar_singleton CHECK (id = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO sr_english_grammar_course(id,title,subtitle,summary,publish_status)
VALUES (1,'英语语法教程','从语言基础到句法分析','按章节学习英语语法。','DRAFT');

CREATE TABLE sr_english_grammar_section (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL DEFAULT 1,
    title VARCHAR(200) NOT NULL,
    sort_order INT NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY idx_sr_english_grammar_section_order (course_id, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sr_english_grammar_lesson (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL DEFAULT 1,
    section_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(150) NOT NULL,
    summary VARCHAR(1000) NULL,
    body_markdown LONGTEXT NOT NULL,
    publish_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    sort_order INT NOT NULL,
    published_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_sr_english_grammar_lesson_slug (slug),
    KEY idx_sr_english_grammar_lesson_order (section_id, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sr_english_reading_article (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(150) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    body_markdown LONGTEXT NOT NULL,
    reading_level TINYINT NOT NULL DEFAULT 1,
    cefr_level VARCHAR(2) NOT NULL DEFAULT 'A1',
    source_name VARCHAR(200) NULL,
    source_url VARCHAR(500) NULL,
    publish_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    sort_order INT NOT NULL DEFAULT 0,
    published_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_sr_english_reading_slug (slug),
    KEY idx_sr_english_reading_public (publish_status, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sr_english_listening_item (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(150) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    transcript_markdown LONGTEXT NULL,
    cefr_level VARCHAR(2) NOT NULL DEFAULT 'A1',
    listening_level TINYINT NOT NULL DEFAULT 1,
    audio_media_id BIGINT NULL,
    duration_seconds INT NOT NULL DEFAULT 0,
    source_name VARCHAR(200) NULL,
    source_url VARCHAR(500) NULL,
    publish_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    sort_order INT NOT NULL DEFAULT 0,
    published_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_sr_english_listening_slug (slug),
    KEY idx_sr_english_listening_public (publish_status, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sr_english_listening_segment (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    listening_item_id BIGINT NOT NULL,
    start_ms INT NOT NULL,
    end_ms INT NOT NULL,
    transcript_text VARCHAR(2000) NOT NULL,
    translation_text VARCHAR(2000) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY idx_sr_english_segment_order (listening_item_id, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sr_english_writing_resource (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    resource_kind VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(150) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    body_markdown LONGTEXT NOT NULL,
    cefr_level VARCHAR(2) NOT NULL DEFAULT 'A1',
    publish_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    sort_order INT NOT NULL DEFAULT 0,
    published_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_sr_english_writing_resource_slug (slug),
    KEY idx_sr_english_writing_resource_public (publish_status, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sr_english_writing_prompt (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(150) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    background_markdown LONGTEXT NOT NULL,
    requirements_markdown LONGTEXT NOT NULL,
    cefr_level VARCHAR(2) NOT NULL DEFAULT 'A1',
    word_min INT NULL,
    word_max INT NULL,
    estimated_minutes INT NULL,
    publish_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    sort_order INT NOT NULL DEFAULT 0,
    published_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_sr_english_writing_prompt_slug (slug),
    KEY idx_sr_english_writing_prompt_public (publish_status, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO sr_permission(code, resource, action, name, description, status) VALUES
('english:content-read-admin', 'english', 'content-read-admin', '查看英语内容管理', '查看五个英语方向的内容', 'ENABLED'),
('english:content-edit', 'english', 'content-edit', '编辑英语内容', '管理五个英语方向的内容', 'ENABLED');

INSERT IGNORE INTO sr_role_permission(role_id, permission_id)
SELECT r.id, p.id FROM sr_role r CROSS JOIN sr_permission p
WHERE r.code IN ('ADMIN', 'SUPER_ADMIN') AND p.code IN ('english:content-read-admin', 'english:content-edit');
