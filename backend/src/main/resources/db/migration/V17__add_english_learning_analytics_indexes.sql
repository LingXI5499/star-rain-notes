-- Read-optimized indexes for aggregate English learning analytics.
-- No learner content or identity data is copied into a reporting table.
CREATE INDEX idx_learning_attempt_time_module_content
    ON english_learning_attempt(attempted_at, content_type, content_id);

CREATE INDEX idx_learning_record_status_type_updated
    ON english_learning_record(completion_status, content_type, updated_at);
