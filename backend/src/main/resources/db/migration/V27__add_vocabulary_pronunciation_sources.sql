-- Optional British phonetics and licensed/uploaded pronunciation sources.
ALTER TABLE vocabulary_word
    ADD COLUMN phonetic_uk VARCHAR(100) NULL AFTER phonetic_us;

CREATE TABLE vocabulary_word_audio (
    id BIGINT NOT NULL AUTO_INCREMENT,
    word_id BIGINT NOT NULL,
    accent VARCHAR(8) NOT NULL,
    media_asset_id BIGINT NOT NULL,
    provider VARCHAR(40) NOT NULL DEFAULT 'UPLOADED',
    source_url VARCHAR(500) NULL,
    license_note VARCHAR(500) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_vocab_audio_media (word_id, media_asset_id),
    KEY idx_vocab_audio_primary (word_id, accent, is_primary, id),
    CONSTRAINT ck_vocab_audio_accent CHECK (accent IN ('US','UK')),
    CONSTRAINT ck_vocab_audio_provider CHECK (provider IN ('UPLOADED','LICENSED_API','OTHER')),
    CONSTRAINT fk_vocab_audio_word FOREIGN KEY (word_id)
        REFERENCES vocabulary_word(id) ON DELETE CASCADE,
    CONSTRAINT fk_vocab_audio_media FOREIGN KEY (media_asset_id)
        REFERENCES media_asset(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
