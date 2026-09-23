SET @scene_meaning_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'vocabulary_word'
      AND column_name = 'scene_meaning'
);
SET @scene_meaning_ddl = IF(
    @scene_meaning_exists = 0,
    'ALTER TABLE vocabulary_word ADD COLUMN scene_meaning VARCHAR(1000) NULL AFTER translation',
    'SELECT 1'
);
PREPARE scene_meaning_stmt FROM @scene_meaning_ddl;
EXECUTE scene_meaning_stmt;
DEALLOCATE PREPARE scene_meaning_stmt;
