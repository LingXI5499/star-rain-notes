-- =====================================================================
-- Star Rain Notes — V9__create_english_shared_foundation.sql
-- 英语阅读/听力/写作系统 · 阶段1「共享底座」
-- 共享语义标签（taxonomy）· CEFR 标准 · 练习基础 · 学习组合主体 ·
-- 词族扩展表 · 媒体类型新增 AUDIO。
-- MySQL 8.0 / InnoDB / utf8mb4 / utf8mb4_0900_ai_ci / DATETIME(6)。
-- 本迁移在 V8 基础上追加，不修改任何既有迁移。
-- =====================================================================

-- ---------------------------------------------------------------
-- 0. media_asset：媒体类型扩展 AUDIO
--    （原 V1 CHECK 仅允许 IMAGE/DOCUMENT，见《详细开发方案》§13.1）
-- ---------------------------------------------------------------
ALTER TABLE media_asset DROP CHECK ck_media_asset_type;
ALTER TABLE media_asset ADD CONSTRAINT ck_media_asset_type
    CHECK (asset_type IN ('IMAGE','DOCUMENT','AUDIO'));

-- ---------------------------------------------------------------
-- 1. english_taxonomy_term — 统一语义标签（最多两级、同维度父子、无循环）
--    维度维度：TOPIC / SCENE / FUNCTION / ABILITY / GENRE / FORMAT
-- ---------------------------------------------------------------
CREATE TABLE english_taxonomy_term (
    id BIGINT NOT NULL AUTO_INCREMENT,
    parent_id BIGINT NULL,
    dimension VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    sort_order INT NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_taxonomy_slug (slug),
    KEY idx_taxonomy_dimension (dimension, parent_id, sort_order, id),
    CONSTRAINT ck_taxonomy_dimension CHECK (
        dimension IN ('TOPIC','SCENE','FUNCTION','ABILITY','GENRE','FORMAT')
    ),
    CONSTRAINT ck_taxonomy_order CHECK (sort_order > 0),
    CONSTRAINT fk_taxonomy_parent FOREIGN KEY (parent_id)
        REFERENCES english_taxonomy_term(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 首批标签种子（§5.1）。显式 id 以便子级引用父级；AUTO_INCREMENT 自动顺延。
INSERT INTO english_taxonomy_term (id, parent_id, dimension, name, slug, description, sort_order) VALUES
    -- TOPIC 主题（8 个顶级，政治/哲学带子级）
    (1,  NULL, 'TOPIC',    '科技',         'topic-tech',        '人工智能、信息技术、工程与科学发现', 10),
    (2,  NULL, 'TOPIC',    '社会',         'topic-society',     '社会现象、人群关系与公共议题',       20),
    (3,  NULL, 'TOPIC',    '经济',         'topic-economy',     '市场、产业、企业与宏观经济',         30),
    (4,  NULL, 'TOPIC',    '环境',         'topic-environment', '生态、气候、能源与可持续发展',       40),
    (5,  NULL, 'TOPIC',    '文化',         'topic-culture',     '习俗、艺术、历史与地域文化',         50),
    (6,  NULL, 'TOPIC',    '健康',         'topic-health',      '医学、公共卫生、心理与生活方式',     60),
    (7,  NULL, 'TOPIC',    '政治',         'topic-politics',    '国际关系、公共政策、国家治理',       70),
    (8,  NULL, 'TOPIC',    '哲学与思想',   'topic-philosophy',  '思想观点、人类认知、价值讨论',       80),
    (9,  7,    'TOPIC',    '国际关系',     'topic-politics-international', '国家间外交、合作与冲突', 10),
    (10, 7,    'TOPIC',    '公共政策',     'topic-politics-policy',         '政府制度、政策与治理',   20),
    (11, 7,    'TOPIC',    '国家治理',     'topic-politics-governance',     '国家治理体系与能力',     30),
    (12, 8,    'TOPIC',    '思想观点',     'topic-philosophy-ideas',        '观念、立场与思想流派',   10),
    (13, 8,    'TOPIC',    '人类认知',     'topic-philosophy-cognition',    '认知、理性与意识',       20),
    (14, 8,    'TOPIC',    '价值讨论',     'topic-philosophy-value',        '伦理、价值与意义追问',   30),
    -- FUNCTION 功能标签（9 个）
    (15, NULL, 'FUNCTION', '描述',         'function-describe',  '客观描述事物、现象或过程', 10),
    (16, NULL, 'FUNCTION', '说明',         'function-explain',   '说明原理、步骤或机制',     20),
    (17, NULL, 'FUNCTION', '比较',         'function-compare',   '对比异同、权衡利弊',       30),
    (18, NULL, 'FUNCTION', '举例',         'function-exemplify', '用实例支撑观点',           40),
    (19, NULL, 'FUNCTION', '因果',         'function-cause',     '剖析原因与结果',           50),
    (20, NULL, 'FUNCTION', '转折',         'function-contrast',  '转折、让步与反差',         60),
    (21, NULL, 'FUNCTION', '论证',         'function-argue',     '论证主张与论据',           70),
    (22, NULL, 'FUNCTION', '反驳',         'function-rebut',     '回应与反驳对立观点',       80),
    (23, NULL, 'FUNCTION', '总结',         'function-summarize', '归纳要点、得出结论',       90),
    -- ABILITY 能力标签（12 个）
    (24, NULL, 'ABILITY',  '句子理解',     'ability-sentence',        '理解句子结构与含义',     10),
    (25, NULL, 'ABILITY',  '信息提取',     'ability-extract',         '定位并提取关键信息',     20),
    (26, NULL, 'ABILITY',  '主题句识别',   'ability-topic-sentence',  '识别段落与全文主题句',   30),
    (27, NULL, 'ABILITY',  '结构分析',     'ability-structure',       '分析篇章层级与组织',     40),
    (28, NULL, 'ABILITY',  '指代判断',     'ability-reference',       '判断代词与指代关系',     50),
    (29, NULL, 'ABILITY',  '逻辑判断',     'ability-logic',           '辨析逻辑与推理关系',     60),
    (30, NULL, 'ABILITY',  '隐含推断',     'ability-inference',       '从隐含信息推断结论',     70),
    (31, NULL, 'ABILITY',  '语音识别',     'ability-phoneme',         '识别语音与音位差异',     80),
    (32, NULL, 'ABILITY',  '信息保持',     'ability-retention',       '短时保持与回忆信息',     90),
    (33, NULL, 'ABILITY',  '观点组织',     'ability-organization',    '组织与展开个人观点',     100),
    (34, NULL, 'ABILITY',  '衔接连贯',     'ability-cohesion',        '使用衔接手段保持连贯',   110),
    (35, NULL, 'ABILITY',  '风格控制',     'ability-style',           '控制正式度与语体风格',   120),
    -- GENRE 文体标签（5 个，阅读）
    (36, NULL, 'GENRE',    '说明文',       'genre-expository',    '说明与解释性文本',       10),
    (37, NULL, 'GENRE',    '议论文',       'genre-argumentative', '论证与立论类文本',       20),
    (38, NULL, 'GENRE',    '新闻报道',     'genre-news',          '新闻与报道文体',         30),
    (39, NULL, 'GENRE',    '学术文本',     'genre-academic',      '学术论文与专业文本',     40),
    (40, NULL, 'GENRE',    '应用文本',     'genre-application',   '应用性、实用性文体',     50),
    -- SCENE 场景标签（4 个，听力）
    (41, NULL, 'SCENE',    '日常交流',     'scene-daily',   '日常生活口语场景', 10),
    (42, NULL, 'SCENE',    '学习场景',     'scene-study',   '学习与课堂场景',   20),
    (43, NULL, 'SCENE',    '工作场景',     'scene-work',    '职场与工作场景',   30),
    (44, NULL, 'SCENE',    '新闻场景',     'scene-news',    '新闻播报场景',     40),
    -- FORMAT 形式标签（5 个，听力）
    (45, NULL, 'FORMAT',   '短对话',       'format-short-dialogue', '简短对话',   10),
    (46, NULL, 'FORMAT',   '长对话',       'format-long-dialogue',  '较长的对话', 20),
    (47, NULL, 'FORMAT',   '独白',         'format-monologue',      '个人独白',   30),
    (48, NULL, 'FORMAT',   '新闻听力',     'format-news',           '新闻听力',   40),
    (49, NULL, 'FORMAT',   '演讲与讲座',   'format-lecture',        '演讲与讲座', 50);

-- ---------------------------------------------------------------
-- 2. english_cefr_standard — 六级 CEFR 标准（主键为等级代码）
--    数值允许上界为空；非空值必须 ≥0 且最小值不能大于最大值。
-- ---------------------------------------------------------------
CREATE TABLE english_cefr_standard (
    level VARCHAR(2) NOT NULL,
    vocab_min INT NOT NULL,
    vocab_max INT NULL,
    reading_sentence_min INT NOT NULL,
    reading_sentence_max INT NULL,
    listening_wpm_min INT NOT NULL,
    listening_wpm_max INT NULL,
    writing_length_min INT NOT NULL,
    writing_length_max INT NULL,
    description VARCHAR(300) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (level),
    CONSTRAINT ck_cefr_level CHECK (level IN ('A1','A2','B1','B2','C1','C2')),
    CONSTRAINT ck_cefr_vocab CHECK (
        vocab_min >= 0 AND (vocab_max IS NULL OR vocab_max >= vocab_min)
    ),
    CONSTRAINT ck_cefr_reading CHECK (
        reading_sentence_min >= 0
        AND (reading_sentence_max IS NULL OR reading_sentence_max >= reading_sentence_min)
    ),
    CONSTRAINT ck_cefr_listening CHECK (
        listening_wpm_min >= 0
        AND (listening_wpm_max IS NULL OR listening_wpm_max >= listening_wpm_min)
    ),
    CONSTRAINT ck_cefr_writing CHECK (
        writing_length_min >= 0
        AND (writing_length_max IS NULL OR writing_length_max >= writing_length_min)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- CEFR 种子（§5.2；仅用于发布提醒，非数据库硬约束）
INSERT INTO english_cefr_standard
    (level, vocab_min, vocab_max, reading_sentence_min, reading_sentence_max,
     listening_wpm_min, listening_wpm_max, writing_length_min, writing_length_max, description)
VALUES
    ('A1', 200,   500,   5,  8,    70,  90, 30, 60,   '入门：简单句、通知与短对话'),
    ('A2', 500,   1000,  8,  12,   90,  110, 60, 120,  '基础：两三个短段落'),
    ('B1', 1000,  2000,  12, 18,   110, 140, 120, 200, '中级：说明或叙述短篇'),
    ('B2', 2000,  3500,  15, 25,   140, 170, 200, 300, '中高级：说明文与议论文'),
    ('C1', 3500,  5000,  20, 30,   170, 200, 300, 500, '高级：长篇学术或专业文本'),
    ('C2', 5000,  NULL,  25, NULL, 190, NULL, 500, NULL,'精通：复杂多主题高密度文本');

-- ---------------------------------------------------------------
-- 3. english_exercise — 练习基础表（题型配置存 JSON，服务层按题型校验）
-- ---------------------------------------------------------------
CREATE TABLE english_exercise (
    id BIGINT NOT NULL AUTO_INCREMENT,
    module_type VARCHAR(20) NOT NULL,
    question_type VARCHAR(50) NOT NULL,
    prompt_markdown TEXT NOT NULL,
    config_json JSON NOT NULL,
    explanation_markdown TEXT NULL,
    score_value INT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0,
    publish_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_exercise_module (module_type, publish_status, sort_order, id),
    CONSTRAINT ck_exercise_module CHECK (module_type IN ('READING','LISTENING','WRITING')),
    CONSTRAINT ck_exercise_status CHECK (publish_status IN ('DRAFT','PUBLISHED','WITHDRAWN')),
    CONSTRAINT ck_exercise_score CHECK (score_value > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------
-- 4. english_learning_bundle — 学习组合主体（条目映射表在后缀阶段）
-- ---------------------------------------------------------------
CREATE TABLE english_learning_bundle (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(150) NOT NULL,
    summary VARCHAR(1000) NULL,
    primary_cefr VARCHAR(2) NULL,
    cover_media_id BIGINT NULL,
    publish_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    sort_order INT NOT NULL DEFAULT 0,
    published_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_bundle_slug (slug),
    KEY idx_bundle_public (publish_status, sort_order, id),
    CONSTRAINT ck_bundle_status CHECK (publish_status IN ('DRAFT','PUBLISHED','WITHDRAWN')),
    CONSTRAINT ck_bundle_lifecycle CHECK (
        (publish_status = 'DRAFT' AND published_at IS NULL)
        OR
        (publish_status IN ('PUBLISHED','WITHDRAWN') AND published_at IS NOT NULL)
    ),
    CONSTRAINT fk_bundle_cefr FOREIGN KEY (primary_cefr)
        REFERENCES english_cefr_standard(level) ON DELETE RESTRICT,
    CONSTRAINT fk_bundle_cover FOREIGN KEY (cover_media_id)
        REFERENCES media_asset(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------
-- 5. 词族扩展（§9.6；只建表，不自动解析任何 inflections 文本）
-- ---------------------------------------------------------------
CREATE TABLE vocabulary_word_family (
    id BIGINT NOT NULL AUTO_INCREMENT,
    head_word VARCHAR(200) NOT NULL,
    slug VARCHAR(150) NOT NULL,
    description VARCHAR(1000) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_word_family_slug (slug)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE vocabulary_family_member (
    id BIGINT NOT NULL AUTO_INCREMENT,
    family_id BIGINT NOT NULL,
    spelling VARCHAR(200) NOT NULL,
    part_of_speech VARCHAR(50) NULL,
    phonetic_us VARCHAR(100) NULL,
    translation VARCHAR(1000) NULL,
    cefr_level VARCHAR(2) NULL,
    example_sentence VARCHAR(1000) NULL,
    example_translation VARCHAR(1000) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_family_member (family_id, sort_order),
    CONSTRAINT fk_family_member_family FOREIGN KEY (family_id)
        REFERENCES vocabulary_word_family(id) ON DELETE CASCADE,
    CONSTRAINT fk_family_member_cefr FOREIGN KEY (cefr_level)
        REFERENCES english_cefr_standard(level) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE vocabulary_word_family_link (
    family_id BIGINT NOT NULL,
    word_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (family_id, word_id),
    KEY idx_word_family_link_word (word_id, family_id),
    CONSTRAINT fk_word_family_link_family FOREIGN KEY (family_id)
        REFERENCES vocabulary_word_family(id) ON DELETE CASCADE,
    CONSTRAINT fk_word_family_link_word FOREIGN KEY (word_id)
        REFERENCES vocabulary_word(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
