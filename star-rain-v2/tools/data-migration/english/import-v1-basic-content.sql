-- One-time local V1 -> V2 English content import template.
-- The runner substitutes {{SOURCE}} and {{TARGET}} after validating database identifiers.
-- INSERT IGNORE keeps reruns from replacing content edited in V2 after the first import.
-- V1 media IDs are intentionally not copied: media assets have separate identities in V2.

UPDATE {{TARGET}}.sr_english_overview target
JOIN {{SOURCE}}.english_overview source ON source.id=target.id
SET target.title=source.title, target.subtitle=source.subtitle,
    target.introduction=source.introduction, target.roadmap_markdown=source.roadmap_markdown
WHERE target.introduction IS NULL AND target.roadmap_markdown IS NULL;

INSERT IGNORE INTO {{TARGET}}.sr_english_vocabulary_theme
(id,layer,layer_order,name,sort_order,created_at,updated_at)
SELECT id,layer,layer_order,name,sort_order,created_at,updated_at
FROM {{SOURCE}}.vocabulary_theme;

INSERT IGNORE INTO {{TARGET}}.sr_english_vocabulary_word
(id,theme_id,word,part_of_speech,phonetic_us,phonetic_uk,translation,examples,sort_order,created_at,updated_at)
SELECT id,theme_id,word,part_of_speech,phonetic_us,phonetic_uk,translation,examples,sort_order,created_at,updated_at
FROM {{SOURCE}}.vocabulary_word;

-- 词卡补充列：scene_meaning / inflections / memory_count / last_memory_at。
-- 用 COALESCE / GREATEST 而不是直接赋值，重跑不会覆盖导入后在 V2 手工编辑的值，
-- 计数也只增不减（V1 与 V2 两边都可能继续累加）。
UPDATE {{TARGET}}.sr_english_vocabulary_word target
JOIN {{SOURCE}}.vocabulary_word source ON source.id=target.id
SET target.scene_meaning=COALESCE(target.scene_meaning,source.scene_meaning),
    target.inflections=COALESCE(target.inflections,source.inflections),
    target.memory_count=GREATEST(target.memory_count,COALESCE(source.memory_count,0)),
    target.last_memory_at=COALESCE(target.last_memory_at,source.last_memory_at)
WHERE target.scene_meaning IS NULL OR target.inflections IS NULL
   OR target.memory_count < COALESCE(source.memory_count,0);

-- 授权发音音频。
-- 关键：V1 的 media_asset_id 在 V2 没有等价身份（媒体资产按库各自生成 ID），
-- 因此 media_asset_id 一律留空，只保留 source_url / license_note / provider / is_primary，
-- 需要真人发音时在 V2 媒体库重新上传后回填 media_asset_id。
-- 幂等靠「同词 + 同口音 + 同来源地址」的 LEFT JOIN 判定：media_asset_id 为 NULL 时
-- 唯一键 uk_sr_english_vocab_audio_media 不生效（MySQL 唯一索引不约束 NULL），
-- 单靠 INSERT IGNORE 会在重跑时插入重复行。
INSERT IGNORE INTO {{TARGET}}.sr_english_vocabulary_word_audio
(word_id,accent,media_asset_id,provider,source_url,license_note,is_primary,created_at,updated_at)
SELECT source_audio.word_id,source_audio.accent,NULL,source_audio.provider,source_audio.source_url,
       CONCAT('V1 导入；V1 媒体 #',source_audio.media_asset_id,
              ' 在 V2 无对应资产，需重新上传后回填 media_asset_id。原授权说明：',
              COALESCE(NULLIF(source_audio.license_note,''),'未标注')),
       source_audio.is_primary,source_audio.created_at,source_audio.updated_at
FROM {{SOURCE}}.vocabulary_word_audio source_audio
LEFT JOIN {{TARGET}}.sr_english_vocabulary_word_audio target_audio
    ON target_audio.word_id=source_audio.word_id
   AND target_audio.accent=source_audio.accent
   AND target_audio.source_url <=> source_audio.source_url
WHERE target_audio.id IS NULL;

UPDATE {{TARGET}}.sr_english_grammar_course target
JOIN {{SOURCE}}.english_grammar_course source ON source.id=target.id
SET target.title=source.title,target.subtitle=source.subtitle,target.summary=source.summary,
    target.introduction=source.introduction,target.roadmap_markdown=source.roadmap_markdown,
    target.publish_status='PUBLISHED',target.published_at=COALESCE(source.published_at,NOW(3))
WHERE target.publish_status='DRAFT' AND target.introduction IS NULL;

INSERT IGNORE INTO {{TARGET}}.sr_english_grammar_section
(id,course_id,title,sort_order,created_at,updated_at)
SELECT id,course_id,title,sort_order,created_at,updated_at
FROM {{SOURCE}}.english_grammar_section;

INSERT IGNORE INTO {{TARGET}}.sr_english_grammar_lesson
(id,course_id,section_id,title,slug,summary,body_markdown,publish_status,sort_order,published_at,created_at,updated_at)
SELECT id,course_id,section_id,title,slug,summary,body_markdown,'PUBLISHED',sort_order,
       COALESCE(published_at,created_at,NOW(3)),created_at,updated_at
FROM {{SOURCE}}.english_grammar_lesson;

INSERT IGNORE INTO {{TARGET}}.sr_english_reading_article
(id,title,slug,summary,body_markdown,reading_level,cefr_level,source_name,source_url,publish_status,
 sort_order,published_at,created_at,updated_at)
SELECT id,title,slug,summary,body_markdown,reading_level,cefr_level,source_name,source_url,'PUBLISHED',
       sort_order,COALESCE(published_at,created_at,NOW(3)),created_at,updated_at
FROM {{SOURCE}}.english_reading_article;

INSERT IGNORE INTO {{TARGET}}.sr_english_writing_resource
(id,resource_kind,title,slug,summary,body_markdown,cefr_level,publish_status,sort_order,published_at,created_at,updated_at)
SELECT id,resource_kind,title,slug,summary,body_markdown,cefr_level,'PUBLISHED',sort_order,
       COALESCE(published_at,created_at,NOW(3)),created_at,updated_at
FROM {{SOURCE}}.english_writing_resource;

INSERT IGNORE INTO {{TARGET}}.sr_english_writing_prompt
(id,title,slug,summary,background_markdown,requirements_markdown,cefr_level,word_min,word_max,
 estimated_minutes,publish_status,sort_order,published_at,created_at,updated_at)
SELECT id,title,slug,summary,background_markdown,requirements_markdown,cefr_level,word_min,word_max,
       estimated_minutes,'PUBLISHED',sort_order,COALESCE(published_at,created_at,NOW(3)),created_at,updated_at
FROM {{SOURCE}}.english_writing_prompt;

SELECT 'vocabulary_theme' domain,COUNT(*) rows_imported FROM {{TARGET}}.sr_english_vocabulary_theme
UNION ALL SELECT 'vocabulary_word',COUNT(*) FROM {{TARGET}}.sr_english_vocabulary_word
UNION ALL SELECT 'vocabulary_word_with_scene_meaning',COUNT(*) FROM {{TARGET}}.sr_english_vocabulary_word
    WHERE scene_meaning IS NOT NULL OR inflections IS NOT NULL
UNION ALL SELECT 'vocabulary_word_audio',COUNT(*) FROM {{TARGET}}.sr_english_vocabulary_word_audio
UNION ALL SELECT 'vocabulary_word_audio_with_media',COUNT(*) FROM {{TARGET}}.sr_english_vocabulary_word_audio
    WHERE media_asset_id IS NOT NULL
UNION ALL SELECT 'grammar_section',COUNT(*) FROM {{TARGET}}.sr_english_grammar_section
UNION ALL SELECT 'grammar_lesson',COUNT(*) FROM {{TARGET}}.sr_english_grammar_lesson
UNION ALL SELECT 'reading',COUNT(*) FROM {{TARGET}}.sr_english_reading_article
UNION ALL SELECT 'writing_resource',COUNT(*) FROM {{TARGET}}.sr_english_writing_resource
UNION ALL SELECT 'writing_prompt',COUNT(*) FROM {{TARGET}}.sr_english_writing_prompt;
