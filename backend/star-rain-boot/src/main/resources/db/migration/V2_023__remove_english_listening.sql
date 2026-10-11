-- Retire the V2 listening domain. Older Flyway migrations remain immutable.
DELETE FROM sr_media_reference
WHERE (source_module = 'ENGLISH' AND source_type = 'LISTENING_ITEM')
   OR usage_code = 'english.listening-audio';

DROP TABLE IF EXISTS sr_english_listening_segment;
DROP TABLE IF EXISTS sr_english_listening_item;

UPDATE sr_permission
SET description = '查看四个英语方向的内容'
WHERE code = 'english:content-read-admin';

UPDATE sr_permission
SET description = '管理四个英语方向的内容'
WHERE code = 'english:content-edit';

UPDATE sr_english_overview
SET subtitle = REPLACE(subtitle,
        '从可理解输入到清晰表达，按 CEFR 建立阅读、听力与写作的长期学习闭环。',
        '从单词和语法打好基础，通过阅读积累输入，再用写作练习清晰表达。'),
    introduction = REPLACE(introduction,
        '每个等级都以一个真实主题串联阅读、听力和写作：先理解信息，再辨认真实语流，最后用自己的语言完成表达。',
        '学习以单词和语法为基础，再通过分级阅读理解信息、积累表达，最后用写作练习组织自己的语言。')
WHERE id = 1;

UPDATE sr_english_overview
SET roadmap_markdown = REPLACE(roadmap_markdown,
    '3. **收听同主题材料**：先盲听，再结合逐句文本复听。', '')
WHERE id = 1;

UPDATE sr_english_overview
SET roadmap_markdown = REPLACE(roadmap_markdown,
    '4. **完成听力练习**：定位信息、判断态度并训练真实语流识别。', '')
WHERE id = 1;

UPDATE sr_english_overview
SET roadmap_markdown = REPLACE(REPLACE(REPLACE(roadmap_markdown,
    '5. **学习写作资源**', '3. **学习写作资源**'),
    '6. **完成写作任务**', '4. **完成写作任务**'),
    '第一次阅读或收听时', '第一次阅读时')
WHERE id = 1;
