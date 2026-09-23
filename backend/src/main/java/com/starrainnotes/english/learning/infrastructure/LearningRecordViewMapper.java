package com.starrainnotes.english.learning.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.english.learning.dto.LearningRecordView;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class LearningRecordViewMapper {
    private final ObjectMapper json;
    private final SiteSettingsTimezone timezone;

    public LearningRecordViewMapper(ObjectMapper json, SiteSettingsTimezone timezone) {
        this.json = json;
        this.timezone = timezone;
    }

    public LearningRecordView map(ResultSet row, int ignored) throws SQLException {
        return new LearningRecordView(
                row.getLong("id"), row.getString("content_type"), row.getLong("content_id"),
                row.getString("content_slug"), row.getString("cefr_level"), row.getString("completion_status"),
                row.getBigDecimal("score"), row.getInt("time_spent_seconds"), row.getInt("attempts"),
                weakPoints(row.getString("weak_points_json")), row.getBigDecimal("mastery_level"),
                date(row, "next_review_at"), date(row, "updated_at"));
    }

    private List<String> weakPoints(String value) {
        try {
            return json.readValue(value, new TypeReference<>() {});
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String date(ResultSet row, String column) throws SQLException {
        var timestamp = row.getTimestamp(column);
        return timestamp == null ? null : timezone.atSite(timestamp.toLocalDateTime())
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
