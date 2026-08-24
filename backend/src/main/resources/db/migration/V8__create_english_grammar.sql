-- Independent English grammar course domain. This deliberately does not reuse
-- tutorial_* or vocabulary_* tables: each English sub-system owns its data.

CREATE TABLE english_grammar_course (
    id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    subtitle VARCHAR(300) NULL,
    summary VARCHAR(1000) NULL,
    introduction TEXT NULL,
    roadmap_markdown TEXT NULL,
    cover_media_id BIGINT NULL,
    seo_title VARCHAR(200) NULL,
    seo_description VARCHAR(500) NULL,
    publish_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    published_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT ck_grammar_course_singleton CHECK (id = 1),
    CONSTRAINT ck_grammar_course_status CHECK (publish_status IN ('DRAFT','PUBLISHED','WITHDRAWN')),
    CONSTRAINT ck_grammar_course_lifecycle CHECK (
        (publish_status = 'DRAFT' AND published_at IS NULL) OR
        (publish_status IN ('PUBLISHED','WITHDRAWN') AND published_at IS NOT NULL)
    ),
    CONSTRAINT fk_grammar_course_cover FOREIGN KEY (cover_media_id)
        REFERENCES media_asset(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE english_grammar_section (
    id BIGINT NOT NULL AUTO_INCREMENT,
    course_id BIGINT NOT NULL DEFAULT 1,
    title VARCHAR(200) NOT NULL,
    sort_order INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_grammar_section_order (course_id, sort_order),
    CONSTRAINT ck_grammar_section_order CHECK (sort_order > 0),
    CONSTRAINT fk_grammar_section_course FOREIGN KEY (course_id)
        REFERENCES english_grammar_course(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE english_grammar_lesson (
    id BIGINT NOT NULL AUTO_INCREMENT,
    course_id BIGINT NOT NULL DEFAULT 1,
    section_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(50) NOT NULL,
    summary VARCHAR(1000) NULL,
    body_markdown LONGTEXT NOT NULL,
    publish_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    sort_order INT NOT NULL,
    published_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_grammar_lesson_slug (slug),
    UNIQUE KEY uk_grammar_lesson_order (section_id, sort_order),
    KEY idx_grammar_lesson_public (publish_status, section_id, sort_order, id),
    CONSTRAINT ck_grammar_lesson_order CHECK (sort_order > 0),
    CONSTRAINT ck_grammar_lesson_status CHECK (publish_status IN ('DRAFT','PUBLISHED','WITHDRAWN')),
    CONSTRAINT ck_grammar_lesson_lifecycle CHECK (
        (publish_status = 'DRAFT' AND published_at IS NULL) OR
        (publish_status IN ('PUBLISHED','WITHDRAWN') AND published_at IS NOT NULL)
    ),
    CONSTRAINT fk_grammar_lesson_course FOREIGN KEY (course_id)
        REFERENCES english_grammar_course(id) ON DELETE RESTRICT,
    CONSTRAINT fk_grammar_lesson_section FOREIGN KEY (section_id)
        REFERENCES english_grammar_section(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO english_grammar_course
    (id, title, subtitle, summary, introduction, roadmap_markdown,
     seo_title, seo_description, publish_status, published_at)
VALUES
    (1, '英语语法完整教程', '从语言基础单位到复杂句法分析',
     '以现代英语语法结构为核心，从词法、短语和句子结构逐步扩展到动词、从句、非谓语、特殊句式与语篇级分析。',
     '建立完整英语语法知识框架，理解英语句子的生成逻辑，掌握复杂句子的结构分析能力，并支撑高级英文阅读与表达。',
     '1. 词法基础\n2. 短语结构\n3. 基本句型\n4. 动词系统\n5. 从句体系\n6. 非谓语体系\n7. 特殊句式\n8. 复杂句分析\n9. 综合应用',
     '英语语法完整教程｜星雨笔录', '从词法到语篇，系统建立英语语法知识框架与复杂句分析能力。',
     'PUBLISHED', UTC_TIMESTAMP(6));

INSERT INTO english_grammar_section (id, course_id, title, sort_order) VALUES
    (1, 1, '第一章 英语语法总论', 10),
    (2, 1, '第二章 词法体系（Word Level）', 20),
    (3, 1, '第三章 短语结构体系', 30),
    (4, 1, '第四章 基本句型体系', 40),
    (5, 1, '第五章 动词高级体系', 50),
    (6, 1, '第六章 从句体系', 60),
    (7, 1, '第七章 非谓语动词体系', 70),
    (8, 1, '第八章 特殊句式体系', 80),
    (9, 1, '第九章 复杂句法分析体系', 90),
    (10, 1, '第十章 高级语法综合应用', 100);

INSERT INTO english_grammar_lesson
    (course_id, section_id, title, slug, summary, body_markdown, publish_status, sort_order, published_at)
VALUES
    (1,1,'语言结构基础','1-1','英语语言组成层次、词与句子的关系、语法规则和句法结构特点。','## 学习内容\n\n- 英语语言组成层次\n- 词、短语、分句、句子的关系\n- 语法规则的基本作用\n- 英语句法结构特点','PUBLISHED',10,UTC_TIMESTAMP(6)),
    (1,1,'英语句子分析方法','1-2','句子成分、主干与修饰成分、层级结构及语法树基础。','## 学习内容\n\n- 句子成分划分\n- 主干与修饰成分识别\n- 层级结构分析方法\n- 语法树基本概念','PUBLISHED',20,UTC_TIMESTAMP(6)),
    (1,2,'名词系统','2-1','名词分类、数与所有格，以及名词的句法功能。','## 学习内容\n\n- 名词分类\n- 可数与不可数名词\n- 名词单复数变化\n- 名词所有格\n- 名词句法功能','PUBLISHED',10,UTC_TIMESTAMP(6)),
    (1,2,'代词系统','2-2','人称、物主、反身、指示、疑问、不定及关系代词。','## 学习内容\n\n- 人称代词\n- 物主代词\n- 反身代词\n- 指示代词\n- 疑问代词\n- 不定代词\n- 关系代词','PUBLISHED',20,UTC_TIMESTAMP(6)),
    (1,2,'冠词系统','2-3','不定冠词、定冠词、零冠词及其使用逻辑。','## 学习内容\n\n- 不定冠词\n- 定冠词\n- 零冠词\n- 冠词使用逻辑','PUBLISHED',30,UTC_TIMESTAMP(6)),
    (1,2,'动词系统基础','2-4','实义、系、助、情态动词及及物性。','## 学习内容\n\n- 实义动词\n- 系动词\n- 助动词\n- 情态动词\n- 及物动词与不及物动词','PUBLISHED',40,UTC_TIMESTAMP(6)),
    (1,2,'形容词系统','2-5','形容词功能、位置、比较等级与多形容词排序。','## 学习内容\n\n- 形容词功能\n- 形容词位置\n- 比较级\n- 最高级\n- 多形容词排序','PUBLISHED',50,UTC_TIMESTAMP(6)),
    (1,2,'副词系统','2-6','副词分类、修饰关系及位置变化。','## 学习内容\n\n- 副词分类\n- 副词修饰关系\n- 副词位置变化','PUBLISHED',60,UTC_TIMESTAMP(6)),
    (1,2,'介词系统','2-7','时间、地点、方式介词与介词短语结构。','## 学习内容\n\n- 时间介词\n- 地点介词\n- 方式介词\n- 介词短语结构','PUBLISHED',70,UTC_TIMESTAMP(6)),
    (1,2,'连词系统','2-8','并列连词、从属连词与连接结构分析。','## 学习内容\n\n- 并列连词\n- 从属连词\n- 连接结构分析','PUBLISHED',80,UTC_TIMESTAMP(6)),
    (1,3,'名词短语','3-1','名词短语组成、限定与修饰关系及长短语分析。','## 学习内容\n\n- 名词短语组成\n- 限定结构\n- 修饰关系\n- 长名词短语分析','PUBLISHED',10,UTC_TIMESTAMP(6)),
    (1,3,'动词短语','3-2','动词中心结构、宾语补语关系与搭配结构。','## 学习内容\n\n- 动词中心结构\n- 宾语与补语关系\n- 动词搭配结构','PUBLISHED',20,UTC_TIMESTAMP(6)),
    (1,3,'形容词短语','3-3','形容词中心结构与程度修饰结构。','## 学习内容\n\n- 形容词中心结构\n- 程度修饰结构','PUBLISHED',30,UTC_TIMESTAMP(6)),
    (1,3,'副词短语','3-4','副词组合结构与状语表达方式。','## 学习内容\n\n- 副词组合结构\n- 状语表达方式','PUBLISHED',40,UTC_TIMESTAMP(6)),
    (1,3,'介词短语','3-5','介词短语功能、后置修饰及多层结构分析。','## 学习内容\n\n- 介词短语功能\n- 后置修饰结构\n- 多层介词结构分析','PUBLISHED',50,UTC_TIMESTAMP(6)),
    (1,4,'句子核心结构','4-1','主语、谓语、宾语、表语、补语、定语和状语。','## 学习内容\n\n- 主语\n- 谓语\n- 宾语\n- 表语\n- 补语\n- 定语\n- 状语','PUBLISHED',10,UTC_TIMESTAMP(6)),
    (1,4,'五大基本句型','4-2','SV、SVC、SVO、SVOO和SVOC结构。','## 学习内容\n\n- SV结构\n- SVC结构\n- SVO结构\n- SVOO结构\n- SVOC结构','PUBLISHED',20,UTC_TIMESTAMP(6)),
    (1,4,'句子扩展机制','4-3','修饰、并列、从句与非谓语扩展。','## 学习内容\n\n- 修饰扩展\n- 并列扩展\n- 从句扩展\n- 非谓语扩展','PUBLISHED',30,UTC_TIMESTAMP(6)),
    (1,5,'时态系统','5-1','一般、进行、完成、完成进行时态及组合逻辑。','## 学习内容\n\n- 一般时态\n- 进行时态\n- 完成时态\n- 完成进行时态\n- 时态组合逻辑','PUBLISHED',10,UTC_TIMESTAMP(6)),
    (1,5,'语态系统','5-2','主动、被动语态及特殊被动结构。','## 学习内容\n\n- 主动语态\n- 被动语态\n- 被动结构变化\n- 特殊被动形式','PUBLISHED',20,UTC_TIMESTAMP(6)),
    (1,5,'情态动词系统','5-3','能力、可能、必要、推测与义务表达。','## 学习内容\n\n- 能力\n- 可能性\n- 必要性\n- 推测\n- 义务表达','PUBLISHED',30,UTC_TIMESTAMP(6)),
    (1,5,'虚拟语气系统','5-4','条件虚拟、愿望、假设与特殊虚拟结构。','## 学习内容\n\n- 条件虚拟\n- 愿望表达\n- 假设表达\n- 特殊虚拟结构','PUBLISHED',40,UTC_TIMESTAMP(6)),
    (1,6,'从句基础理论','6-1','主句从句、限定性与非限定性及连接方式。','## 学习内容\n\n- 主句与从句\n- 限定性与非限定性结构\n- 从句连接方式','PUBLISHED',10,UTC_TIMESTAMP(6)),
    (1,6,'名词性从句','6-2','主语、宾语、表语和同位语从句。','## 学习内容\n\n- 主语从句\n- 宾语从句\n- 表语从句\n- 同位语从句','PUBLISHED',20,UTC_TIMESTAMP(6)),
    (1,6,'定语从句','6-3','关系词、限制性与非限制性定语从句及复杂关系结构。','## 学习内容\n\n- 关系代词\n- 关系副词\n- 限制性定语从句\n- 非限制性定语从句\n- 复杂关系结构','PUBLISHED',30,UTC_TIMESTAMP(6)),
    (1,6,'状语从句','6-4','时间、原因、条件、让步、目的、结果和比较状语从句。','## 学习内容\n\n- 时间状语从句\n- 原因状语从句\n- 条件状语从句\n- 让步状语从句\n- 目的状语从句\n- 结果状语从句\n- 比较状语从句','PUBLISHED',40,UTC_TIMESTAMP(6)),
    (1,7,'非谓语动词理论','7-1','谓语与非谓语区别、产生原因及句法功能。','## 学习内容\n\n- 谓语与非谓语区别\n- 非谓语产生原因\n- 非谓语句法功能','PUBLISHED',10,UTC_TIMESTAMP(6)),
    (1,7,'不定式','7-2','不定式基本形式、三类功能与复合结构。','## 学习内容\n\n- 基本形式\n- 名词功能\n- 形容词功能\n- 副词功能\n- 复合结构','PUBLISHED',20,UTC_TIMESTAMP(6)),
    (1,7,'动名词','7-3','动名词结构、名词功能、逻辑主语与固定搭配。','## 学习内容\n\n- 动名词结构\n- 名词功能\n- 逻辑主语\n- 固定搭配','PUBLISHED',30,UTC_TIMESTAMP(6)),
    (1,7,'分词','7-4','现在与过去分词、定语状语功能及独立结构。','## 学习内容\n\n- 现在分词\n- 过去分词\n- 分词作定语\n- 分词作状语\n- 独立分词结构','PUBLISHED',40,UTC_TIMESTAMP(6)),
    (1,8,'倒装结构','8-1','完全、部分、否定与条件倒装。','## 学习内容\n\n- 完全倒装\n- 部分倒装\n- 否定倒装\n- 条件倒装','PUBLISHED',10,UTC_TIMESTAMP(6)),
    (1,8,'强调结构','8-2','强调句、强调成分与特殊强调表达。','## 学习内容\n\n- 强调句\n- 强调成分\n- 特殊强调表达','PUBLISHED',20,UTC_TIMESTAMP(6)),
    (1,8,'省略结构','8-3','主语、谓语、从句和比较结构省略。','## 学习内容\n\n- 主语省略\n- 谓语省略\n- 从句省略\n- 比较结构省略','PUBLISHED',30,UTC_TIMESTAMP(6)),
    (1,8,'插入语结构','8-4','插入成分、语气表达与评价结构。','## 学习内容\n\n- 插入成分\n- 语气表达\n- 评价结构','PUBLISHED',40,UTC_TIMESTAMP(6)),
    (1,8,'同位语结构','8-5','名词同位语、同位语从句与复杂同位结构。','## 学习内容\n\n- 名词同位语\n- 同位语从句\n- 复杂同位结构','PUBLISHED',50,UTC_TIMESTAMP(6)),
    (1,8,'独立主格结构','8-6','独立主格组成、分词独立结构与逻辑关系。','## 学习内容\n\n- 独立主格组成\n- 分词独立结构\n- 逻辑关系分析','PUBLISHED',60,UTC_TIMESTAMP(6)),
    (1,9,'长句拆解方法','9-1','寻找核心谓语、分析主干、修饰范围与嵌套结构。','## 学习内容\n\n- 寻找核心谓语\n- 分析主干\n- 划分修饰范围\n- 识别嵌套结构','PUBLISHED',10,UTC_TIMESTAMP(6)),
    (1,9,'多层结构分析','9-2','多重从句、非谓语、长名词短语与插入结构。','## 学习内容\n\n- 多重从句\n- 多重非谓语\n- 长名词短语\n- 插入结构','PUBLISHED',20,UTC_TIMESTAMP(6)),
    (1,9,'语义逻辑分析','9-3','因果、转折、条件和推理关系。','## 学习内容\n\n- 因果关系\n- 转折关系\n- 条件关系\n- 推理关系','PUBLISHED',30,UTC_TIMESTAMP(6)),
    (1,10,'复杂句生成','10-1','简单句扩展、复合句构造与高级表达组织。','## 学习内容\n\n- 简单句扩展\n- 复合句构造\n- 高级表达组织','PUBLISHED',10,UTC_TIMESTAMP(6)),
    (1,10,'语篇语法','10-2','句间关系、信息组织、衔接与连贯。','## 学习内容\n\n- 句间关系\n- 信息组织\n- 衔接与连贯','PUBLISHED',20,UTC_TIMESTAMP(6)),
    (1,10,'语法综合训练体系','10-3','句子分析、结构转换、错误识别和综合应用。','## 学习内容\n\n- 句子分析\n- 结构转换\n- 错误识别\n- 综合应用','PUBLISHED',30,UTC_TIMESTAMP(6));
