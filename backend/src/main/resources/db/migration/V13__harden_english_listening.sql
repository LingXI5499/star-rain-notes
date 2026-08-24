-- =====================================================================
-- Star Rain Notes — V13__harden_english_listening.sql
-- 听力系统硬化：对 V12 采用的 sort_order 默认 0 进行硬化，
-- 规范为 10 并增加正数 CHECK 约束（V12 已执行，不做回改）。
-- 不新增业务表，总表数保持不变。
-- =====================================================================

-- ---------------------------------------------------------------
-- english_listening_item.sort_order
-- ---------------------------------------------------------------
UPDATE english_listening_item SET sort_order = 10 WHERE sort_order <= 0;
ALTER TABLE english_listening_item MODIFY sort_order INT NOT NULL DEFAULT 10;
ALTER TABLE english_listening_item ADD CONSTRAINT ck_listening_item_sort CHECK (sort_order > 0);

-- ---------------------------------------------------------------
-- english_listening_segment.sort_order
-- ---------------------------------------------------------------
UPDATE english_listening_segment SET sort_order = 10 WHERE sort_order <= 0;
ALTER TABLE english_listening_segment MODIFY sort_order INT NOT NULL DEFAULT 10;
ALTER TABLE english_listening_segment ADD CONSTRAINT ck_listening_segment_sort CHECK (sort_order > 0);

-- ---------------------------------------------------------------
-- english_learning_bundle_listening_item.sort_order
-- ---------------------------------------------------------------
UPDATE english_learning_bundle_listening_item SET sort_order = 10 WHERE sort_order <= 0;
ALTER TABLE english_learning_bundle_listening_item MODIFY sort_order INT NOT NULL DEFAULT 10;
ALTER TABLE english_learning_bundle_listening_item ADD CONSTRAINT ck_bundle_listening_sort CHECK (sort_order > 0);

-- ---------------------------------------------------------------
-- english_reading_listening_pair.sort_order
-- ---------------------------------------------------------------
UPDATE english_reading_listening_pair SET sort_order = 10 WHERE sort_order <= 0;
ALTER TABLE english_reading_listening_pair MODIFY sort_order INT NOT NULL DEFAULT 10;
ALTER TABLE english_reading_listening_pair ADD CONSTRAINT ck_reading_listening_pair_sort CHECK (sort_order > 0);

-- ---------------------------------------------------------------
-- english_listening_pronunciation_rule.sort_order
-- ---------------------------------------------------------------
UPDATE english_listening_pronunciation_rule SET sort_order = 10 WHERE sort_order <= 0;
ALTER TABLE english_listening_pronunciation_rule MODIFY sort_order INT NOT NULL DEFAULT 10;
ALTER TABLE english_listening_pronunciation_rule ADD CONSTRAINT ck_pronunciation_rule_sort CHECK (sort_order > 0);
