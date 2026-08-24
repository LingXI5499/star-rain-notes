-- Tutorial hierarchy V2: knowledge systems and curriculum groups are flat.
-- Preserve the exact pre-migration hierarchy metadata so an operator can
-- audit or restore every affected row without touching chapter Markdown.
CREATE TABLE IF NOT EXISTS tutorial_node_hierarchy_backup_v5 (
    id BIGINT NOT NULL,
    tutorial_id BIGINT NOT NULL,
    parent_id BIGINT NULL,
    node_type VARCHAR(20) NOT NULL,
    sort_order INT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    backed_up_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO tutorial_node_hierarchy_backup_v5
    (id, tutorial_id, parent_id, node_type, sort_order, created_at, updated_at)
SELECT id, tutorial_id, parent_id, node_type, sort_order, created_at, updated_at
FROM tutorial_node
WHERE (node_type = 'GROUP' AND parent_id IS NOT NULL)
   OR (node_type = 'CHAPTER' AND parent_id IS NULL)
ON DUPLICATE KEY UPDATE
    tutorial_id = VALUES(tutorial_id),
    parent_id = VALUES(parent_id),
    node_type = VALUES(node_type),
    sort_order = VALUES(sort_order),
    created_at = VALUES(created_at),
    updated_at = VALUES(updated_at);

-- Capture the current preorder before flattening. Nested groups are placed
-- immediately after their former parent and keep their sibling order.
CREATE TEMPORARY TABLE v5_group_order (
    id BIGINT NOT NULL PRIMARY KEY,
    target_order INT NOT NULL
);

INSERT INTO v5_group_order (id, target_order)
WITH RECURSIVE group_paths AS (
    SELECT n.id,
           n.tutorial_id,
           CAST(CONCAT(LPAD(COALESCE(n.sort_order, 0), 10, '0'), '-', LPAD(n.id, 20, '0'))
                AS CHAR(4000)) AS sort_path
    FROM tutorial_node n
    WHERE n.node_type = 'GROUP'
      AND n.parent_id IS NULL

    UNION ALL

    SELECT child.id,
           child.tutorial_id,
           CONCAT(parent.sort_path, '/',
                  LPAD(COALESCE(child.sort_order, 0), 10, '0'), '-', LPAD(child.id, 20, '0'))
    FROM tutorial_node child
    JOIN group_paths parent
      ON parent.id = child.parent_id
     AND parent.tutorial_id = child.tutorial_id
    WHERE child.node_type = 'GROUP'
), ranked AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY tutorial_id ORDER BY sort_path, id) * 10 AS target_order
    FROM group_paths
)
SELECT id, target_order
FROM ranked;

UPDATE tutorial_node n
JOIN v5_group_order o ON o.id = n.id
SET n.parent_id = NULL,
    n.sort_order = o.target_order
WHERE n.node_type = 'GROUP';

DROP TEMPORARY TABLE v5_group_order;

-- Knowledge systems are a single ordered level as well.
UPDATE tutorial_category
SET parent_id = NULL
WHERE parent_id IS NOT NULL;

CREATE TEMPORARY TABLE v5_category_order AS
SELECT id,
       ROW_NUMBER() OVER (ORDER BY sort_order, id) * 10 AS target_order
FROM tutorial_category;

UPDATE tutorial_category c
JOIN v5_category_order o ON o.id = c.id
SET c.sort_order = o.target_order;

DROP TEMPORARY TABLE v5_category_order;

ALTER TABLE tutorial_category
    ADD CONSTRAINT ck_tutorial_category_root CHECK (parent_id IS NULL);

ALTER TABLE tutorial_node
    ADD CONSTRAINT ck_tutorial_group_root CHECK (
        node_type <> 'GROUP' OR parent_id IS NULL
    ),
    ADD CONSTRAINT ck_tutorial_chapter_grouped CHECK (
        node_type <> 'CHAPTER' OR parent_id IS NOT NULL
    );
