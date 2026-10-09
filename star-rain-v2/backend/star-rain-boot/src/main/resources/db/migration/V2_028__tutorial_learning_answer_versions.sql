ALTER TABLE sr_user_question_answer
 MODIFY reference_unlocked_at DATETIME(3) NULL,
 ADD latest_version_id BIGINT NULL, ADD version_count INT NOT NULL DEFAULT 0,
 ADD first_answered_at DATETIME(3) NULL, ADD last_answered_at DATETIME(3) NULL;
CREATE TABLE sr_user_question_answer_version (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, answer_id BIGINT NOT NULL,
 version_no INT NOT NULL, answer_text LONGTEXT NOT NULL, answer_phase VARCHAR(24) NOT NULL,
 created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 UNIQUE KEY uk_answer_version(answer_id,version_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO sr_user_question_answer_version(answer_id,version_no,answer_text,answer_phase,created_at)
 SELECT id,1,answer_text,'BEFORE_REFERENCE',first_submitted_at FROM sr_user_question_answer;
UPDATE sr_user_question_answer a JOIN sr_user_question_answer_version v ON v.answer_id=a.id
 SET a.latest_version_id=v.id,a.version_count=1,a.first_answered_at=a.first_submitted_at,a.last_answered_at=a.updated_at;
