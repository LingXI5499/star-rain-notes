package com.starrainnotes.english.learning.infrastructure;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/** Aggregates learning attempts without reading content-domain tables. */
@Repository
public class LearningAnalyticsRepository {
    public record Overview(long activeLearners, long attempts, long completions, long timeSpent) { }
    public record Trend(long attempts, long completions, long activeLearners, long timeSpent) { }
    public record Module(long attempts, long engagedContent, long completions, long timeSpent) {
        public static final Module EMPTY = new Module(0, 0, 0, 0);
    }

    private final JdbcTemplate jdbc;

    public LearningAnalyticsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Overview overview(LocalDateTime since, String type) {
        QueryFilter filter = filter(since, type);
        return jdbc.queryForObject("""
                SELECT COUNT(*) total_attempts,
                       COUNT(DISTINCT learner_id) active_learners,
                       COALESCE(SUM(completion_status='COMPLETED'),0) completions,
                       COALESCE(SUM(time_spent_seconds),0) total_time
                FROM english_learning_attempt
                WHERE attempted_at>=?""" + filter.typeClause(),
                (rs, row) -> new Overview(rs.getLong("active_learners"), rs.getLong("total_attempts"),
                        rs.getLong("completions"), rs.getLong("total_time")), filter.args());
    }

    public Map<LocalDate, Trend> trend(LocalDateTime since, String type, String offset) {
        QueryFilter filter = filter(since, type);
        Object[] args = prepend(offset, filter.args());
        Map<LocalDate, Trend> values = new HashMap<>();
        jdbc.query("""
                SELECT DATE(CONVERT_TZ(attempted_at,'+00:00',?)) activity_day,
                       COUNT(*) attempts,
                       COALESCE(SUM(completion_status='COMPLETED'),0) completions,
                       COUNT(DISTINCT learner_id) active_learners,
                       COALESCE(SUM(time_spent_seconds),0) time_spent
                FROM english_learning_attempt
                WHERE attempted_at>=?""" + filter.typeClause() + """
                GROUP BY activity_day
                ORDER BY activity_day
                """, rs -> {
            while (rs.next()) {
                values.put(rs.getDate("activity_day").toLocalDate(),
                        new Trend(rs.getLong("attempts"), rs.getLong("completions"),
                                rs.getLong("active_learners"), rs.getLong("time_spent")));
            }
            return null;
        }, args);
        return values;
    }

    public Map<String, Module> modules(LocalDateTime since, String type) {
        QueryFilter filter = filter(since, type);
        Map<String, Module> values = new HashMap<>();
        jdbc.query("""
                SELECT content_type,COUNT(*) attempts,COUNT(DISTINCT content_id) engaged_content,
                       COALESCE(SUM(completion_status='COMPLETED'),0) completions,
                       COALESCE(SUM(time_spent_seconds),0) time_spent
                FROM english_learning_attempt
                WHERE attempted_at>=?""" + filter.typeClause() + " GROUP BY content_type", rs -> {
            while (rs.next()) {
                values.put(rs.getString("content_type"),
                        new Module(rs.getLong("attempts"), rs.getLong("engaged_content"),
                                rs.getLong("completions"), rs.getLong("time_spent")));
            }
            return null;
        }, filter.args());
        return values;
    }

    private QueryFilter filter(LocalDateTime since, String type) {
        return "ALL".equals(type) ? new QueryFilter("", new Object[]{since})
                : new QueryFilter(" AND content_type=?", new Object[]{since, type});
    }

    private Object[] prepend(Object first, Object[] values) {
        Object[] result = new Object[values.length + 1];
        result[0] = first;
        System.arraycopy(values, 0, result, 1, values.length);
        return result;
    }

    private record QueryFilter(String typeClause, Object[] args) { }
}
