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

INSERT IGNORE INTO {{TARGET}}.sr_english_listening_item
(id,title,slug,summary,transcript_markdown,cefr_level,listening_level,duration_seconds,source_name,source_url,
 publish_status,sort_order,published_at,created_at,updated_at)
SELECT id,title,slug,summary,transcript_markdown,cefr_level,listening_level,duration_seconds,source_name,source_url,
       'PUBLISHED',sort_order,COALESCE(published_at,created_at,NOW(3)),created_at,updated_at
FROM {{SOURCE}}.english_listening_item;

INSERT IGNORE INTO {{TARGET}}.sr_english_listening_segment
(id,listening_item_id,start_ms,end_ms,transcript_text,translation_text,sort_order,created_at,updated_at)
SELECT id,listening_item_id,start_ms,end_ms,transcript_text,translation_text,sort_order,created_at,updated_at
FROM {{SOURCE}}.english_listening_segment;

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
UNION ALL SELECT 'grammar_section',COUNT(*) FROM {{TARGET}}.sr_english_grammar_section
UNION ALL SELECT 'grammar_lesson',COUNT(*) FROM {{TARGET}}.sr_english_grammar_lesson
UNION ALL SELECT 'reading',COUNT(*) FROM {{TARGET}}.sr_english_reading_article
UNION ALL SELECT 'listening',COUNT(*) FROM {{TARGET}}.sr_english_listening_item
UNION ALL SELECT 'listening_segment',COUNT(*) FROM {{TARGET}}.sr_english_listening_segment
UNION ALL SELECT 'writing_resource',COUNT(*) FROM {{TARGET}}.sr_english_writing_resource
UNION ALL SELECT 'writing_prompt',COUNT(*) FROM {{TARGET}}.sr_english_writing_prompt;
