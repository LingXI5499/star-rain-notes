-- Optimistic concurrency for workspace content. Published revisions are immutable.
ALTER TABLE sr_tutorial_chapter ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE sr_tutorial_knowledge_card ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE sr_tutorial_question ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0;
