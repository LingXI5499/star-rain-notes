-- =====================================================================
-- Star Rain Notes — V28__add_vocabulary_word_search_index.sql
-- Prefix indexes for global search over vocabulary_word (LIKE queries).
-- =====================================================================

ALTER TABLE vocabulary_word
    ADD KEY idx_vocabulary_word_word (word(100)),
    ADD KEY idx_vocabulary_word_translation (translation(100));
