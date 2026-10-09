-- Evidence is immutable. Legacy review tables remain available for audit.
ALTER TABLE sr_tutorial_knowledge_card ADD COLUMN content_version INT NOT NULL DEFAULT 1;
CREATE TABLE sr_tutorial_question_card (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, question_id BIGINT NOT NULL,
 knowledge_card_id BIGINT NOT NULL, sort_order INT NOT NULL DEFAULT 0,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 UNIQUE KEY uk_question_card(question_id,knowledge_card_id), KEY idx_question_card_card(knowledge_card_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE sr_learning_account_lock (
 account_id BIGINT NOT NULL PRIMARY KEY
) ENGINE=InnoDB;
CREATE TABLE sr_learning_session (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, account_id BIGINT NOT NULL,
 session_type VARCHAR(24) NOT NULL, study_plan_id BIGINT NULL, study_task_id BIGINT NULL,
 tutorial_id BIGINT NULL, chapter_id BIGINT NULL, status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
 requested_item_count INT NULL, summary_json JSON NULL, started_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 completed_at DATETIME(3) NULL, abandoned_at DATETIME(3) NULL,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
 KEY idx_session_current(account_id,status,session_type), KEY idx_session_chapter(account_id,chapter_id,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE sr_learning_session_item (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, session_id BIGINT NOT NULL, account_id BIGINT NOT NULL,
 tutorial_id BIGINT NOT NULL, chapter_id BIGINT NOT NULL, knowledge_card_id BIGINT NOT NULL,
 card_content_version INT NOT NULL, front_text TEXT NOT NULL, back_markdown LONGTEXT NOT NULL,
 source_priority INT NULL, sequence_no INT NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
 revealed_at DATETIME(3) NULL, completed_at DATETIME(3) NULL,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 UNIQUE KEY uk_session_card(session_id,knowledge_card_id), KEY idx_session_item_order(session_id,sequence_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE sr_learning_evidence (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, account_id BIGINT NOT NULL, session_id BIGINT NOT NULL,
 tutorial_id BIGINT NOT NULL, chapter_id BIGINT NOT NULL, knowledge_card_id BIGINT NOT NULL,
 card_content_version INT NOT NULL, evidence_type VARCHAR(24) NOT NULL, rating VARCHAR(20) NOT NULL,
 previous_mastery_status VARCHAR(24) NULL, mastery_status VARCHAR(24) NULL,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 initial_card_id BIGINT GENERATED ALWAYS AS (CASE WHEN evidence_type='INITIAL' THEN knowledge_card_id ELSE NULL END) STORED,
 UNIQUE KEY uk_evidence_session(account_id,session_id,knowledge_card_id),
 UNIQUE KEY uk_evidence_initial(account_id,initial_card_id),
 KEY idx_evidence_card(account_id,knowledge_card_id,created_at,id),
 KEY idx_evidence_account(account_id,created_at), KEY idx_evidence_session(session_id,knowledge_card_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
ALTER TABLE sr_knowledge_mastery
 ADD COLUMN mastery_status VARCHAR(24) NOT NULL DEFAULT 'UNLEARNED',
 ADD COLUMN latest_rating VARCHAR(20) NULL,
 ADD COLUMN forgot_count INT NOT NULL DEFAULT 0,
 ADD COLUMN fuzzy_count INT NOT NULL DEFAULT 0,
 ADD COLUMN remembered_count INT NOT NULL DEFAULT 0,
 ADD COLUMN recent_score INT NOT NULL DEFAULT 0,
 ADD COLUMN latest_evidence_card_version INT NULL;
-- One historical result is one completed session; do not fabricate INITIAL evidence.
INSERT INTO sr_learning_session(account_id,session_type,tutorial_id,chapter_id,status,requested_item_count,started_at,completed_at,created_at)
 SELECT r.account_id,'REVIEW',c.tutorial_id,c.id,'COMPLETED',1,r.created_at,r.created_at,r.created_at
 FROM sr_review_result r JOIN sr_tutorial_knowledge_card k ON k.id=r.knowledge_card_id
 JOIN sr_tutorial_chapter c ON c.id=k.chapter_id ORDER BY r.id;
-- Persist a deterministic result-to-session mapping, including deleted content.
CREATE TABLE sr_learning_legacy_review_map (result_id BIGINT NOT NULL PRIMARY KEY, session_id BIGINT NOT NULL UNIQUE) ENGINE=InnoDB;
INSERT INTO sr_learning_legacy_review_map(result_id,session_id)
 SELECT result_id,session_id FROM
 (SELECT r.id result_id, ROW_NUMBER() OVER(ORDER BY r.id) n FROM sr_review_result r
 JOIN sr_tutorial_knowledge_card k ON k.id=r.knowledge_card_id JOIN sr_tutorial_chapter c ON c.id=k.chapter_id) r
 JOIN (SELECT id session_id,ROW_NUMBER() OVER(ORDER BY id) n FROM sr_learning_session) s ON s.n=r.n;
INSERT INTO sr_learning_evidence(account_id,session_id,tutorial_id,chapter_id,knowledge_card_id,card_content_version,evidence_type,rating,created_at)
 SELECT r.account_id,m.session_id,c.tutorial_id,c.id,r.knowledge_card_id,1,'REVIEW',
 CASE r.recall_rating WHEN 'FORGOT' THEN 'FORGOT' WHEN 'HARD' THEN 'FUZZY' ELSE 'REMEMBERED' END,r.created_at
 FROM sr_review_result r JOIN sr_learning_legacy_review_map m ON m.result_id=r.id
 JOIN sr_tutorial_knowledge_card k ON k.id=r.knowledge_card_id JOIN sr_tutorial_chapter c ON c.id=k.chapter_id ORDER BY r.id;
-- A result whose card no longer exists remains in sr_review_result; no chapter is guessed.
INSERT INTO sr_learning_session_item(session_id,account_id,tutorial_id,chapter_id,knowledge_card_id,card_content_version,front_text,back_markdown,sequence_no,status,revealed_at,completed_at,created_at)
 SELECT e.session_id,e.account_id,e.tutorial_id,e.chapter_id,e.knowledge_card_id,1,k.front_text,k.back_markdown,1,'COMPLETED',e.created_at,e.created_at,e.created_at
 FROM sr_learning_evidence e JOIN sr_tutorial_knowledge_card k ON k.id=e.knowledge_card_id;
-- Recompute from converted facts rather than retaining old self ratings.
UPDATE sr_knowledge_mastery SET mastery_status='UNLEARNED',evidence_count=0,last_evidence_at=NULL;
INSERT INTO sr_knowledge_mastery(account_id,knowledge_card_id,mastery_status,latest_rating,evidence_count,forgot_count,fuzzy_count,remembered_count,recent_score,last_evidence_at,latest_evidence_card_version)
 SELECT account_id,knowledge_card_id,
 CASE WHEN COUNT(*)<3 OR SUM(IF(n<=3,score,0))<=3 THEN 'LEARNING' WHEN SUM(IF(n<=3,score,0))=6 THEN 'STABLE_MASTERED' ELSE 'BASIC_MASTERED' END,
 MAX(IF(n=1,rating,NULL)),COUNT(*),SUM(rating='FORGOT'),SUM(rating='FUZZY'),SUM(rating='REMEMBERED'),SUM(IF(n<=3,score,0)),MAX(created_at),1
 FROM (SELECT e.*,CASE rating WHEN 'REMEMBERED' THEN 2 WHEN 'FUZZY' THEN 1 ELSE 0 END score,
 ROW_NUMBER() OVER(PARTITION BY account_id,knowledge_card_id ORDER BY created_at DESC,id DESC) n FROM sr_learning_evidence e) ranked
 GROUP BY account_id,knowledge_card_id
 ON DUPLICATE KEY UPDATE mastery_status=VALUES(mastery_status),latest_rating=VALUES(latest_rating),evidence_count=VALUES(evidence_count),forgot_count=VALUES(forgot_count),fuzzy_count=VALUES(fuzzy_count),remembered_count=VALUES(remembered_count),recent_score=VALUES(recent_score),last_evidence_at=VALUES(last_evidence_at),latest_evidence_card_version=1;
