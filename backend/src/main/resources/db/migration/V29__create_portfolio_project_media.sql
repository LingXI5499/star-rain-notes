-- Portfolio Case Study gallery media (independent of cover_media_id).
CREATE TABLE portfolio_project_media (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    media_asset_id BIGINT NOT NULL,
    title VARCHAR(200) NULL,
    description VARCHAR(1000) NULL,
    alt_text VARCHAR(300) NULL,
    device_type VARCHAR(20) NOT NULL DEFAULT 'DESKTOP',
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_portfolio_media_project (project_id, sort_order, id),
    CONSTRAINT ck_portfolio_media_device CHECK (device_type IN ('DESKTOP', 'MOBILE', 'TABLET')),
    CONSTRAINT fk_portfolio_media_project FOREIGN KEY (project_id)
        REFERENCES portfolio_project(id) ON DELETE CASCADE,
    CONSTRAINT fk_portfolio_media_asset FOREIGN KEY (media_asset_id)
        REFERENCES media_asset(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
