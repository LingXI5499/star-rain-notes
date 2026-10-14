ALTER TABLE sr_blog_post ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0 COMMENT '编辑聚合版本';
ALTER TABLE sr_portfolio_work ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0 COMMENT '编辑聚合版本';
