package com.starrainnotes.english.listening.infrastructure;

import com.starrainnotes.common.error.ApiException;
import com.starrainnotes.english.listening.domain.SegmentTimelinePolicy;
import com.starrainnotes.english.listening.dto.ListeningSegmentRequest;
import com.starrainnotes.english.listening.dto.ListeningSegmentView;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Persists listening segment data independently from item lifecycle and queries. */
@Repository
public class ListeningSegmentRepository {
    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private final JdbcTemplate jdbc;
    private final SegmentTimelinePolicy timeline;

    public ListeningSegmentRepository(JdbcTemplate jdbc, SegmentTimelinePolicy timeline) {
        this.jdbc = jdbc;
        this.timeline = timeline;
    }

    public List<ListeningSegmentView> list(Long itemId) {
        requireExists(itemId);
        return query(itemId);
    }

    public List<ListeningSegmentView> publicList(Long itemId) {
        requirePublished(itemId);
        return query(itemId);
    }

    @Transactional
    public ListeningSegmentView create(Long itemId, ListeningSegmentRequest request) {
        requireExists(itemId);
        Integer duration = duration(itemId);
        timeline.validateRange(request.startMs(), request.endMs(), duration);
        Integer max = jdbc.queryForObject("SELECT COALESCE(MAX(sort_order),0) FROM english_listening_segment WHERE listening_item_id=?",
                Integer.class, itemId);
        int order = (max == null ? 0 : max) + 10;
        jdbc.update("INSERT INTO english_listening_segment(listening_item_id,start_ms,end_ms,transcript_text,translation_text,sort_order) VALUES (?,?,?,?,?,?)",
                itemId, request.startMs(), request.endMs(), request.transcriptText(), clean(request.translationText()), order);
        return bySort(itemId, order);
    }

    @Transactional
    public ListeningSegmentView update(Long itemId, Long segmentId, ListeningSegmentRequest request) {
        requireSegment(itemId, segmentId);
        timeline.validateRange(request.startMs(), request.endMs(), duration(itemId));
        jdbc.update("UPDATE english_listening_segment SET start_ms=?,end_ms=?,transcript_text=?,translation_text=? WHERE id=? AND listening_item_id=?",
                request.startMs(), request.endMs(), request.transcriptText(), clean(request.translationText()), segmentId, itemId);
        return bySort(itemId, segmentSort(itemId, segmentId));
    }

    @Transactional
    public void delete(Long itemId, Long segmentId) {
        requireSegment(itemId, segmentId);
        jdbc.update("DELETE FROM english_listening_segment WHERE id=? AND listening_item_id=?", segmentId, itemId);
    }

    @Transactional
    public void move(Long itemId, Long segmentId, int targetIndex) {
        requireSegment(itemId, segmentId);
        List<Long> ids = new ArrayList<>(jdbc.queryForList(
                "SELECT id FROM english_listening_segment WHERE listening_item_id=? ORDER BY sort_order,id", Long.class, itemId));
        ids.remove(segmentId);
        ids.add(Math.min(Math.max(targetIndex, 0), ids.size()), segmentId);
        if (ids.isEmpty()) return;
        jdbc.update("UPDATE english_listening_segment SET sort_order=100000 WHERE listening_item_id=?", itemId);
        for (int i = 0; i < ids.size(); i++) {
            jdbc.update("UPDATE english_listening_segment SET sort_order=? WHERE id=?", (i + 1) * 10, ids.get(i));
        }
    }

    @Transactional
    public List<ListeningSegmentView> replace(Long itemId, List<ListeningSegmentRequest> requests) {
        requireExists(itemId);
        timeline.validateReplacement(requests, duration(itemId));
        jdbc.update("DELETE FROM english_listening_segment WHERE listening_item_id=?", itemId);
        int order = 10;
        for (ListeningSegmentRequest request : requests) {
            jdbc.update("INSERT INTO english_listening_segment(listening_item_id,start_ms,end_ms,transcript_text,translation_text,sort_order) VALUES (?,?,?,?,?,?)",
                    itemId, request.startMs(), request.endMs(), request.transcriptText(), clean(request.translationText()), order);
            order += 10;
        }
        return query(itemId);
    }

    private List<ListeningSegmentView> query(Long itemId) {
        return jdbc.query("SELECT id,listening_item_id,start_ms,end_ms,transcript_text,translation_text,sort_order,updated_at FROM english_listening_segment WHERE listening_item_id=? ORDER BY sort_order,id",
                (rs, row) -> map(rs), itemId);
    }

    private Integer duration(Long itemId) {
        return jdbc.queryForObject("SELECT duration_seconds FROM english_listening_item WHERE id=?", Integer.class, itemId);
    }

    private void requireExists(Long itemId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM english_listening_item WHERE id=?", Integer.class, itemId);
        if (count == null || count == 0) throw new ApiException(HttpStatus.NOT_FOUND,
                "ENGLISH_LISTENING_ITEM_NOT_FOUND", "Item not found", "The listening material does not exist.");
    }

    private void requirePublished(Long itemId) {
        String status = jdbc.queryForObject("SELECT publish_status FROM english_listening_item WHERE id=?", String.class, itemId);
        if (!"PUBLISHED".equals(status)) throw new ApiException(HttpStatus.NOT_FOUND,
                "ENGLISH_CONTENT_NOT_PUBLISHED", "Item not available", "The listening material is not published.");
    }

    private void requireSegment(Long itemId, Long segmentId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM english_listening_segment WHERE id=? AND listening_item_id=?",
                Integer.class, segmentId, itemId);
        if (count == null || count == 0) throw new ApiException(HttpStatus.NOT_FOUND, "ENGLISH_LISTENING_SEGMENT_NOT_FOUND",
                "Segment not found", "The listening segment does not exist.");
    }

    private ListeningSegmentView bySort(Long itemId, int order) {
        try {
            return jdbc.queryForObject("SELECT id,listening_item_id,start_ms,end_ms,transcript_text,translation_text,sort_order,updated_at FROM english_listening_segment WHERE listening_item_id=? AND sort_order=? ORDER BY id DESC LIMIT 1",
                    (rs, row) -> map(rs), itemId, order);
        } catch (EmptyResultDataAccessException ex) {
            throw new IllegalStateException("Inserted segment could not be reloaded", ex);
        }
    }

    private Integer segmentSort(Long itemId, Long segmentId) {
        return jdbc.queryForObject("SELECT sort_order FROM english_listening_segment WHERE id=? AND listening_item_id=?",
                Integer.class, segmentId, itemId);
    }

    private ListeningSegmentView map(ResultSet rs) throws SQLException {
        Timestamp updated = rs.getTimestamp("updated_at");
        return new ListeningSegmentView(rs.getLong("id"), rs.getLong("listening_item_id"), rs.getInt("start_ms"),
                rs.getInt("end_ms"), rs.getString("transcript_text"), rs.getString("translation_text"),
                rs.getInt("sort_order"), updated == null ? null : ISO_OFFSET.format(updated.toInstant().atOffset(java.time.ZoneOffset.UTC)));
    }

    private String clean(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
}
