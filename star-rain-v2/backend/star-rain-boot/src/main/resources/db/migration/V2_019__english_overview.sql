CREATE TABLE sr_english_overview (
    id TINYINT NOT NULL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    subtitle VARCHAR(500) NULL,
    introduction TEXT NULL,
    current_stage VARCHAR(50) NOT NULL DEFAULT 'FOUNDATION',
    roadmap_markdown LONGTEXT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT ck_sr_english_overview_singleton CHECK (id = 1),
    CONSTRAINT ck_sr_english_overview_stage CHECK (current_stage IN ('FOUNDATION', 'WORD_MEMORY', 'READING', 'ANALYTICS'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO sr_english_overview(id, title, subtitle, current_stage)
VALUES (1, '英语能力成长路径', '从可理解输入到清晰表达', 'FOUNDATION');

INSERT INTO sr_permission(code, resource, action, name, description, status) VALUES
('english:overview-manage', 'english', 'overview-manage', '管理英语概览', '维护英语首页文案和路线图', 'ENABLED');

INSERT IGNORE INTO sr_role_permission(role_id, permission_id)
SELECT r.id, p.id FROM sr_role r CROSS JOIN sr_permission p
WHERE r.code IN ('ADMIN', 'SUPER_ADMIN') AND p.code = 'english:overview-manage';
