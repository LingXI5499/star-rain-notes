-- ONE-OFF, MANUALLY APPROVED maintenance. NOT a Flyway/startup/import script.
-- Default: unconfigured gates cause zero DELETEs; final ROLLBACK always undoes changes.
-- Fill these values from a reviewed, quiescent inventory IN A SEPARATE local execution copy.
SET @approved_database = NULL;
SET @approval_phrase = NULL;
SET @expected_counts = NULL;
SET @expected_article_versions = NULL;
SET SESSION group_concat_max_len = 1048576;
START TRANSACTION;
-- Lock existing article rows. Stop writes/application before running; dependent tables use logical FKs.
SELECT id, row_version FROM sr_english_reading_article ORDER BY id FOR UPDATE;
SELECT CONCAT_WS(',',
 (SELECT COUNT(*) FROM sr_english_reading_revision),
 (SELECT COUNT(*) FROM sr_english_reading_alignment),
 (SELECT COUNT(*) FROM sr_english_reading_annotation),
 (SELECT COUNT(*) FROM sr_english_reading_vocabulary),
 (SELECT COUNT(*) FROM sr_english_reading_tag),
 (SELECT COUNT(*) FROM sr_english_reading_rights),
 (SELECT COUNT(*) FROM sr_english_reading_article)) INTO @actual_counts;
SELECT COALESCE(GROUP_CONCAT(CONCAT(id,':',row_version) ORDER BY id), '') INTO @actual_article_versions
FROM sr_english_reading_article;
SET @verified_scope = COALESCE(DATABASE()=@approved_database
 AND @approval_phrase=CONCAT('RESET_READING_ONCE:',@approved_database)
 AND @actual_counts=@expected_counts
 AND @actual_article_versions=@expected_article_versions, 0);
SELECT DATABASE() AS actual_database, @verified_scope AS verified_scope,
 @actual_counts AS actual_counts, @actual_article_versions AS actual_article_versions;
-- No statement below can delete rows unless ALL gates matched, even if a client ignores errors.
DELETE FROM sr_english_reading_revision WHERE @verified_scope=1;
DELETE FROM sr_english_reading_alignment WHERE @verified_scope=1;
DELETE FROM sr_english_reading_annotation WHERE @verified_scope=1;
DELETE FROM sr_english_reading_vocabulary WHERE @verified_scope=1;
DELETE FROM sr_english_reading_tag WHERE @verified_scope=1;
DELETE FROM sr_english_reading_rights WHERE @verified_scope=1;
DELETE FROM sr_english_reading_article WHERE @verified_scope=1;
SELECT CONCAT_WS(',',
 (SELECT COUNT(*) FROM sr_english_reading_revision),
 (SELECT COUNT(*) FROM sr_english_reading_alignment),
 (SELECT COUNT(*) FROM sr_english_reading_annotation),
 (SELECT COUNT(*) FROM sr_english_reading_vocabulary),
 (SELECT COUNT(*) FROM sr_english_reading_tag),
 (SELECT COUNT(*) FROM sr_english_reading_rights),
 (SELECT COUNT(*) FROM sr_english_reading_article)) AS remaining_counts;
-- COMMIT is allowed only in a reviewed execution copy, after explicit target-specific authorization.
ROLLBACK;
