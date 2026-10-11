-- Optional static prototypes are existing Media assets, not a second upload system.
ALTER TABLE sr_portfolio_work ADD COLUMN prototype_asset_id BIGINT NULL, ADD COLUMN prototype_entry VARCHAR(500) NULL;
-- Preserve lifecycle metadata held in the older type-specific JSON contract.
UPDATE sr_portfolio_work w JOIN sr_portfolio_work_detail d ON d.work_id=w.id
SET w.role=JSON_UNQUOTE(JSON_EXTRACT(d.detail_json,'$.role')),
 w.project_status=CASE WHEN JSON_UNQUOTE(JSON_EXTRACT(d.detail_json,'$.projectStage')) IN ('DEVELOPING','COMPLETED','ONLINE')
 THEN JSON_UNQUOTE(JSON_EXTRACT(d.detail_json,'$.projectStage')) ELSE w.project_status END
WHERE JSON_TYPE(JSON_EXTRACT(d.detail_json,'$.role'))='STRING'
 OR JSON_UNQUOTE(JSON_EXTRACT(d.detail_json,'$.projectStage')) IN ('DEVELOPING','COMPLETED','ONLINE');
