-- =====================================================================
-- Star Rain Notes V1 — V2__seed_system_singletons.sql
-- Frozen seed (03-database-design.md §9).
-- ONLY the three singleton rows. NO default admin user
-- (admin_user must stay empty until the setup flow creates one).
-- =====================================================================

INSERT INTO site_setting (
    id, site_name, tagline, default_seo_description, timezone
) VALUES (
    1,
    '星雨笔录',
    'Knowledge · Code · Growth',
    '个人知识、技术教程、博客与项目作品记录。',
    'Asia/Shanghai'
);

INSERT INTO profile (id, current_focus)
VALUES (1, JSON_ARRAY());

INSERT INTO english_overview (
    id, title, subtitle, current_stage, roadmap_markdown
) VALUES (
    1,
    'English',
    'Build English as a long-term skill.',
    'FOUNDATION',
    ''
);
