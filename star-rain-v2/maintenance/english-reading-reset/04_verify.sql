SELECT DATABASE() AS actual_database;
SELECT 'revision' AS reading_table, COUNT(*) AS remaining_count FROM sr_english_reading_revision
UNION ALL SELECT 'alignment', COUNT(*) FROM sr_english_reading_alignment
UNION ALL SELECT 'annotation', COUNT(*) FROM sr_english_reading_annotation
UNION ALL SELECT 'vocabulary', COUNT(*) FROM sr_english_reading_vocabulary
UNION ALL SELECT 'tag', COUNT(*) FROM sr_english_reading_tag
UNION ALL SELECT 'rights', COUNT(*) FROM sr_english_reading_rights
UNION ALL SELECT 'article', COUNT(*) FROM sr_english_reading_article;
SELECT owner_account_id, state, visibility, COUNT(*) AS row_count FROM sr_english_writing_article
GROUP BY owner_account_id, state, visibility;
SELECT article_id, COUNT(*) AS version_count FROM sr_english_writing_revision GROUP BY article_id;
SELECT column_name FROM information_schema.columns WHERE table_schema=DATABASE()
AND table_name='sr_english_reading_article' AND column_name IN ('reading_level','cefr_level','level_assessed');
SELECT table_name FROM information_schema.tables WHERE table_schema=DATABASE()
AND table_name IN ('sr_english_writing_resource','sr_english_writing_prompt');
-- Compare preserved rows/digests with inventory + restored backup, not just counts.
-- With the upgraded backend: GET /api/public/english/content/reading -> total:0;
-- previously listed reading slugs -> 404; knowledge topic reading counts -> 0.
-- Restart twice, add one NEW reading and restart; it must remain present.
