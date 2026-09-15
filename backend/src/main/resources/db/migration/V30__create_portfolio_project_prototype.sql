CREATE TABLE portfolio_project_prototype (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    revision VARCHAR(64) NOT NULL,
    entry_path VARCHAR(300) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    source_name VARCHAR(255) NOT NULL,
    file_count INT NOT NULL,
    total_bytes BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_portfolio_prototype_project (project_id),
    UNIQUE KEY uk_portfolio_prototype_revision (revision),
    CONSTRAINT fk_portfolio_prototype_project FOREIGN KEY (project_id)
        REFERENCES portfolio_project(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
