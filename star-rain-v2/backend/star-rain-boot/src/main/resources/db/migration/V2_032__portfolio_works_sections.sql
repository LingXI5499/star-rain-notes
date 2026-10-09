-- Works V1.1 extends the existing V2 aggregate; identifiers and legacy content remain intact.
ALTER TABLE sr_portfolio_work
 ADD COLUMN subtitle VARCHAR(255) NULL,
 ADD COLUMN category_id BIGINT NULL,
 ADD COLUMN format_id BIGINT NULL,
 ADD COLUMN role VARCHAR(255) NULL,
 ADD COLUMN tech_stack VARCHAR(1000) NULL,
 ADD COLUMN project_status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
 ADD COLUMN started_on DATE NULL,
 ADD COLUMN ended_on DATE NULL,
 ADD COLUMN featured BOOLEAN NOT NULL DEFAULT FALSE,
 ADD COLUMN sort_order INT NOT NULL DEFAULT 0,
 ADD COLUMN seo_title VARCHAR(255) NULL,
 ADD COLUMN seo_description VARCHAR(1000) NULL,
 ADD INDEX idx_portfolio_category_status (category_id,status,sort_order),
 ADD INDEX idx_portfolio_format_status (format_id,status);
CREATE TABLE sr_portfolio_category (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, code VARCHAR(50) NOT NULL UNIQUE,
 name VARCHAR(100) NOT NULL, group_code VARCHAR(30) NULL, sort_order INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE sr_portfolio_format LIKE sr_portfolio_category;
CREATE TABLE sr_portfolio_tag LIKE sr_portfolio_category;
CREATE TABLE sr_portfolio_work_tag (
 work_id BIGINT NOT NULL, tag_id BIGINT NOT NULL, PRIMARY KEY(work_id,tag_id), INDEX idx_work_tag_tag(tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE sr_portfolio_template (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, code VARCHAR(50) NOT NULL UNIQUE,
 name VARCHAR(100) NOT NULL, description VARCHAR(500) NOT NULL, sections_json JSON NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE sr_portfolio_section (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, work_id BIGINT NOT NULL,
 section_type VARCHAR(30) NOT NULL, title VARCHAR(255) NULL, content LONGTEXT NULL,
 data_json JSON NOT NULL, block_version INT NOT NULL DEFAULT 1,
 visible BOOLEAN NOT NULL DEFAULT TRUE, sort_order INT NOT NULL DEFAULT 0,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
 INDEX idx_work_section(work_id,sort_order,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE sr_portfolio_section_media (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, section_id BIGINT NOT NULL,
 media_asset_id BIGINT NOT NULL, caption VARCHAR(500) NULL, sort_order INT NOT NULL DEFAULT 0,
 UNIQUE KEY uk_section_media(section_id,media_asset_id), INDEX idx_section_asset(media_asset_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO sr_portfolio_category(code,name,group_code,sort_order) VALUES ('software','软件项目',NULL,10);
INSERT INTO sr_portfolio_category(code,name,group_code,sort_order) VALUES ('ai-agent','AI Agent',NULL,20);
INSERT INTO sr_portfolio_category(code,name,group_code,sort_order) VALUES ('tool','工具',NULL,30);
INSERT INTO sr_portfolio_category(code,name,group_code,sort_order) VALUES ('experiment','实验与研究',NULL,40);
INSERT INTO sr_portfolio_category(code,name,group_code,sort_order) VALUES ('content-creation','内容创作',NULL,50);
INSERT INTO sr_portfolio_format(code,name,group_code,sort_order) VALUES ('web','Web 应用','SOFTWARE',10);
INSERT INTO sr_portfolio_format(code,name,group_code,sort_order) VALUES ('mobile','移动应用','SOFTWARE',20);
INSERT INTO sr_portfolio_format(code,name,group_code,sort_order) VALUES ('desktop','桌面应用','SOFTWARE',30);
INSERT INTO sr_portfolio_format(code,name,group_code,sort_order) VALUES ('cli','命令行工具','SOFTWARE',40);
INSERT INTO sr_portfolio_format(code,name,group_code,sort_order) VALUES ('api-service','API 服务','SOFTWARE',50);
INSERT INTO sr_portfolio_format(code,name,group_code,sort_order) VALUES ('mini-program','小程序','SOFTWARE',60);
INSERT INTO sr_portfolio_format(code,name,group_code,sort_order) VALUES ('game','游戏','SOFTWARE',70);
INSERT INTO sr_portfolio_format(code,name,group_code,sort_order) VALUES ('embedded-iot','嵌入式 / IoT','SOFTWARE',80);
INSERT INTO sr_portfolio_format(code,name,group_code,sort_order) VALUES ('article','文章','CONTENT',90);
INSERT INTO sr_portfolio_format(code,name,group_code,sort_order) VALUES ('video','视频','CONTENT',100);
INSERT INTO sr_portfolio_format(code,name,group_code,sort_order) VALUES ('music','音乐','CONTENT',110);
INSERT INTO sr_portfolio_format(code,name,group_code,sort_order) VALUES ('novel','小说','CONTENT',120);
INSERT INTO sr_portfolio_format(code,name,group_code,sort_order) VALUES ('photography','摄影','CONTENT',130);
INSERT INTO sr_portfolio_format(code,name,group_code,sort_order) VALUES ('design','设计','CONTENT',140);
INSERT INTO sr_portfolio_format(code,name,group_code,sort_order) VALUES ('animation','动画','CONTENT',150);
INSERT INTO sr_portfolio_format(code,name,group_code,sort_order) VALUES ('technical-poc','技术验证','EXPERIMENT',160);
INSERT INTO sr_portfolio_format(code,name,group_code,sort_order) VALUES ('benchmark','基准测试','EXPERIMENT',170);
INSERT INTO sr_portfolio_format(code,name,group_code,sort_order) VALUES ('research','研究','EXPERIMENT',180);
INSERT INTO sr_portfolio_format(code,name,group_code,sort_order) VALUES ('visualization','可视化','EXPERIMENT',190);
INSERT INTO sr_portfolio_format(code,name,group_code,sort_order) VALUES ('prototype','原型','EXPERIMENT',200);
INSERT INTO sr_portfolio_tag(code,name,group_code,sort_order) VALUES ('java','Java','TECH',10);
INSERT INTO sr_portfolio_tag(code,name,group_code,sort_order) VALUES ('vue','Vue','TECH',20);
INSERT INTO sr_portfolio_tag(code,name,group_code,sort_order) VALUES ('spring-boot','Spring Boot','TECH',30);
INSERT INTO sr_portfolio_tag(code,name,group_code,sort_order) VALUES ('ai','人工智能','TOPIC',40);
INSERT INTO sr_portfolio_tag(code,name,group_code,sort_order) VALUES ('learning','学习','TOPIC',50);
INSERT INTO sr_portfolio_tag(code,name,group_code,sort_order) VALUES ('open-source','开源','OTHER',60);
INSERT INTO sr_portfolio_template(code,name,description,sections_json) VALUES ('SOFTWARE','软件项目','选择后在保存作品时复制内容结构，可独立修改。','[{"sectionType":"MARKDOWN","title":"项目背景","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"项目目标","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"用户 / 使用场景","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"功能模块","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"产品设计","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"技术架构","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"数据设计","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"开发过程","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"核心难点","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"测试与质量","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"部署与上线","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"项目成果","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"项目复盘","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"后续计划","content":"","data":{},"blockVersion":1,"visible":true}]');
INSERT INTO sr_portfolio_template(code,name,description,sections_json) VALUES ('AI_AGENT','AI Agent','选择后在保存作品时复制内容结构，可独立修改。','[{"sectionType":"MARKDOWN","title":"项目背景","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"问题定义","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"Agent 目标","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"总体架构","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"Model","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"Tools","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"Memory","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"RAG / Knowledge","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"Workflow","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"Prompt / Skills","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"执行流程","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"Evaluation","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"效果展示","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"已知限制","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"后续计划","content":"","data":{},"blockVersion":1,"visible":true}]');
INSERT INTO sr_portfolio_template(code,name,description,sections_json) VALUES ('TOOL','工具','选择后在保存作品时复制内容结构，可独立修改。','[{"sectionType":"MARKDOWN","title":"为什么做","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"解决的问题","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"核心功能","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"使用流程","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"关键实现","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"使用示例","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"安装 / 下载","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"项目结果","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"后续计划","content":"","data":{},"blockVersion":1,"visible":true}]');
INSERT INTO sr_portfolio_template(code,name,description,sections_json) VALUES ('EXPERIMENT','实验与研究','选择后在保存作品时复制内容结构，可独立修改。','[{"sectionType":"MARKDOWN","title":"Question","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"Hypothesis","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"实验环境","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"实验设计","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"实验过程","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"数据结果","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"分析","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"结论","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"局限","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"下一步","content":"","data":{},"blockVersion":1,"visible":true}]');
INSERT INTO sr_portfolio_template(code,name,description,sections_json) VALUES ('CONTENT','内容创作','选择后在保存作品时复制内容结构，可独立修改。','[{"sectionType":"MARKDOWN","title":"创作缘起","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"想表达什么","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"创作过程","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"最终作品","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"幕后内容","content":"","data":{},"blockVersion":1,"visible":true},{"sectionType":"MARKDOWN","title":"创作复盘","content":"","data":{},"blockVersion":1,"visible":true}]');
INSERT INTO sr_portfolio_template(code,name,description,sections_json) VALUES ('BLANK','空白作品','选择后在保存作品时复制内容结构，可独立修改。','[]');
UPDATE sr_portfolio_work w SET
 category_id=(SELECT id FROM sr_portfolio_category WHERE code=IF(w.work_type='SOFTWARE','software','content-creation')),
 format_id=(SELECT id FROM sr_portfolio_format WHERE code=CASE w.work_type WHEN 'VIDEO' THEN 'video' WHEN 'MUSIC' THEN 'music' WHEN 'WRITING' THEN 'article' ELSE 'web' END);
-- Existing bodies remain the legacy fallback until an author deliberately adds sections.
