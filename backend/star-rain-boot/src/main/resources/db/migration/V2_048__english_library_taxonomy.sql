-- Additive schema only. Content alignment is an explicit, backed-up maintenance operation.
ALTER TABLE sr_english_taxonomy_term
  ADD COLUMN library_module VARCHAR(20) NULL,
  ADD COLUMN code VARCHAR(20) NULL,
  ADD UNIQUE KEY uk_sr_english_library_code (library_module, code);

CREATE TABLE sr_english_vocabulary_mapping (
  word_id BIGINT NOT NULL PRIMARY KEY,
  entry_id VARCHAR(100) NOT NULL,
  legacy_index INT NOT NULL,
  migration_status VARCHAR(20) NOT NULL,
  migration_reason VARCHAR(500) NOT NULL DEFAULT '',
  row_version BIGINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_sr_english_vocabulary_entry (entry_id),
  KEY idx_sr_english_vocabulary_mapping_status (migration_status, word_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sr_english_vocabulary_topic (
  word_id BIGINT NOT NULL,
  term_id BIGINT NOT NULL,
  PRIMARY KEY (word_id, term_id),
  KEY idx_sr_english_vocabulary_topic_term (term_id, word_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- Logical references retain existing vocabulary IDs and all learning relationships.
