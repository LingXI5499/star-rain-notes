ALTER TABLE sr_tutorial_revision
  ADD COLUMN summary_json JSON NULL COMMENT '冻结版本的列表摘要；不含章节正文',
  ADD COLUMN frozen_category_id BIGINT GENERATED ALWAYS AS (CAST(JSON_UNQUOTE(JSON_EXTRACT(snapshot_json,'$.categoryId')) AS UNSIGNED)) STORED,
  ADD COLUMN frozen_title VARCHAR(255) GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(snapshot_json,'$.title'))) STORED,
  ADD KEY idx_revision_public_category(frozen_category_id,id);

UPDATE sr_tutorial_revision r
SET summary_json=JSON_SET(JSON_REMOVE(snapshot_json,'$.groups'),'$.wordCount',
  (SELECT COALESCE(SUM(CHAR_LENGTH(REGEXP_REPLACE(j.body,'[[:space:]#*_`>\\[\\]()]',''))),0)
   FROM JSON_TABLE(r.snapshot_json,'$.groups[*].chapters[*]' COLUMNS(body LONGTEXT PATH '$.bodyMarkdown')) j));
