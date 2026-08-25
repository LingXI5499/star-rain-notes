-- Star Rain Notes — V23__create_account_personal_learning.sql
-- 阶段四 · 个人英语数据：learner profile 关联账号 + 账号型单词记忆表。
CREATE TABLE account_vocabulary_memory (
    account_id BIGINT NOT NULL,
    word_id BIGINT NOT NULL,
    memory_count INT NOT NULL DEFAULT 0,
    last_memory_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (account_id, word_id),
    CONSTRAINT fk_acct_vocab_account FOREIGN KEY (account_id)
        REFERENCES user_account(id) ON DELETE CASCADE,
    CONSTRAINT fk_acct_vocab_word FOREIGN KEY (word_id)
        REFERENCES vocabulary_word(id) ON DELETE CASCADE,
    CONSTRAINT ck_acct_vocab_count CHECK (memory_count >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;