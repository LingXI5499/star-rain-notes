package com.starrainnotes.account.review.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starrainnotes.account.review.dto.ContentReviewView;
import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.site.service.SiteSettingsTimezone;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Repository
public class ContentReviewRepository {
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final SiteSettingsTimezone timezone;

    public ContentReviewRepository(JdbcTemplate jdbc, ObjectMapper json, SiteSettingsTimezone timezone) {
        this.jdbc = jdbc;
        this.json = json;
        this.timezone = timezone;
    }

    public List<ContentReviewView> list(String status, int safePage, int safeSize) {
        String where = status == null || status.isBlank() || "ALL".equalsIgnoreCase(status) ? "" : " WHERE status=?";
        Object[] args = where.isEmpty()
                ? new Object[]{safeSize, (safePage - 1) * safeSize}
                : new Object[]{status.toUpperCase(), safeSize, (safePage - 1) * safeSize};
        return jdbc.query("SELECT * FROM content_review_request" + where
                + " ORDER BY created_at DESC,id DESC LIMIT ? OFFSET ?", this::map, args);
    }

    public int countPending(String contentType, long contentId) {
        Integer pending = jdbc.queryForObject("""
                SELECT COUNT(*) FROM content_review_request
                WHERE content_type=? AND content_id=? AND status='PENDING'
                """, Integer.class, contentType, contentId);
        return pending == null ? 0 : pending;
    }

    public long insert(String contentType, long contentId, String title, String payloadJson, Long actorId) {
        jdbc.update("""
                INSERT INTO content_review_request(content_type,content_id,action_type,title,payload_json,submitted_by)
                VALUES (?,?,'UPDATE',?,?,?)
                """, contentType, contentId, title, payloadJson, actorId);
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return id;
    }

    public void markApproved(long id, Long reviewerId, String note) {
        jdbc.update("""
                UPDATE content_review_request
                SET status='APPROVED',reviewed_by=?,review_note=?,reviewed_at=UTC_TIMESTAMP(6)
                WHERE id=? AND status='PENDING'
                """, reviewerId, note, id);
    }

    public void markRejected(long id, Long reviewerId, String note) {
        jdbc.update("""
                UPDATE content_review_request
                SET status='REJECTED',reviewed_by=?,review_note=?,reviewed_at=UTC_TIMESTAMP(6)
                WHERE id=? AND status='PENDING'
                """, reviewerId, note, id);
    }

    public ContentReviewView require(long id) {
        try {
            return jdbc.queryForObject("SELECT * FROM content_review_request WHERE id=?", this::map, id);
        } catch (EmptyResultDataAccessException e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CONTENT_REVIEW_NOT_FOUND",
                    "Review not found", "The requested review does not exist.");
        }
    }

    private ContentReviewView map(ResultSet rs, int row) throws SQLException {
        return new ContentReviewView(rs.getLong("id"), rs.getString("content_type"), rs.getLong("content_id"),
                rs.getString("action_type"), rs.getString("title"),
                readPayload(rs.getString("payload_json")),
                rs.getString("status"), nullableLong(rs, "submitted_by"), nullableLong(rs, "reviewed_by"),
                rs.getString("review_note"), format(rs, "created_at"), format(rs, "updated_at"), format(rs, "reviewed_at"));
    }

    private Map<String, Object> readPayload(String value) {
        try {
            return json.readValue(value, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Invalid review payload JSON", e);
        }
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private String format(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timezone.atSite(timestamp.toLocalDateTime()).format(ISO);
    }
}
