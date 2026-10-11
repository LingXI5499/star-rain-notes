-- V2_010: 教程个人学习域（LRN-001 ~ LRN-025）
-- 个人数据只按当前账户读取与写入；关联均为逻辑外键。
SET NAMES utf8mb4;

-- B. Personal Learning Domain
-- ---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS sr_learning_progress (
    id                      BIGINT NOT NULL AUTO_INCREMENT,
    account_id              BIGINT NOT NULL COMMENT '逻辑关联 Account',
    tutorial_id             BIGINT NOT NULL,
    group_id                BIGINT NOT NULL,
    chapter_id              BIGINT NOT NULL,
    scroll_anchor           VARCHAR(255) NULL COMMENT '最近阅读锚点/位置标识',
    progress_ratio          DECIMAL(5,4) NULL COMMENT '0~1 阅读位置提示，不作为完成事实',
    study_seconds_total     BIGINT NOT NULL DEFAULT 0,
    completed_at            DATETIME(3) NULL,
    last_studied_at         DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                            ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_learning_progress_account_chapter (account_id, chapter_id),
    KEY idx_sr_learning_progress_recent (account_id, last_studied_at),
    KEY idx_sr_learning_progress_tutorial (account_id, tutorial_id, completed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='用户章节学习进度';

CREATE TABLE IF NOT EXISTS sr_user_question_answer (
    id                      BIGINT NOT NULL AUTO_INCREMENT,
    account_id              BIGINT NOT NULL,
    question_id             BIGINT NOT NULL COMMENT '逻辑关联 sr_tutorial_question.id',
    answer_text             LONGTEXT NOT NULL,
    first_submitted_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    reference_unlocked_at   DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                            ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_user_question_answer (account_id, question_id),
    KEY idx_sr_user_question_answer_account (account_id, updated_at),
    KEY idx_sr_user_question_answer_question (question_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='登录用户章节问题答案';

CREATE TABLE IF NOT EXISTS sr_study_plan (
    id                      BIGINT NOT NULL AUTO_INCREMENT,
    account_id              BIGINT NOT NULL,
    tutorial_id             BIGINT NOT NULL,
    name                    VARCHAR(160) NOT NULL,
    scope_type              VARCHAR(20) NOT NULL COMMENT 'TUTORIAL/GROUP/CHAPTER',
    scope_id                BIGINT NOT NULL COMMENT '对应 scope_type 的业务ID',
    start_date              DATE NOT NULL,
    end_date                DATE NULL,
    study_weekdays_json     JSON NOT NULL COMMENT '计划学习日，如 [1,2,3,4,5]',
    daily_target_minutes    INT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
                            COMMENT 'DRAFT/ACTIVE/PAUSED/COMPLETED/CANCELLED',
    generated_version       INT NOT NULL DEFAULT 0 COMMENT '任务重算版本',
    created_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                            ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    KEY idx_sr_study_plan_account_status (account_id, status, updated_at),
    KEY idx_sr_study_plan_tutorial (account_id, tutorial_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='个人学习计划';

CREATE TABLE IF NOT EXISTS sr_study_task (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    plan_id             BIGINT NOT NULL COMMENT '逻辑关联 sr_study_plan.id',
    account_id          BIGINT NOT NULL,
    tutorial_id         BIGINT NOT NULL,
    chapter_id          BIGINT NULL,
    target_type         VARCHAR(30) NOT NULL COMMENT 'CHAPTER/CARD_SET',
    target_id           BIGINT NOT NULL,
    task_date           DATE NOT NULL,
    sequence_no         INT NOT NULL DEFAULT 0,
    status              VARCHAR(20) NOT NULL DEFAULT 'TODO'
                        COMMENT 'TODO/IN_PROGRESS/COMPLETED/SKIPPED/OVERDUE',
    generation_version  INT NOT NULL,
    started_at          DATETIME(3) NULL,
    completed_at        DATETIME(3) NULL,
    created_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                        ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_study_task_identity (plan_id, generation_version, target_type, target_id),
    KEY idx_sr_study_task_today (account_id, task_date, status, sequence_no),
    KEY idx_sr_study_task_plan (plan_id, status, task_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='学习任务';

CREATE TABLE IF NOT EXISTS sr_knowledge_mastery (
    id                      BIGINT NOT NULL AUTO_INCREMENT,
    account_id              BIGINT NOT NULL,
    knowledge_card_id       BIGINT NOT NULL COMMENT '逻辑关联知识卡片',
    system_suggested_level  VARCHAR(10) NOT NULL DEFAULT 'L1' COMMENT 'L1/L2/L3/L4',
    user_self_level         VARCHAR(10) NULL COMMENT '用户自评 L1/L2/L3/L4',
    last_recall_rating      VARCHAR(20) NULL COMMENT 'FORGOT/HARD/NORMAL/EASY',
    evidence_count          INT NOT NULL DEFAULT 0,
    first_learned_at        DATETIME(3) NULL,
    last_evidence_at        DATETIME(3) NULL,
    created_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                            ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_mastery_account_card (account_id, knowledge_card_id),
    KEY idx_sr_mastery_account_level (account_id, system_suggested_level, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='知识掌握状态';

CREATE TABLE IF NOT EXISTS sr_review_schedule (
    id                      BIGINT NOT NULL AUTO_INCREMENT,
    account_id              BIGINT NOT NULL,
    knowledge_card_id       BIGINT NOT NULL,
    step_index              INT NOT NULL DEFAULT 0,
    current_interval_days   INT NOT NULL DEFAULT 1,
    next_review_at          DATETIME(3) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                            COMMENT 'ACTIVE/PAUSED/COMPLETED',
    last_result_at          DATETIME(3) NULL,
    created_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                            ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_review_schedule_account_card (account_id, knowledge_card_id),
    KEY idx_sr_review_schedule_due (status, next_review_at),
    KEY idx_sr_review_schedule_account_due (account_id, status, next_review_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='知识卡片复习计划';

CREATE TABLE IF NOT EXISTS sr_review_task (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    schedule_id         BIGINT NOT NULL COMMENT '逻辑关联 sr_review_schedule.id',
    account_id          BIGINT NOT NULL,
    knowledge_card_id   BIGINT NOT NULL,
    due_at              DATETIME(3) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                        COMMENT 'PENDING/COMPLETED/OVERDUE',
    generated_at        DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    completed_at        DATETIME(3) NULL,
    created_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_review_task_schedule_due (schedule_id, due_at),
    KEY idx_sr_review_task_account_due (account_id, status, due_at),
    KEY idx_sr_review_task_card (knowledge_card_id, status, due_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='复习任务';

CREATE TABLE IF NOT EXISTS sr_review_result (
    id                          BIGINT NOT NULL AUTO_INCREMENT,
    review_task_id              BIGINT NOT NULL,
    schedule_id                 BIGINT NOT NULL,
    account_id                  BIGINT NOT NULL,
    knowledge_card_id           BIGINT NOT NULL,
    recall_rating               VARCHAR(20) NOT NULL COMMENT 'FORGOT/HARD/NORMAL/EASY',
    previous_interval_days      INT NOT NULL,
    next_interval_days          INT NOT NULL,
    previous_next_review_at     DATETIME(3) NOT NULL,
    calculated_next_review_at   DATETIME(3) NOT NULL,
    created_at                  DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_review_result_task (review_task_id),
    KEY idx_sr_review_result_account (account_id, created_at),
    KEY idx_sr_review_result_card (knowledge_card_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='单次复习结果';

CREATE TABLE IF NOT EXISTS sr_learning_history (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    account_id          BIGINT NOT NULL,
    event_type          VARCHAR(50) NOT NULL,
    tutorial_id         BIGINT NULL,
    group_id            BIGINT NULL,
    chapter_id          BIGINT NULL,
    knowledge_card_id   BIGINT NULL,
    study_task_id       BIGINT NULL,
    review_task_id      BIGINT NULL,
    detail_json         JSON NULL,
    occurred_at         DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    KEY idx_sr_learning_history_account_time (account_id, occurred_at),
    KEY idx_sr_learning_history_account_tutorial (account_id, tutorial_id, occurred_at),
    KEY idx_sr_learning_history_event (event_type, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='个人学习事件历史';
