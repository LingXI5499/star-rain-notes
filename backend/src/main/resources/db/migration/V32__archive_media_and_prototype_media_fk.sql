-- V32: ARCHIVE media type + bind prototypes to media_asset; clear legacy unbound prototypes.

ALTER TABLE media_asset DROP CHECK ck_media_asset_type;
ALTER TABLE media_asset ADD CONSTRAINT ck_media_asset_type
    CHECK (asset_type IN ('IMAGE','DOCUMENT','AUDIO','ARCHIVE'));

-- Legacy rows have no media_asset_id; wipe and require re-upload per product decision.
DELETE FROM portfolio_project_prototype;

ALTER TABLE portfolio_project_prototype
    ADD COLUMN media_asset_id BIGINT NOT NULL AFTER project_id,
    ADD UNIQUE KEY uk_portfolio_prototype_media (media_asset_id),
    ADD CONSTRAINT fk_portfolio_prototype_media
        FOREIGN KEY (media_asset_id) REFERENCES media_asset(id) ON DELETE CASCADE;
