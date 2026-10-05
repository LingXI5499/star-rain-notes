-- One-time, repeatable transfer of V1 public portfolio and author content.
-- Only missing works/relations are inserted. Existing V2 author text is filled
-- from V1 when the V2 field is still empty or contains the initial seed name.
START TRANSACTION;

SET @owner_id = (SELECT id FROM {{TARGET}}.sr_account WHERE username = 'Lingxi' LIMIT 1);

INSERT IGNORE INTO {{TARGET}}.sr_portfolio_work
    (slug, work_type, title, summary, body_markdown, status, published_at,
     created_by_account_id, updated_by_account_id, created_at, updated_at)
SELECT p.slug, 'SOFTWARE', p.title, p.summary, p.body_markdown,
       CASE WHEN p.publish_status = 'PUBLISHED' THEN 'PUBLISHED' ELSE 'DRAFT' END,
       p.published_at, @owner_id, @owner_id, p.created_at, p.updated_at
FROM {{SOURCE}}.portfolio_project p
WHERE p.publish_status = 'PUBLISHED' AND @owner_id IS NOT NULL;

INSERT IGNORE INTO {{TARGET}}.sr_portfolio_work_detail
    (work_id, work_type, detail_json, created_at, updated_at)
SELECT w.id, 'SOFTWARE',
       JSON_OBJECT('techStack', JSON_EXTRACT(p.tech_stack, '$'),
                   'role', COALESCE(NULLIF(p.role, ''), '开发者'),
                   'projectStage', p.project_status),
       p.created_at, p.updated_at
FROM {{SOURCE}}.portfolio_project p
JOIN {{TARGET}}.sr_portfolio_work w ON w.slug = p.slug
WHERE p.publish_status = 'PUBLISHED';

INSERT INTO {{TARGET}}.sr_portfolio_link
    (work_id, link_type, label, url, sort_order, status, created_at, updated_at)
SELECT w.id, 'GITHUB', '查看代码', p.repository_url, 0, 'ENABLED', p.created_at, p.updated_at
FROM {{SOURCE}}.portfolio_project p
JOIN {{TARGET}}.sr_portfolio_work w ON w.slug = p.slug
WHERE p.publish_status = 'PUBLISHED' AND NULLIF(p.repository_url, '') IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM {{TARGET}}.sr_portfolio_link l
                  WHERE l.work_id = w.id AND l.link_type = 'GITHUB' AND l.url = p.repository_url);

INSERT INTO {{TARGET}}.sr_portfolio_link
    (work_id, link_type, label, url, sort_order, status, created_at, updated_at)
SELECT w.id, 'DEMO', '访问作品', p.demo_url, 1, 'ENABLED', p.created_at, p.updated_at
FROM {{SOURCE}}.portfolio_project p
JOIN {{TARGET}}.sr_portfolio_work w ON w.slug = p.slug
WHERE p.publish_status = 'PUBLISHED' AND NULLIF(p.demo_url, '') IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM {{TARGET}}.sr_portfolio_link l
                  WHERE l.work_id = w.id AND l.link_type = 'DEMO' AND l.url = p.demo_url);

UPDATE {{TARGET}}.sr_profile dst
JOIN {{SOURCE}}.profile src ON src.id = 1
SET dst.display_name = CASE WHEN dst.display_name = '星雨笔录' THEN src.display_name ELSE dst.display_name END,
    dst.headline = COALESCE(NULLIF(dst.headline, ''), src.headline),
    dst.bio_markdown = COALESCE(NULLIF(dst.bio_markdown, ''),
        CONCAT(COALESCE(src.bio, ''), '\n\n## 技术方向\n\n',
               COALESCE(src.technical_direction_markdown, ''),
               '\n\n## 学习与实践\n\n', COALESCE(src.journey_markdown, ''))),
    dst.updated_at = CURRENT_TIMESTAMP(3)
WHERE dst.profile_key = 'OWNER';

INSERT IGNORE INTO {{TARGET}}.sr_profile_skill
    (profile_id, category, name, sort_order, status)
SELECT dst.id, 'DIRECTION', focus.name, focus.sort_order, 'ENABLED'
FROM {{SOURCE}}.profile src
JOIN {{TARGET}}.sr_profile dst ON dst.profile_key = 'OWNER'
JOIN JSON_TABLE(src.current_focus, '$[*]'
     COLUMNS (sort_order FOR ORDINALITY, name VARCHAR(120) PATH '$')) focus
WHERE src.id = 1 AND NULLIF(focus.name, '') IS NOT NULL;

INSERT IGNORE INTO {{TARGET}}.sr_profile_social_link
    (profile_id, platform_code, label, url, sort_order, status)
SELECT dst.id, 'GITHUB', 'GitHub', src.github_url, 0, 'ENABLED'
FROM {{SOURCE}}.profile src JOIN {{TARGET}}.sr_profile dst ON dst.profile_key = 'OWNER'
WHERE src.id = 1 AND NULLIF(src.github_url, '') IS NOT NULL;

INSERT IGNORE INTO {{TARGET}}.sr_profile_social_link
    (profile_id, platform_code, label, url, sort_order, status)
SELECT dst.id, 'EMAIL_PAGE', '邮箱', CONCAT('mailto:', src.public_email), 1, 'ENABLED'
FROM {{SOURCE}}.profile src JOIN {{TARGET}}.sr_profile dst ON dst.profile_key = 'OWNER'
WHERE src.id = 1 AND NULLIF(src.public_email, '') IS NOT NULL;

INSERT IGNORE INTO {{TARGET}}.sr_profile_featured_content
    (profile_id, content_type, content_id, sort_order, status)
SELECT dst.id, 'PORTFOLIO', w.id, ROW_NUMBER() OVER (ORDER BY p.sort_order, p.id), 'ENABLED'
FROM {{SOURCE}}.portfolio_project p
JOIN {{TARGET}}.sr_portfolio_work w ON w.slug = p.slug
JOIN {{TARGET}}.sr_profile dst ON dst.profile_key = 'OWNER'
WHERE p.publish_status = 'PUBLISHED' AND p.featured = 1;

COMMIT;

SELECT 'portfolio_works' AS metric, COUNT(*) AS total FROM {{TARGET}}.sr_portfolio_work
UNION ALL SELECT 'portfolio_details', COUNT(*) FROM {{TARGET}}.sr_portfolio_work_detail
UNION ALL SELECT 'portfolio_links', COUNT(*) FROM {{TARGET}}.sr_portfolio_link
UNION ALL SELECT 'profile_directions', COUNT(*) FROM {{TARGET}}.sr_profile_skill WHERE category = 'DIRECTION';
