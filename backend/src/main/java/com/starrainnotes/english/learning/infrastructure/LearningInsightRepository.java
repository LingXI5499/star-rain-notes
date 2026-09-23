package com.starrainnotes.english.learning.infrastructure;

import com.starrainnotes.english.learning.dto.LearningActivityDayView;
import com.starrainnotes.english.learning.dto.LearningModuleInsightView;
import com.starrainnotes.english.learning.dto.LearningSummaryView;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class LearningInsightRepository {
    private final JdbcTemplate jdbc;
    private final SiteSettingsTimezone timezone;
    private final LearningRecordRepository records;

    public LearningInsightRepository(JdbcTemplate jdbc, SiteSettingsTimezone timezone, LearningRecordRepository records) {
        this.jdbc = jdbc;
        this.timezone = timezone;
        this.records = records;
    }

    public LearningSummaryView summary(long learnerId) {
        long total = count("SELECT COUNT(*) FROM english_learning_record WHERE learner_id=?", learnerId);
        long progress = count("SELECT COUNT(*) FROM english_learning_record WHERE learner_id=? AND completion_status='IN_PROGRESS'", learnerId);
        long completed = count("SELECT COUNT(*) FROM english_learning_record WHERE learner_id=? AND completion_status='COMPLETED'", learnerId);
        long due = count("SELECT COUNT(*) FROM english_learning_record WHERE learner_id=? AND next_review_at<=UTC_TIMESTAMP(6)", learnerId);
        Map<String,Long> byType = new LinkedHashMap<>();
        jdbc.query("SELECT content_type,COUNT(*) total FROM english_learning_record WHERE learner_id=? AND completion_status='COMPLETED' GROUP BY content_type",
                rs -> { while (rs.next()) byType.put(rs.getString(1), rs.getLong(2)); return null; }, learnerId);
        return new LearningSummaryView(total, progress, completed, due, byType, records.recent(learnerId));
    }

    public Map<String,Object> totals(long learnerId) {
        return jdbc.queryForMap("""
                SELECT COALESCE(SUM(time_spent_seconds),0) total_time,
                       COALESCE(SUM(attempts),0) total_attempts,
                       ROUND(AVG(score),2) average_score,
                       ROUND(AVG(mastery_level),4) average_mastery
                FROM english_learning_record WHERE learner_id=?
                """, learnerId);
    }

    public List<LearningActivityDayView> activity(long learnerId) {
        LocalDate today = LocalDate.now(timezone.zone());
        LocalDate first = today.minusDays(13);
        Map<LocalDate,int[]> grouped = new HashMap<>();
        jdbc.query("""
                SELECT attempted_at,completion_status,time_spent_seconds
                FROM english_learning_attempt
                WHERE learner_id=? AND attempted_at>=?
                ORDER BY attempted_at
                """, rs -> {
            while (rs.next()) {
                LocalDate day = rs.getTimestamp("attempted_at").toLocalDateTime().toLocalDate();
                int[] value = grouped.computeIfAbsent(day, ignored -> new int[3]);
                value[0]++;
                if ("COMPLETED".equals(rs.getString("completion_status"))) value[1]++;
                value[2] += rs.getInt("time_spent_seconds");
            }
            return null;
        }, learnerId, first.atStartOfDay(timezone.zone()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
        List<LearningActivityDayView> result = new ArrayList<>();
        for (int i=0; i<14; i++) {
            LocalDate day = first.plusDays(i);
            int[] value = grouped.getOrDefault(day, new int[3]);
            result.add(new LearningActivityDayView(day.toString(), value[0], value[1], value[2]));
        }
        return result;
    }

    public List<LearningModuleInsightView> modules(long learnerId) {
        Map<String,LearningModuleInsightView> values = new LinkedHashMap<>();
        for (String type : List.of("GRAMMAR", "READING", "LISTENING", "WRITING"))
            values.put(type, new LearningModuleInsightView(type,0,0,null,0));
        jdbc.query("""
                SELECT content_type,COUNT(*) total,
                       SUM(completion_status='COMPLETED') completed,
                       ROUND(AVG(mastery_level),4) average_mastery,
                       SUM(time_spent_seconds) time_spent
                FROM english_learning_record WHERE learner_id=? GROUP BY content_type
                """, rs -> {
            while (rs.next()) {
                String type = rs.getString("content_type");
                values.put(type, new LearningModuleInsightView(type, rs.getLong("total"),
                        rs.getLong("completed"), rs.getBigDecimal("average_mastery"), rs.getLong("time_spent")));
            }
            return null;
        }, learnerId);
        return List.copyOf(values.values());
    }

    private long count(String sql, long learnerId) {
        Long value = jdbc.queryForObject(sql, Long.class, learnerId);
        return value == null ? 0 : value;
    }
}
