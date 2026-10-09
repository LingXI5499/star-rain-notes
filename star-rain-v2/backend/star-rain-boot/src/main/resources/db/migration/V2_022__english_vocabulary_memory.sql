-- V2_022: 英语词汇记忆体系（词卡字段补全 + 账户级记忆/复习/设置/音频）
-- 迁移来源：V1 star_rain_notes 的 vocabulary_word 四列与 account_vocabulary_* / vocabulary_word_audio。
-- 个人数据只按当前账户读写；所有跨表关联均为逻辑外键，不建物理外键（与既有迁移一致）。
SET NAMES utf8mb4;

-- A. 词卡字段补全
-- V1 的 vocabulary_word 比 V2 多出 scene_meaning / inflections / memory_count / last_memory_at，
-- 词卡（本主题用法、词形、记忆次数）必须用到，因此补到 V2 词表上。
-- memory_count / last_memory_at 在 V2 只作为内容侧的编辑依据，个人记忆次数另存于
-- sr_english_vocabulary_memory，二者不可混用。
-- ---------------------------------------------------------------------
ALTER TABLE sr_english_vocabulary_word
    ADD COLUMN scene_meaning VARCHAR(1000) NULL COMMENT '本主题用法' AFTER translation,
    ADD COLUMN inflections VARCHAR(1000) NULL COMMENT '词形变化' AFTER scene_meaning,
    ADD COLUMN memory_count INT NOT NULL DEFAULT 0 COMMENT '内容侧记忆次数（非个人进度）' AFTER examples,
    ADD COLUMN last_memory_at DATETIME(3) NULL COMMENT '内容侧最近记忆时间' AFTER memory_count;


-- B. 词卡显示偏好
-- 只保存「单卡覆盖全局」的显式选择；跟随全局时不留行。
-- ---------------------------------------------------------------------
CREATE TABLE sr_english_vocabulary_card_preference (
    account_id      BIGINT NOT NULL COMMENT '逻辑关联 sr_account.id',
    word_id         BIGINT NOT NULL COMMENT '逻辑关联 sr_english_vocabulary_word.id',
    display_mode    VARCHAR(16) NOT NULL COMMENT 'BILINGUAL/ENGLISH_ONLY/CHINESE_ONLY',
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                    ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (account_id, word_id),
    KEY idx_sr_english_vocab_card_word (word_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='英语词卡显示偏好（单卡覆盖全局）';


-- C. 个人记忆状态
-- learning_status：NEW（已建行但未开始）/ ACTIVE（计划内）/ PAUSED（暂停）。
-- review_step 与 review_count 分开保留：review_count 是真实复习次数，只会增长；
-- review_step 是固定间隔表的档位，上限 10，与 V1 口径一致。
-- ---------------------------------------------------------------------
CREATE TABLE sr_english_vocabulary_memory (
    account_id          BIGINT NOT NULL COMMENT '逻辑关联 sr_account.id',
    word_id             BIGINT NOT NULL COMMENT '逻辑关联 sr_english_vocabulary_word.id',
    memory_count        INT NOT NULL DEFAULT 0 COMMENT '累计记忆次数',
    review_step         INT NOT NULL DEFAULT 0 COMMENT '固定间隔档位 0~10',
    review_count        INT NOT NULL DEFAULT 0 COMMENT '真实复习次数',
    last_memory_at      DATETIME(3) NULL,
    first_learned_at    DATETIME(3) NULL,
    last_reviewed_at    DATETIME(3) NULL,
    next_review_at      DATETIME(3) NULL,
    learning_status     VARCHAR(16) NOT NULL DEFAULT 'NEW' COMMENT 'NEW/ACTIVE/PAUSED',
    created_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                        ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (account_id, word_id),
    KEY idx_sr_english_vocab_due (account_id, learning_status, next_review_at, word_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='英语单词个人记忆状态';


-- D. 复习日志
-- 每次「完成本次记忆」写一行；review_session_id 是前端生成的幂等键，
-- 唯一键 (account_id, review_session_id) 保证重放不会重复计数。
-- timing_status：NEW（首次）/ EARLY / ON_TIME / OVERDUE，口径见 VocabularyReviewPolicy。
-- ---------------------------------------------------------------------
CREATE TABLE sr_english_vocabulary_review_log (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    account_id          BIGINT NOT NULL COMMENT '逻辑关联 sr_account.id',
    word_id             BIGINT NOT NULL COMMENT '逻辑关联 sr_english_vocabulary_word.id',
    review_session_id   CHAR(36) NOT NULL COMMENT '前端生成的幂等复习标识',
    direction           VARCHAR(16) NOT NULL COMMENT 'EN_TO_ZH/ZH_TO_EN',
    review_number       INT NOT NULL COMMENT '第几次复习',
    scheduled_at        DATETIME(3) NULL COMMENT '计划复习时间（首次为空）',
    reviewed_at         DATETIME(3) NOT NULL COMMENT '实际复习时间',
    interval_seconds    BIGINT NOT NULL COMMENT '本次采用的固定间隔秒数',
    timing_status       VARCHAR(16) NOT NULL COMMENT 'NEW/EARLY/ON_TIME/OVERDUE',
    created_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_english_vocab_review_session (account_id, review_session_id),
    KEY idx_sr_english_vocab_review_history (account_id, word_id, reviewed_at, id),
    KEY idx_sr_english_vocab_review_daily (account_id, reviewed_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='英语单词真实复习记录';


-- E. 学习设置
-- review_direction：EN_TO_ZH 英译中 / ZH_TO_EN 中译英 / MIXED 随机混合。
-- ---------------------------------------------------------------------
CREATE TABLE sr_english_vocabulary_study_setting (
    account_id          BIGINT NOT NULL COMMENT '逻辑关联 sr_account.id',
    show_english        TINYINT(1) NOT NULL DEFAULT 1 COMMENT '全局词卡显示英文',
    show_chinese        TINYINT(1) NOT NULL DEFAULT 1 COMMENT '全局词卡显示中文',
    review_direction    VARCHAR(16) NOT NULL DEFAULT 'MIXED' COMMENT 'EN_TO_ZH/ZH_TO_EN/MIXED',
    daily_new_limit     INT NOT NULL DEFAULT 20 COMMENT '每日新词上限 0~200',
    daily_review_limit  INT NOT NULL DEFAULT 200 COMMENT '每日复习上限 1~1000',
    updated_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                        ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='英语单词学习设置';


-- F. 授权发音音频
-- media_asset_id 逻辑关联 sr_media_asset.id，可为空：V1 的 media_asset_id 在 V2 没有
-- 等价身份（媒体身份按库各自生成），导入时一律留空并保留 source_url / license_note，
-- 需要真人发音时在 V2 媒体库重新上传后回填。
-- provider：UPLOADED（本地上传）/ LICENSED_API（授权接口）/ OTHER。
-- ---------------------------------------------------------------------
CREATE TABLE sr_english_vocabulary_word_audio (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    word_id         BIGINT NOT NULL COMMENT '逻辑关联 sr_english_vocabulary_word.id',
    accent          VARCHAR(8) NOT NULL COMMENT 'US/UK',
    media_asset_id  BIGINT NULL COMMENT '逻辑关联 sr_media_asset.id；V1 媒体 ID 不可直接映射',
    provider        VARCHAR(40) NOT NULL DEFAULT 'UPLOADED' COMMENT 'UPLOADED/LICENSED_API/OTHER',
    source_url      VARCHAR(500) NULL,
    license_note    VARCHAR(500) NOT NULL DEFAULT '',
    is_primary      TINYINT(1) NOT NULL DEFAULT 0,
    created_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
                    ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uk_sr_english_vocab_audio_media (word_id, media_asset_id),
    KEY idx_sr_english_vocab_audio_primary (word_id, accent, is_primary, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='英语单词授权发音音频';
