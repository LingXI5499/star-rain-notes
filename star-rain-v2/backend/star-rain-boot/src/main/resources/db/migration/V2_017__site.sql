CREATE TABLE sr_site_config (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(50) NOT NULL DEFAULT 'PRIMARY',
    site_name VARCHAR(120) NOT NULL,
    site_title VARCHAR(255) NOT NULL,
    site_description VARCHAR(1000) NULL,
    home_intro VARCHAR(2000) NULL,
    footer_text VARCHAR(1000) NULL,
    logo_media_asset_id BIGINT NULL,
    favicon_media_asset_id BIGINT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_sr_site_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sr_site_home_section (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    section_code VARCHAR(80) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    config_json JSON NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_sr_site_home_section_code (section_code),
    KEY idx_sr_site_home_section_order (enabled, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO sr_site_config(config_key, site_name, site_title, site_description, home_intro, footer_text)
VALUES ('PRIMARY', '星雨笔录', '星雨笔录', '记录技术、学习与作品。', '在这里整理学习路径、技术文章与作品。', '星雨笔录');

INSERT INTO sr_site_home_section(section_code, display_name, enabled, sort_order, config_json) VALUES
('HERO', '首页介绍', 1, 0, '{"layout":"hero"}'),
('TUTORIALS', '教程', 1, 10, '{"limit":6}'),
('BLOG', '博客', 1, 20, '{"limit":6}'),
('PORTFOLIO', '作品', 1, 30, '{"limit":6}'),
('PROFILE', '关于作者', 1, 40, '{}'),
('HOT_CONTENT', '热门内容', 0, 50, '{"limit":6}');

INSERT INTO sr_permission(code, resource, action, name, description, status) VALUES
('site:config-manage', 'site', 'config-manage', '管理站点配置', '修改站点配置及站点媒体', 'ENABLED'),
('site:section-manage', 'site', 'section-manage', '管理首页区块', '调整首页区块与顺序', 'ENABLED'),
('site:dashboard-read', 'site', 'dashboard-read', '查看站点仪表盘', '查看后台站点汇总', 'ENABLED');

INSERT IGNORE INTO sr_role_permission(role_id, permission_id)
SELECT r.id, p.id FROM sr_role r CROSS JOIN sr_permission p
WHERE r.code = 'SUPER_ADMIN' AND p.code IN ('site:config-manage', 'site:section-manage', 'site:dashboard-read');
