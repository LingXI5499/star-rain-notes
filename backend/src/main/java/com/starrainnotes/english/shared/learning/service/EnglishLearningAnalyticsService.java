package com.starrainnotes.english.shared.learning.service;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.shared.learning.dto.AdminLearningAnalyticsView;
import com.starrainnotes.english.shared.learning.dto.AdminLearningModuleView;
import com.starrainnotes.english.shared.learning.dto.AdminLearningOverviewView;
import com.starrainnotes.english.shared.learning.dto.AdminLearningTrendDayView;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class EnglishLearningAnalyticsService {
    private static final List<String> TYPES = List.of("GRAMMAR", "READING", "LISTENING", "WRITING");
    private static final Set<Integer> RANGES = Set.of(7, 30, 90);
    private static final DateTimeFormatter GENERATED_AT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final JdbcTemplate jdbc;
    private final SiteSettingsTimezone timezone;

    public EnglishLearningAnalyticsService(JdbcTemplate jdbc, SiteSettingsTimezone timezone) {
        this.jdbc = jdbc;
        this.timezone = timezone;
    }

    public AdminLearningAnalyticsView analytics(int days, String rawType) {
        if (!RANGES.contains(days)) throw invalid("days must be one of 7, 30 or 90.");
        String type = rawType == null ? "ALL" : rawType.trim().toUpperCase(Locale.ROOT);
        if (!"ALL".equals(type) && !TYPES.contains(type)) {
            throw invalid("type must be ALL, GRAMMAR, READING, LISTENING or WRITING.");
        }

        ZoneId zone = timezone.zone();
        LocalDate first = LocalDate.now(zone).minusDays(days - 1L);
        LocalDateTime sinceUtc = first.atStartOfDay(zone).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        String offset = zone.getRules().getOffset(Instant.now()).getId();
        if ("Z".equals(offset)) offset = "+00:00";

        return new AdminLearningAnalyticsView(days, type, ZonedDateTime.now(zone).format(GENERATED_AT),
                overview(sinceUtc, type), trend(first, days, sinceUtc, type, offset), modules(sinceUtc, type));
    }

    private AdminLearningOverviewView overview(LocalDateTime since, String type) {
        QueryFilter filter = filter(since, type);
        Map<String, Object> row = jdbc.queryForMap("""
                SELECT COUNT(*) total_attempts,
                       COUNT(DISTINCT learner_id) active_learners,
                       COALESCE(SUM(completion_status='COMPLETED'),0) completions,
                       COALESCE(SUM(time_spent_seconds),0) total_time
                FROM english_learning_attempt
                WHERE attempted_at>=?""" + filter.typeClause(), filter.args());
        long attempts = number(row.get("total_attempts"));
        long completions = number(row.get("completions"));
        return new AdminLearningOverviewView(number(row.get("active_learners")), attempts, completions,
                rate(completions, attempts), number(row.get("total_time")));
    }

    private List<AdminLearningTrendDayView> trend(LocalDate first, int days, LocalDateTime since,
                                                   String type, String offset) {
        QueryFilter filter = filter(since, type);
        Object[] args = prepend(offset, filter.args());
        Map<LocalDate, AdminLearningTrendDayView> values = new HashMap<>();
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
                LocalDate day = rs.getDate("activity_day").toLocalDate();
                values.put(day, new AdminLearningTrendDayView(day.toString(), rs.getLong("attempts"),
                        rs.getLong("completions"), rs.getLong("active_learners"), rs.getLong("time_spent")));
            }
            return null;
        }, args);
        List<AdminLearningTrendDayView> result = new ArrayList<>(days);
        for (int index = 0; index < days; index++) {
            LocalDate day = first.plusDays(index);
            result.add(values.getOrDefault(day, new AdminLearningTrendDayView(day.toString(), 0, 0, 0, 0)));
        }
        return List.copyOf(result);
    }

    private List<AdminLearningModuleView> modules(LocalDateTime since, String selectedType) {
        Map<String, Long> published = publishedCounts();
        Map<String, ModuleAggregate> activity = new HashMap<>();
        QueryFilter filter = filter(since, selectedType);
        jdbc.query("""
                SELECT content_type,COUNT(*) attempts,COUNT(DISTINCT content_id) engaged_content,
                       COALESCE(SUM(completion_status='COMPLETED'),0) completions,
                       COALESCE(SUM(time_spent_seconds),0) time_spent
                FROM english_learning_attempt
                WHERE attempted_at>=?""" + filter.typeClause() + " GROUP BY content_type", rs -> {
            while (rs.next()) activity.put(rs.getString("content_type"), moduleAggregate(rs));
            return null;
        }, filter.args());

        List<String> visibleTypes = "ALL".equals(selectedType) ? TYPES : List.of(selectedType);
        return visibleTypes.stream().map(type -> {
            ModuleAggregate row = activity.getOrDefault(type, ModuleAggregate.EMPTY);
            return new AdminLearningModuleView(type, published.getOrDefault(type, 0L), row.engagedContent(),
                    row.attempts(), row.completions(), rate(row.completions(), row.attempts()), row.timeSpent());
        }).toList();
    }

    private Map<String, Long> publishedCounts() {
        Map<String, Long> values = new HashMap<>();
        jdbc.query("""
                SELECT content_type,COUNT(*) total FROM (
                    SELECT 'GRAMMAR' content_type,l.id FROM english_grammar_lesson l
                    JOIN english_grammar_course c ON c.id=l.course_id
                    WHERE l.publish_status='PUBLISHED' AND c.publish_status='PUBLISHED'
                    UNION ALL SELECT 'READING',id FROM english_reading_article WHERE publish_status='PUBLISHED'
                    UNION ALL SELECT 'LISTENING',id FROM english_listening_item WHERE publish_status='PUBLISHED'
                    UNION ALL SELECT 'WRITING',id FROM english_writing_prompt WHERE publish_status='PUBLISHED'
                ) content_catalog GROUP BY content_type
                """, rs -> {
            while (rs.next()) values.put(rs.getString("content_type"), rs.getLong("total"));
            return null;
        });
        return values;
    }

    private ModuleAggregate moduleAggregate(ResultSet rs) throws SQLException {
        return new ModuleAggregate(rs.getLong("attempts"), rs.getLong("engaged_content"),
                rs.getLong("completions"), rs.getLong("time_spent"));
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

    private BigDecimal rate(long completed, long attempts) {
        if (attempts == 0) return BigDecimal.ZERO.setScale(1);
        return BigDecimal.valueOf(completed * 100).divide(BigDecimal.valueOf(attempts), 1, RoundingMode.HALF_UP);
    }

    private long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0;
    }

    private ApiException invalid(String detail) {
        return new ApiException(HttpStatus.BAD_REQUEST, "ENGLISH_ANALYTICS_FILTER_INVALID",
                "Invalid analytics filter", detail);
    }

    private record QueryFilter(String typeClause, Object[] args) { }
    private record ModuleAggregate(long attempts, long engagedContent, long completions, long timeSpent) {
        private static final ModuleAggregate EMPTY = new ModuleAggregate(0, 0, 0, 0);
    }
}
