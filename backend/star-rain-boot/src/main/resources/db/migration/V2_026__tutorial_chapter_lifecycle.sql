UPDATE sr_tutorial_chapter SET status = 'PUBLISHED' WHERE status = 'ACTIVE';
UPDATE sr_tutorial_chapter SET status = 'WITHDRAWN' WHERE status = 'ARCHIVED';
UPDATE sr_tutorial_group SET status = 'ACTIVE' WHERE status = 'ARCHIVED';
ALTER TABLE sr_tutorial_chapter MODIFY status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
    COMMENT 'DRAFT/PUBLISHED/WITHDRAWN';
UPDATE sr_tutorial SET editing_status = 'DRAFT' WHERE editing_status = 'IN_REVIEW';
