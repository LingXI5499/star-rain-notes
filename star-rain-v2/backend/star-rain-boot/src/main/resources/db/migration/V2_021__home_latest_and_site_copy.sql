-- V1 首页对齐：站点副标题 + 首页区块重排
--
-- 1. `sr_site_config.tagline` —— V1 首页主视觉在站名之下还有一行绿色副标题
--    （V1 的 `appStore.tagline`）。V2 的站点配置只有「首页主标题」，缺这一格，
--    因此补一个可编辑字段，而不是把它硬编码进前端。
-- 2. 站点文案按 V1 公开站实际内容回填：站名/主标题=星雨笔录，副标题=建立自己的知识世界，
--    首页介绍=首页那一段正文，SEO 描述=站点 meta description，页脚文字=品牌英文标语。
--    V2 早期把「页脚文字」当成了页脚那句话，导致品牌区标语与正文互换，这里一并纠正。
-- 3. 首页区块按 V1 的信息架构重排：新增 LATEST（最近更新，跨教程/博客/作品的混合列表），
--    教程区块改名为「精选知识体系」并收窄到 3 项，作品区块改名为「把学习做成作品」；
--    BLOG 区块关闭（博客内容已由「最近更新」承载，V1 首页也没有独立的博客区块）。
--    关闭不删除：管理端仍可随时重新开启或排序。

ALTER TABLE sr_site_config
    ADD COLUMN tagline VARCHAR(255) NULL AFTER site_title;

UPDATE sr_site_config
SET site_name = '星雨笔录',
    site_title = '星雨笔录',
    tagline = '建立自己的知识世界',
    site_description = '系统整理 Java 全栈、计算机基础、软件工程与英语学习内容，用真实项目验证学习与成长。',
    home_intro = '系统整理技术，认真记录思考，在真实项目中验证学习，也为下一次出发留下清晰的路径。',
    footer_text = 'Knowledge · Code · Growth'
WHERE config_key = 'PRIMARY';

INSERT INTO sr_site_home_section(section_code, display_name, enabled, sort_order, config_json) VALUES
('LATEST', '最近更新', 1, 10, '{"limit":6,"layout":"list"}');

UPDATE sr_site_home_section SET display_name = '精选知识体系', sort_order = 20, config_json = '{"limit":3,"layout":"cards"}'
WHERE section_code = 'TUTORIALS';

UPDATE sr_site_home_section SET display_name = '把学习做成作品', sort_order = 30, config_json = '{"limit":3,"layout":"cards"}'
WHERE section_code = 'PORTFOLIO';

UPDATE sr_site_home_section SET display_name = '关于作者', sort_order = 40
WHERE section_code = 'PROFILE';

UPDATE sr_site_home_section SET enabled = 0, sort_order = 60
WHERE section_code = 'BLOG';

UPDATE sr_site_home_section SET enabled = 0, sort_order = 70
WHERE section_code = 'HOT_CONTENT';
