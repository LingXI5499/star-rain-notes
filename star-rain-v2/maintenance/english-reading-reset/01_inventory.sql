-- Read-only inventory. Run against an explicitly selected database.
SELECT DATABASE() AS target_database, @@hostname AS server_host, @@port AS server_port;
SELECT version, description, checksum, success FROM flyway_schema_history ORDER BY installed_rank;
SELECT 'sr_english_reading_revision' AS table_name, COUNT(*) AS row_count FROM sr_english_reading_revision
UNION ALL SELECT 'sr_english_reading_alignment', COUNT(*) FROM sr_english_reading_alignment
UNION ALL SELECT 'sr_english_reading_annotation', COUNT(*) FROM sr_english_reading_annotation
UNION ALL SELECT 'sr_english_reading_vocabulary', COUNT(*) FROM sr_english_reading_vocabulary
UNION ALL SELECT 'sr_english_reading_tag', COUNT(*) FROM sr_english_reading_tag
UNION ALL SELECT 'sr_english_reading_rights', COUNT(*) FROM sr_english_reading_rights
UNION ALL SELECT 'sr_english_reading_article', COUNT(*) FROM sr_english_reading_article;
SELECT id, slug, title, publish_status, row_version, deleted_at FROM sr_english_reading_article ORDER BY id;
-- Before V2_037 only: both tables are physically absent after the structural migration.
SELECT 'sr_english_writing_resource' AS table_name, COUNT(*) AS row_count FROM sr_english_writing_resource
UNION ALL SELECT 'sr_english_writing_prompt', COUNT(*) FROM sr_english_writing_prompt;
SELECT 'sr_english_writing_article' AS preserved_table, COUNT(*) AS row_count FROM sr_english_writing_article
UNION ALL SELECT 'sr_english_writing_article_tag', COUNT(*) FROM sr_english_writing_article_tag
UNION ALL SELECT 'sr_english_writing_revision', COUNT(*) FROM sr_english_writing_revision
UNION ALL SELECT 'sr_english_taxonomy_term', COUNT(*) FROM sr_english_taxonomy_term
UNION ALL SELECT 'sr_english_vocabulary_word', COUNT(*) FROM sr_english_vocabulary_word
UNION ALL SELECT 'sr_english_grammar_lesson', COUNT(*) FROM sr_english_grammar_lesson;
SELECT owner_account_id, state, visibility, COUNT(*) AS row_count
FROM sr_english_writing_article GROUP BY owner_account_id, state, visibility;
-- Inventory ALL types first; never guess a destructive ENGLISH-wide predicate.
SELECT source_module, source_type, COUNT(*) AS row_count FROM sr_media_reference GROUP BY source_module, source_type;
SELECT content_type, COUNT(*) AS row_count FROM sr_search_document GROUP BY content_type;
SELECT table_name, column_name, referenced_table_name FROM information_schema.key_column_usage
WHERE referenced_table_schema=DATABASE() AND referenced_table_name IN
('sr_english_reading_article','sr_english_writing_resource','sr_english_writing_prompt');
SELECT trigger_name, event_object_table FROM information_schema.triggers WHERE trigger_schema=DATABASE()
AND event_object_table IN ('sr_english_reading_article','sr_english_writing_resource','sr_english_writing_prompt');
