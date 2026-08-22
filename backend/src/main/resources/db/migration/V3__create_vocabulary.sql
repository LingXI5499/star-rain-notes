-- =====================================================================
-- Star Rain Notes — V3__create_vocabulary.sql
-- 英语词汇模块（用户批准的规格变更）：主题 + 词条两张业务表。
-- MySQL 8.0 / InnoDB / utf8mb4 / utf8mb4_0900_ai_ci / DATETIME(3)。
-- =====================================================================

-- ---------------------------------------------------------------
-- vocabulary_theme — 词汇主题（按词层分组，词层由 layer + layer_order 表达）
-- ---------------------------------------------------------------
CREATE TABLE vocabulary_theme (
    id BIGINT NOT NULL AUTO_INCREMENT,
    layer VARCHAR(100) NOT NULL,
    layer_order INT NOT NULL DEFAULT 0,
    name VARCHAR(200) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_vocabulary_theme_layer (layer_order, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------
-- vocabulary_word — 词条；examples 为 JSON 数组（例句，用户自加）；
-- memory_count / last_memory_at 为服务端记忆记录（手动 +1 / 修正）。
-- ---------------------------------------------------------------
CREATE TABLE vocabulary_word (
    id BIGINT NOT NULL AUTO_INCREMENT,
    theme_id BIGINT NOT NULL,
    part_of_speech VARCHAR(50) NOT NULL DEFAULT '',
    word VARCHAR(200) NOT NULL,
    phonetic_us VARCHAR(100) NULL,
    translation VARCHAR(1000) NOT NULL,
    inflections VARCHAR(1000) NULL,
    examples JSON NOT NULL,
    memory_count INT NOT NULL DEFAULT 0,
    last_memory_at DATETIME(3) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_vocabulary_word_theme (theme_id, sort_order),
    CONSTRAINT fk_vocabulary_word_theme FOREIGN KEY (theme_id)
        REFERENCES vocabulary_theme(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
