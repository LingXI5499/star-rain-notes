package com.starrainnotes.english.learning.infrastructure;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
public class RecommendationRepository {
    private final JdbcTemplate jdbc;

    public RecommendationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record ReviewRecord(String contentType, long contentId, String completionStatus,
                               BigDecimal mastery, LocalDateTime nextReviewAt, LocalDateTime updatedAt) { }
    public record ContentKey(String type, long id) { }
    public record CompletedSource(String type, long id, LocalDateTime updatedAt) { }

    public List<ReviewRecord> reviewRecords(long learnerId) {
        return jdbc.query("""
                SELECT r.content_type,r.content_id,r.completion_status,r.mastery_level,
                       r.next_review_at,r.updated_at
                FROM english_learning_record r
                WHERE r.learner_id=?
                """, (rs, row) -> new ReviewRecord(rs.getString("content_type"), rs.getLong("content_id"),
                rs.getString("completion_status"),
                rs.getBigDecimal("mastery_level"),
                rs.getTimestamp("next_review_at") == null ? null : rs.getTimestamp("next_review_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()), learnerId);
    }

    public Set<ContentKey> studiedContent(long learnerId) {
        return jdbc.query("SELECT content_type,content_id FROM english_learning_record WHERE learner_id=?",
                (rs, row) -> new ContentKey(rs.getString(1), rs.getLong(2)), learnerId)
                .stream().collect(Collectors.toSet());
    }

    public Map<ContentKey, String> completionStatuses(long learnerId) {
        Map<ContentKey, String> statuses = new HashMap<>();
        jdbc.query("SELECT content_type,content_id,completion_status FROM english_learning_record WHERE learner_id=?",
                rs -> {
                    while (rs.next()) {
                        statuses.put(new ContentKey(rs.getString(1), rs.getLong(2)), rs.getString(3));
                    }
                    return null;
                }, learnerId);
        return statuses;
    }

    public List<CompletedSource> completedPairSources(long learnerId) {
        return jdbc.query("""
                SELECT content_type,content_id,updated_at FROM english_learning_record
                WHERE learner_id=? AND completion_status='COMPLETED'
                  AND content_type IN ('READING','LISTENING')
                ORDER BY updated_at DESC
                """, (rs, row) -> new CompletedSource(rs.getString(1), rs.getLong(2),
                rs.getTimestamp(3).toLocalDateTime()), learnerId);
    }

    public List<CompletedSource> completedTaggedSources(long learnerId) {
        return jdbc.query("""
                SELECT content_type,content_id,updated_at FROM english_learning_record
                WHERE learner_id=? AND completion_status='COMPLETED'
                  AND content_type IN ('READING','LISTENING','WRITING')
                ORDER BY updated_at DESC
                """, (rs, row) -> new CompletedSource(rs.getString(1), rs.getLong(2),
                rs.getTimestamp(3).toLocalDateTime()), learnerId);
    }
}
