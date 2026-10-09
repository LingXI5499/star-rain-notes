-- Progress belongs to a plan round; mastery and answer versions remain cumulative.
ALTER TABLE sr_study_plan ADD study_round INT NOT NULL DEFAULT 1;
ALTER TABLE sr_study_task ADD study_round INT NOT NULL DEFAULT 1,
 ADD INDEX idx_study_task_round(plan_id,study_round,sequence_no);
ALTER TABLE sr_learning_session ADD study_round INT NOT NULL DEFAULT 1,
 ADD INDEX idx_session_plan_round(account_id,study_plan_id,study_round,chapter_id);
ALTER TABLE sr_study_plan_chapter ADD completed_at DATETIME(3) NULL;
UPDATE sr_study_plan_chapter c JOIN sr_learning_progress p
 ON p.account_id=c.account_id AND p.chapter_id=c.chapter_id
 SET c.completed_at=p.completed_at WHERE p.completed_at IS NOT NULL;
-- Version baselines prevent an old answer (even in the same millisecond) from completing a new round.
CREATE TABLE sr_learning_session_question (
 session_id BIGINT NOT NULL,question_id BIGINT NOT NULL,baseline_version INT NOT NULL DEFAULT 0,
 PRIMARY KEY(session_id,question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO sr_learning_session_question(session_id,question_id,baseline_version)
 SELECT s.id,q.id,0 FROM sr_learning_session s JOIN sr_tutorial_question q ON q.chapter_id=s.chapter_id
 WHERE s.session_type='INITIAL_STUDY' AND s.status='IN_PROGRESS' AND s.study_plan_id IS NOT NULL;
