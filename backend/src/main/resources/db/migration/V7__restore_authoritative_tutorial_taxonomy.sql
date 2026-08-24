-- Reconcile the live tutorial taxonomy with tutorial-taxonomy-data.sql while
-- retaining every authored chapter body created after that baseline.

CREATE TABLE IF NOT EXISTS tutorial_category_full_backup_v7 LIKE tutorial_category;
CREATE TABLE IF NOT EXISTS tutorial_full_backup_v7 LIKE tutorial;
CREATE TABLE IF NOT EXISTS tutorial_node_full_backup_v7 LIKE tutorial_node;
CREATE TABLE IF NOT EXISTS tutorial_authored_chapter_backup_v7 LIKE tutorial_node;

INSERT IGNORE INTO tutorial_category_full_backup_v7
SELECT * FROM tutorial_category;

INSERT IGNORE INTO tutorial_full_backup_v7
SELECT * FROM tutorial;

INSERT IGNORE INTO tutorial_node_full_backup_v7
SELECT * FROM tutorial_node;

INSERT IGNORE INTO tutorial_authored_chapter_backup_v7
SELECT *
FROM tutorial_node
WHERE node_type = 'CHAPTER'
  AND body_markdown IS NOT NULL
  AND body_markdown <> ''
  AND NOT REGEXP_LIKE(body_markdown, '^待编写[[:space:]]*$');

-- Restore the two canonical data-structure groups that were removed after
-- the baseline import. SELECT keeps this migration safe on empty test schemas.
INSERT INTO tutorial_node (
    id, tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at, created_at, updated_at
)
SELECT
    464, 19, NULL, 'GROUP', '数组与字符串', NULL, NULL, NULL,
    NULL, 20, NULL, '2026-08-20 16:20:36.772', '2026-08-20 16:20:36.772'
FROM tutorial
WHERE id = 19
  AND NOT EXISTS (SELECT 1 FROM tutorial_node WHERE id = 464);

INSERT INTO tutorial_node (
    id, tutorial_id, parent_id, node_type, title, slug, summary, body_markdown,
    publish_status, sort_order, published_at, created_at, updated_at
)
SELECT
    465, 19, NULL, 'GROUP', '链表', NULL, NULL, NULL,
    NULL, 30, NULL, '2026-08-20 16:20:36.780', '2026-08-20 16:20:36.780'
FROM tutorial
WHERE id = 19
  AND NOT EXISTS (SELECT 1 FROM tutorial_node WHERE id = 465);

-- Keep the three authored data-structure chapters. Attach them to canonical
-- root groups instead of retaining the ad-hoc nested group hierarchy.
UPDATE tutorial_node
SET parent_id = 463, sort_order = 10
WHERE id = 1016 AND tutorial_id = 19 AND node_type = 'CHAPTER';

UPDATE tutorial_node
SET parent_id = 463, sort_order = 20
WHERE id = 1017 AND tutorial_id = 19 AND node_type = 'CHAPTER';

UPDATE tutorial_node
SET parent_id = 464, sort_order = 10
WHERE id = 1021 AND tutorial_id = 19 AND node_type = 'CHAPTER';

-- These three groups were created after the authoritative import. They are
-- empty after the chapter recovery above and are fully recoverable from the
-- v7 backup tables.
DELETE FROM tutorial_node WHERE id IN (1019, 1020);
DELETE FROM tutorial_node WHERE id = 1018;

-- Restore the exact top-level knowledge-system ordering from the supplied SQL.
UPDATE tutorial_category
SET sort_order = CASE id
    WHEN 11 THEN 0
    WHEN 12 THEN 200
    WHEN 13 THEN 300
    WHEN 14 THEN 400
    WHEN 15 THEN 500
    ELSE sort_order
END
WHERE id IN (11, 12, 13, 14, 15);

-- Restore canonical data-structure group ordering.
UPDATE tutorial_node
SET sort_order = CASE id
    WHEN 463 THEN 10
    WHEN 464 THEN 20
    WHEN 465 THEN 30
    WHEN 466 THEN 40
    WHEN 467 THEN 50
    WHEN 468 THEN 60
    WHEN 469 THEN 70
    WHEN 470 THEN 80
    WHEN 471 THEN 90
    WHEN 472 THEN 100
    WHEN 473 THEN 110
    WHEN 474 THEN 120
    WHEN 475 THEN 130
    WHEN 476 THEN 140
    WHEN 477 THEN 150
    WHEN 478 THEN 160
    WHEN 479 THEN 170
    WHEN 480 THEN 180
    WHEN 481 THEN 190
    WHEN 482 THEN 200
    WHEN 483 THEN 210
    WHEN 484 THEN 220
    ELSE sort_order
END
WHERE tutorial_id = 19 AND id BETWEEN 463 AND 484;

-- Restore the remaining ordering differences found against the supplied SQL.
UPDATE tutorial_node
SET sort_order = CASE id
    WHEN 863 THEN 40
    WHEN 864 THEN 50
    WHEN 865 THEN 60
    WHEN 1008 THEN 10
    WHEN 1009 THEN 20
    ELSE sort_order
END
WHERE id IN (863, 864, 865, 1008, 1009);
