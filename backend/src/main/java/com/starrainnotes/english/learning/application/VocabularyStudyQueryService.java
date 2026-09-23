package com.starrainnotes.english.learning.application;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class VocabularyStudyQueryService {
    private final JdbcTemplate jdbc;
    public VocabularyStudyQueryService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public List<Map<String,Object>> memory(long accountId) {
        return jdbc.queryForList("SELECT word_id,memory_count,last_memory_at FROM account_vocabulary_memory WHERE account_id=? ORDER BY word_id",accountId);
    }
}
