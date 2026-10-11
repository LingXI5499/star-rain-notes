ALTER TABLE sr_blog_topic
    ADD COLUMN sort_order INT NOT NULL DEFAULT 0 COMMENT '博客导航人工顺序' AFTER description,
    ADD COLUMN featured TINYINT(1) NOT NULL DEFAULT 0 COMMENT '优先显示在博客导航' AFTER sort_order;

UPDATE sr_blog_topic SET sort_order = id * 10;
UPDATE sr_blog_topic SET featured = 1 WHERE id IN (
    SELECT id FROM (SELECT id FROM sr_blog_topic WHERE status = 'ENABLED' ORDER BY id LIMIT 6) initial_topics
);
CREATE INDEX idx_sr_blog_topic_navigation ON sr_blog_topic (status, featured, sort_order, id);
