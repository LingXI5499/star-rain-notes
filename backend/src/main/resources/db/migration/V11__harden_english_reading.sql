-- Stage 2 acceptance hardening. V10 has already been applied locally, so the
-- ownership constraint is added in a forward-only migration.
ALTER TABLE english_reading_article_exercise
    ADD CONSTRAINT uk_reading_exercise_single_owner UNIQUE (exercise_id);
