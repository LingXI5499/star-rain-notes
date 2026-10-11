-- Some legacy results outlive deleted authoring rows. Retain their facts without guessing a chapter.
ALTER TABLE sr_learning_evidence MODIFY tutorial_id BIGINT NULL, MODIFY chapter_id BIGINT NULL;
ALTER TABLE sr_learning_session ADD legacy_review_result_id BIGINT NULL,
 ADD UNIQUE KEY uk_session_legacy_result(legacy_review_result_id);
UPDATE sr_learning_session s JOIN sr_learning_legacy_review_map m ON m.session_id=s.id
 SET s.legacy_review_result_id=m.result_id;
INSERT INTO sr_learning_session(account_id,session_type,tutorial_id,chapter_id,status,requested_item_count,started_at,completed_at,created_at,legacy_review_result_id)
 SELECT r.account_id,'REVIEW',c.tutorial_id,c.id,'COMPLETED',1,r.created_at,r.created_at,r.created_at,r.id
 FROM sr_review_result r LEFT JOIN sr_learning_legacy_review_map m ON m.result_id=r.id
 LEFT JOIN sr_tutorial_knowledge_card k ON k.id=r.knowledge_card_id LEFT JOIN sr_tutorial_chapter c ON c.id=k.chapter_id
 WHERE m.result_id IS NULL ORDER BY r.id;
INSERT INTO sr_learning_legacy_review_map(result_id,session_id)
 SELECT s.legacy_review_result_id,s.id FROM sr_learning_session s
 LEFT JOIN sr_learning_legacy_review_map m ON m.result_id=s.legacy_review_result_id
 WHERE s.legacy_review_result_id IS NOT NULL AND m.result_id IS NULL;
INSERT INTO sr_learning_evidence(account_id,session_id,tutorial_id,chapter_id,knowledge_card_id,card_content_version,evidence_type,rating,created_at)
 SELECT r.account_id,m.session_id,c.tutorial_id,c.id,r.knowledge_card_id,1,'REVIEW',
 CASE r.recall_rating WHEN 'FORGOT' THEN 'FORGOT' WHEN 'HARD' THEN 'FUZZY' ELSE 'REMEMBERED' END,r.created_at
 FROM sr_review_result r JOIN sr_learning_legacy_review_map m ON m.result_id=r.id
 LEFT JOIN sr_tutorial_knowledge_card k ON k.id=r.knowledge_card_id LEFT JOIN sr_tutorial_chapter c ON c.id=k.chapter_id
 WHERE NOT EXISTS(SELECT 1 FROM sr_learning_evidence e WHERE e.session_id=m.session_id AND e.knowledge_card_id=r.knowledge_card_id);
INSERT INTO sr_knowledge_mastery(account_id,knowledge_card_id,mastery_status,latest_rating,evidence_count,forgot_count,fuzzy_count,remembered_count,recent_score,last_evidence_at,latest_evidence_card_version)
 SELECT account_id,knowledge_card_id,
 CASE WHEN COUNT(*)<3 OR SUM(IF(n<=3,score,0))<=3 THEN 'LEARNING' WHEN SUM(IF(n<=3,score,0))=6 THEN 'STABLE_MASTERED' ELSE 'BASIC_MASTERED' END,
 MAX(IF(n=1,rating,NULL)),COUNT(*),SUM(rating='FORGOT'),SUM(rating='FUZZY'),SUM(rating='REMEMBERED'),SUM(IF(n<=3,score,0)),MAX(created_at),MAX(IF(n=1,card_content_version,NULL))
 FROM (SELECT e.*,CASE rating WHEN 'REMEMBERED' THEN 2 WHEN 'FUZZY' THEN 1 ELSE 0 END score,
 ROW_NUMBER() OVER(PARTITION BY account_id,knowledge_card_id ORDER BY created_at DESC,id DESC) n FROM sr_learning_evidence e) ranked
 GROUP BY account_id,knowledge_card_id
 ON DUPLICATE KEY UPDATE mastery_status=VALUES(mastery_status),latest_rating=VALUES(latest_rating),evidence_count=VALUES(evidence_count),forgot_count=VALUES(forgot_count),fuzzy_count=VALUES(fuzzy_count),remembered_count=VALUES(remembered_count),recent_score=VALUES(recent_score),last_evidence_at=VALUES(last_evidence_at),latest_evidence_card_version=VALUES(latest_evidence_card_version);
