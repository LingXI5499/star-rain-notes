-- 英语写作系统：资源、写作任务、标签和练习关联。独立于阅读/听力内容表。
CREATE TABLE english_writing_resource (
    id BIGINT NOT NULL AUTO_INCREMENT,
    resource_kind VARCHAR(32) NOT NULL,
    expression_level VARCHAR(20) NULL,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(150) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    body_markdown LONGTEXT NOT NULL,
    cover_media_id BIGINT NULL,
    cefr_level VARCHAR(2) NOT NULL,
    word_min INT NULL,
    word_max INT NULL,
    estimated_minutes INT NOT NULL DEFAULT 0,
    template_schema_json JSON NULL,
    publish_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    sort_order INT NOT NULL DEFAULT 10,
    published_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_writing_resource_slug (slug),
    KEY idx_writing_resource_public (resource_kind,publish_status,cefr_level,sort_order,id),
    CONSTRAINT ck_writing_resource_kind CHECK (resource_kind IN ('EXPRESSION_LESSON','GENRE_LESSON','MODEL_ESSAY','TEMPLATE')),
    CONSTRAINT ck_writing_expression_level CHECK (expression_level IS NULL OR expression_level IN ('SENTENCE','PARAGRAPH','COHESION','STYLE')),
    CONSTRAINT ck_writing_resource_words CHECK ((word_min IS NULL OR word_min >= 0) AND (word_max IS NULL OR word_max >= 0) AND (word_min IS NULL OR word_max IS NULL OR word_min <= word_max)),
    CONSTRAINT ck_writing_resource_minutes CHECK (estimated_minutes >= 0 AND sort_order > 0),
    CONSTRAINT ck_writing_resource_lifecycle CHECK ((publish_status='DRAFT' AND published_at IS NULL) OR (publish_status IN ('PUBLISHED','WITHDRAWN') AND published_at IS NOT NULL)),
    CONSTRAINT ck_writing_resource_status CHECK (publish_status IN ('DRAFT','PUBLISHED','WITHDRAWN')),
    CONSTRAINT fk_writing_resource_cover FOREIGN KEY (cover_media_id) REFERENCES media_asset(id) ON DELETE SET NULL,
    CONSTRAINT fk_writing_resource_cefr FOREIGN KEY (cefr_level) REFERENCES english_cefr_standard(level) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE english_writing_resource_tag (
    resource_id BIGINT NOT NULL,
    term_id BIGINT NOT NULL,
    PRIMARY KEY (resource_id,term_id),
    KEY idx_writing_resource_tag_term (term_id,resource_id),
    CONSTRAINT fk_writing_resource_tag_resource FOREIGN KEY (resource_id) REFERENCES english_writing_resource(id) ON DELETE CASCADE,
    CONSTRAINT fk_writing_resource_tag_term FOREIGN KEY (term_id) REFERENCES english_taxonomy_term(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE english_writing_prompt (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(150) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    background_markdown LONGTEXT NOT NULL,
    requirements_markdown LONGTEXT NOT NULL,
    cefr_level VARCHAR(2) NOT NULL,
    word_min INT NOT NULL DEFAULT 0,
    word_max INT NOT NULL DEFAULT 0,
    estimated_minutes INT NOT NULL DEFAULT 0,
    rubric_json JSON NULL,
    checklist_json JSON NULL,
    template_resource_id BIGINT NULL,
    model_resource_id BIGINT NULL,
    cover_media_id BIGINT NULL,
    publish_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    sort_order INT NOT NULL DEFAULT 10,
    published_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_writing_prompt_slug (slug),
    KEY idx_writing_prompt_public (publish_status,cefr_level,sort_order,id),
    CONSTRAINT ck_writing_prompt_words CHECK (word_min >= 0 AND word_max >= word_min AND estimated_minutes >= 0 AND sort_order > 0),
    CONSTRAINT ck_writing_prompt_lifecycle CHECK ((publish_status='DRAFT' AND published_at IS NULL) OR (publish_status IN ('PUBLISHED','WITHDRAWN') AND published_at IS NOT NULL)),
    CONSTRAINT ck_writing_prompt_status CHECK (publish_status IN ('DRAFT','PUBLISHED','WITHDRAWN')),
    CONSTRAINT fk_writing_prompt_cefr FOREIGN KEY (cefr_level) REFERENCES english_cefr_standard(level) ON DELETE RESTRICT,
    CONSTRAINT fk_writing_prompt_template FOREIGN KEY (template_resource_id) REFERENCES english_writing_resource(id) ON DELETE SET NULL,
    CONSTRAINT fk_writing_prompt_model FOREIGN KEY (model_resource_id) REFERENCES english_writing_resource(id) ON DELETE SET NULL,
    CONSTRAINT fk_writing_prompt_cover FOREIGN KEY (cover_media_id) REFERENCES media_asset(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE english_writing_prompt_tag (
    prompt_id BIGINT NOT NULL,
    term_id BIGINT NOT NULL,
    PRIMARY KEY (prompt_id,term_id),
    KEY idx_writing_prompt_tag_term (term_id,prompt_id),
    CONSTRAINT fk_writing_prompt_tag_prompt FOREIGN KEY (prompt_id) REFERENCES english_writing_prompt(id) ON DELETE CASCADE,
    CONSTRAINT fk_writing_prompt_tag_term FOREIGN KEY (term_id) REFERENCES english_taxonomy_term(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE english_writing_prompt_exercise (
    prompt_id BIGINT NOT NULL,
    exercise_id BIGINT NOT NULL,
    PRIMARY KEY (prompt_id,exercise_id),
    KEY idx_writing_prompt_exercise_exercise (exercise_id,prompt_id),
    CONSTRAINT fk_writing_prompt_exercise_prompt FOREIGN KEY (prompt_id) REFERENCES english_writing_prompt(id) ON DELETE CASCADE,
    CONSTRAINT fk_writing_prompt_exercise_exercise FOREIGN KEY (exercise_id) REFERENCES english_exercise(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE english_learning_bundle_writing_item (
    bundle_id BIGINT NOT NULL,
    prompt_id BIGINT NOT NULL,
    sort_order INT NOT NULL DEFAULT 10,
    PRIMARY KEY (bundle_id,prompt_id),
    KEY idx_bundle_writing_prompt (prompt_id,bundle_id),
    CONSTRAINT ck_bundle_writing_sort CHECK (sort_order > 0),
    CONSTRAINT fk_bundle_writing_bundle FOREIGN KEY (bundle_id) REFERENCES english_learning_bundle(id) ON DELETE CASCADE,
    CONSTRAINT fk_bundle_writing_prompt FOREIGN KEY (prompt_id) REFERENCES english_writing_prompt(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
