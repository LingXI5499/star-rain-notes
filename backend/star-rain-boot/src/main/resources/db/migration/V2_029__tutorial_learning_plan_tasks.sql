ALTER TABLE sr_study_plan ADD target_cards_per_task INT NOT NULL DEFAULT 12,
 MODIFY scope_type VARCHAR(20) NULL, MODIFY scope_id BIGINT NULL,
 MODIFY start_date DATE NULL, MODIFY study_weekdays_json JSON NULL;
ALTER TABLE sr_study_task ADD group_id BIGINT NULL, ADD card_count INT NOT NULL DEFAULT 0,
 ADD question_count INT NOT NULL DEFAULT 0, MODIFY target_type VARCHAR(30) NULL,
 MODIFY target_id BIGINT NULL, MODIFY task_date DATE NULL, MODIFY generation_version INT NULL;
CREATE TABLE sr_study_plan_chapter (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,plan_id BIGINT NOT NULL,account_id BIGINT NOT NULL,
 tutorial_id BIGINT NOT NULL,group_id BIGINT NOT NULL,chapter_id BIGINT NOT NULL,
 group_order INT NOT NULL,chapter_order INT NOT NULL,card_count INT NOT NULL,question_count INT NOT NULL,
 chapter_title VARCHAR(300) NOT NULL,created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 UNIQUE KEY uk_plan_chapter(plan_id,chapter_id),KEY idx_plan_chapter_conflict(account_id,chapter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE sr_study_task_chapter (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,study_task_id BIGINT NOT NULL,chapter_id BIGINT NOT NULL,
 sequence_no INT NOT NULL,card_count INT NOT NULL,question_count INT NOT NULL,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),UNIQUE KEY uk_task_chapter(study_task_id,chapter_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- Preserve old task identities and order; freeze their original chapter scope.
INSERT INTO sr_study_plan_chapter(plan_id,account_id,tutorial_id,group_id,chapter_id,group_order,chapter_order,card_count,question_count,chapter_title)
 SELECT DISTINCT t.plan_id,t.account_id,t.tutorial_id,c.group_id,c.id,g.sort_order,c.sort_order,
 (SELECT COUNT(*) FROM sr_tutorial_knowledge_card k WHERE k.chapter_id=c.id AND k.status='ENABLED'),
 (SELECT COUNT(*) FROM sr_tutorial_question q WHERE q.chapter_id=c.id AND q.status='ENABLED'),c.title
 FROM sr_study_task t JOIN sr_tutorial_chapter c ON c.id=t.chapter_id JOIN sr_tutorial_group g ON g.id=c.group_id;
INSERT INTO sr_study_task_chapter(study_task_id,chapter_id,sequence_no,card_count,question_count)
 SELECT t.id,t.chapter_id,1,p.card_count,p.question_count FROM sr_study_task t JOIN sr_study_plan_chapter p ON p.plan_id=t.plan_id AND p.chapter_id=t.chapter_id;
UPDATE sr_study_task t JOIN sr_study_plan_chapter p ON p.plan_id=t.plan_id AND p.chapter_id=t.chapter_id
 SET t.group_id=p.group_id,t.card_count=p.card_count,t.question_count=p.question_count;
UPDATE sr_study_task SET status='PENDING' WHERE status IN ('TODO','OVERDUE');
CREATE TABLE sr_learning_legacy_completion AS SELECT account_id,chapter_id,completed_at FROM sr_learning_progress WHERE completed_at IS NOT NULL;
CREATE TABLE sr_learning_legacy_task_state AS SELECT id,plan_id,status,completed_at FROM sr_study_task;
-- Legacy completion was a reading action. New completion must be established by evidence.
UPDATE sr_study_task SET status='PENDING',completed_at=NULL WHERE status='COMPLETED';
UPDATE sr_learning_progress SET completed_at=NULL WHERE completed_at IS NOT NULL;
CREATE TABLE sr_learning_legacy_plan_state AS SELECT id,account_id,status FROM sr_study_plan;
UPDATE sr_study_plan SET status='PAUSED' WHERE status='ACTIVE';
