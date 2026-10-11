-- Structural retirement only. Existing reading content is NEVER cleared at startup.
-- Before deploying to an existing database: approved scope, backup and restore rehearsal.
ALTER TABLE sr_english_reading_article
  DROP COLUMN reading_level,
  DROP COLUMN cefr_level,
  DROP COLUMN level_assessed;

DROP TABLE sr_english_writing_resource;
DROP TABLE sr_english_writing_prompt;

-- Refresh ONLY unchanged historical homepage defaults. Do not overwrite authored overview content.
UPDATE sr_english_overview
SET introduction = '从单词和语法建立基础，通过真实文章阅读积累输入，再用自主英文创作整理思考与表达。阅读按主题、文体和用途探索；写作从私人草稿开始，逐步修改并保留版本。',
    roadmap_markdown = CONCAT('## 阅读与表达\n\n',
      '1. **积累词汇与语法**：建立可以持续复习的基础。\n',
      '2. **阅读真实文章**：先抓主旨，再观察结构和关键表达。\n',
      '3. **结合中文与人工精读**：按需要查看译文和精选解析。\n',
      '4. **写出自己的想法**：从私人草稿开始，反复修改并保留历史版本。')
WHERE id=1
  AND SHA2(introduction,256)='1ef588f68f3dcea195bf9d490b3020865ac129c030b822e93a563ae8242c8852'
  AND SHA2(roadmap_markdown,256)='acfee952b5ef476d8112c0fbb645a00af11213b1d13ffae49ffed54c38d2bc81';
